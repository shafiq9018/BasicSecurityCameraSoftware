package webcamServer;

import java.util.*;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import javax.crypto.spec.*;
import org.libjpegturbo.turbojpeg.*;
import org.nanohttpd.protocols.http.*;
import org.nanohttpd.protocols.http.response.*;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import com.github.sarxos.webcam.*;
import com.github.sarxos.webcam.ds.ipcam.*;
import com.github.sarxos.webcam.ds.nativeapi.*;
import sharedObjects.*;

public class WebcamServer {
	private static final String version = "1.0";
	public final static Logger logger = new Logger();
	private static volatile boolean killThread = false;
	private static volatile Webcam webcam = null;
	private static volatile boolean isIpCamera = false;
	private static volatile TJCompressor turboJpegCompressor = null;

	private static volatile Snapshot lastSnapshot = new Snapshot();
	private static volatile NetImage lastWebcamImage = new NetImage();
	private static volatile byte[] emptyImage = new byte[0];
	private static volatile int httpPort = -1, tcpLivePort = -1, tcpHistoryPort = -1, udpDiscoveryPort = -1;
	private static volatile File snapshotFolder = null;
	private static volatile int maxFps = -1, quality = -1, width = -1, height = -1, snapshotInterval = -1, snapshotHistoryDays = -1, timestampPosition = -1, timestampSize = -1, timestampTransparency = 120;
	private static volatile boolean timestamp = false, timestampColor = false;
	private static volatile String wwwTitle = "", timestampFormat = "";

	public static void main(String[] args) {
		logger.logLn("Webcam server " + version + " started");
		
		logger.logLn("Initializing");
		
		javax.swing.JOptionPane.setDefaultLocale(Locale.ENGLISH);
		Locale.setDefault(new Locale("en", "US"));
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				logger.logThrowable(e);
			}
		});

		try {
			turboJpegCompressor = new TJCompressor();
			turboJpegCompressor.setSubsamp(TJ.SAMP_444);
		} catch (Exception e) {
			logger.logException(e);
			System.exit(0);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.logLn("Shutdown received");
				killThread = true;
			}
		});

		File settingsFile = new File("settings.txt");
		Scanner scanner = null;
		try {
			scanner = new Scanner(settingsFile);
			logger.logLn("Reading settings from file");
		} catch (Exception e) {
			settingsFile = null;
			scanner = new Scanner(System.in);
		}

		System.out.println();
		
		if(readString("Webcam type (usb/ip): ", scanner, settingsFile != null, new String[] {"usb", "ip"}, true, 0).equals("ip")) {
			Webcam.setDriver(new IpCamDriver());
			System.out.println();

			String url = readString("mjpeg stream url: ", scanner, settingsFile != null, null, false, 1); // like http://x.x.x.x/mjpg/video.mjpg
			String mode = readString("Streaming mode (push/pull): ", scanner, settingsFile != null, new String[] {"push", "pull"}, true, 0);
			String username = readString("Webcam username (optional): ", scanner, settingsFile != null, null, false, 0);
			String password = readString("Webcam password (optional): ", scanner, settingsFile != null, null, false, 0);

			try {
				IpCamMode icm = null;
				if(mode.equals("push")) icm = IpCamMode.PUSH;
				if(mode.equals("pull")) icm = IpCamMode.PULL;
				IpCamAuth auth = null;
				if(username.length() > 0 || password.length() > 0) auth = new IpCamAuth(username, password);

				IpCamDeviceRegistry.register(new IpCamDevice("Webcam", url, icm, auth));
				webcam = Webcam.getDefault();
				isIpCamera = true;
			} catch (Exception e) {
				logger.logException(e);
				System.exit(0);
			}
		}
		else {
			String os = System.getProperty("os.name").toLowerCase();
			if(os.contains("win") || os.contains("mac")) {
				logger.logLn("Loading native library");
				Webcam.setDriver(new NativeWebcamDriver());
				System.out.println();
			}
			else logger.logLn("Loading default library");

			java.util.List<Webcam> webcams = Webcam.getWebcams();
			if(webcams == null || webcams.size() == 0) {
				logger.logLn("No webcams found");
				System.exit(0);
			}
			else {
				System.out.println("Available webcams:");
				for(int i = 0; i < webcams.size(); i++) System.out.println("[" + i + "] " + webcams.get(i).getName());
				int selection = readInt("Choose a webcam: ", scanner, settingsFile != null, 0, webcams.size() - 1);
				webcam = webcams.get(selection);
			}
		}

		String tcpLivePassword = null, tcpHistoryPassword = null;

		width = readInt("Frame width (100-3000): ", scanner, settingsFile != null, 100, 3000);
		height = readInt("Frame height (100-3000): ", scanner, settingsFile != null, 100, 3000);
		maxFps = readInt("Maximum fps (1-30): ", scanner, settingsFile != null, 1, 30);
		quality = readInt("Compression quality (1-100): ", scanner, settingsFile != null, 1, 100);
		if(readString("Timestamp enable (y/n): ", scanner, settingsFile != null, new String[] {"y", "n"}, true, 0).equals("y")) timestamp = true;
		if(timestamp) {
			timestampFormat = readString("Timestamp format (empty to default): ", scanner, settingsFile != null, null, false, 0);
			if(timestampFormat.length() < 1) timestampFormat = "yyyy-MM-dd HH:mm:ss.SSS";
			timestampSize = readInt("Timestamp size (10-50): ", scanner, settingsFile != null, 10, 50);
			System.out.println("Timestamp positions:");
			System.out.println("[0] center");
			System.out.println("[1] top left");
			System.out.println("[2] top right");
			System.out.println("[3] bottom left");
			System.out.println("[4] bottom right");
			timestampPosition = readInt("Choose a position: ", scanner, settingsFile != null, 0, 4);
			System.out.println("Timestamp color:");
			System.out.println("[0] black on white");
			System.out.println("[1] white on black");
			timestampColor = readInt("Choose the colors: ", scanner, settingsFile != null, 0, 1) != 0;
		}
		tcpLivePort = readInt("TCP live server port (0 to disable): ", scanner, settingsFile != null, 0, 65535);
		if(tcpLivePort > 0) tcpLivePassword = readString("Password for the connection (optional): ", scanner, settingsFile != null, null, false, 0);
		udpDiscoveryPort = readInt("UDP discovery port (0 to disable): ", scanner, settingsFile != null, 0, 65535);
		httpPort = readInt("HTTP server port (0 to disable): ", scanner, settingsFile != null, 0, 65535);
		if(httpPort > 0) wwwTitle = readString("Page title: ", scanner, settingsFile != null, null, false, 1);
		String folder = readString("Snapshot folder (optional): ", scanner, settingsFile != null, null, false, 0);
		if(folder.length() > 0) {
			snapshotFolder = new File(folder);
			int min = 1000 / maxFps;
			snapshotInterval = readInt("Snapshot interval ms (" + min + "-300000): ", scanner, settingsFile != null, min, 300000);
			snapshotHistoryDays = readInt("Snapshot history limit days (0 to disable): ", scanner, settingsFile != null, 0, Integer.MAX_VALUE);
			tcpHistoryPort = readInt("TCP history server port (0 to disable): ", scanner, settingsFile != null, 0, 65535);
			if(tcpHistoryPort > 0) tcpHistoryPassword = readString("Password for the connection (optional): ", scanner, settingsFile != null, null, false, 0);
		}

		scanner.close();

		if(tcpLivePort > 0) {
			logger.logLn("Initializing tcp live server");

			try {
				Map<Integer, Long> liveIntFlags = new ConcurrentHashMap<>(), liveFrameCounter = new ConcurrentHashMap<>();
				Server server = new Server(5000000, 5000000);
				
				NetworkKey networkKey = new NetworkKey("Blowfish", "Blowfish", false); // new NetworkKey("AES", "AES/CBC/PKCS5Padding", true);
				javax.crypto.Cipher.getInstance(networkKey.transformation);

				networkKey.key = new SecretKeySpec(MessageDigest.getInstance("MD5").digest(tcpLivePassword.getBytes()), networkKey.algorithm);
				if(networkKey.hasIv) networkKey.iv = MessageDigest.getInstance("MD5").digest(networkKey.key.getEncoded());

				Kryo kryo = server.getKryo();
				kryo.register(byte[].class);
				Registration r = kryo.register(NetImage.class);
				kryo.register(NetImage.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());
				r = kryo.register(IntParameter.class);
				kryo.register(IntParameter.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());

				server.addListener(new Listener() {
					public void connected (Connection connection) {
						try {
							logger.logLn("Opened live connection #" + connection.getID() + " from " + connection.getRemoteAddressTCP().getAddress().getHostAddress());

							connection.setKeepAliveTCP(2000);
							connection.setTimeout(12000);
							connection.setIdleThreshold(0.01f);
						} catch (Exception e) {
							logger.logException(e);
						}
					}

					public void disconnected (Connection connection) {
						try {
							long frames = 0;
							if(liveFrameCounter.containsKey(connection.getID())) frames = liveFrameCounter.get(connection.getID());
							logger.logLn("Closed live connection #" + connection.getID() + " (" + frames + " frames sent)");

							liveIntFlags.remove(connection.getID());
							liveFrameCounter.remove(connection.getID());
						} catch (Exception e) {
							logger.logException(e);
						}
					}

					public void received (Connection connection, Object object) {
						try {
							if(object == null) return;
							else if(object instanceof NetImage) {
								boolean authorized = false;
								long number = 0;
								if(liveIntFlags.containsKey(connection.getID())) {
									number = liveIntFlags.get(connection.getID());
									if(((NetImage) object).getNumber() == number) authorized = true;
								}

								if(authorized) {
									if(liveFrameCounter.containsKey(connection.getID())) liveFrameCounter.put(connection.getID(), liveFrameCounter.get(connection.getID()) + 1L);
									else liveFrameCounter.put(connection.getID(), 1L);
									liveIntFlags.put(connection.getID(), number + 1L);
									connection.sendTCP(lastWebcamImage);
								}
								else {
									logger.logLn("Live connection #" + connection.getID() + " is not authorized");
									connection.close();
								}
							}
							else if(object instanceof IntParameter) {
								String key = ((IntParameter) object).getKey();
								if(key == null) key = "";

								switch(key) {
								case "fps":
									connection.sendTCP(new IntParameter("fps", maxFps));
									break;
								case "start":
									logger.logLn("Live connection #" + connection.getID() + " requested stream start");
									long start = new Random().nextLong();
									liveIntFlags.put(connection.getID(), start + 1L);
									connection.sendTCP(new IntParameter("start", start));
									break;
								default:
									break;
								}
							}
						} catch (Exception e) {
							logger.logException(e);
						}
					}

					public void idle (Connection connection) {

					}
				});
				server.start();
				server.bind(tcpLivePort);
			} catch (Exception e) {
				logger.logException(e);
			}
		}

		if(udpDiscoveryPort > 0) {
			logger.logLn("Initializing udp discovery server");

			new Thread() {
				public void run() {
					DatagramSocket discoverySocket = null;

					try {
						discoverySocket = new DatagramSocket(udpDiscoveryPort);
						discoverySocket.setSoTimeout(0);

						byte[] receiveData = new byte[1024];
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						while (!killThread) {
							try {
								discoverySocket.receive(receivePacket);
								discoverySocket.send(new DatagramPacket(receivePacket.getData(), receivePacket.getData().length, receivePacket.getAddress(), receivePacket.getPort()));
							} catch (Exception e) {
								logger.logException(e);
							}
						}
					} catch (Exception e) {
						logger.logException(e);
					}

					try {
						discoverySocket.close();
					} catch (Exception e) { }
				}
			}.start();
		}

		if(httpPort > 0) {
			logger.logLn("Initializing http server");

			try {
				WebServer webServer = new WebServer(httpPort);
				webServer.start();
			} catch (Exception e) {
				logger.logException(e);
			}
		}

		if(snapshotFolder != null) {
			logger.logLn("Initializing file archive");

			DateTimeFormatter folderFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");

			new Thread() {
				public void run() {
					long lastTimeProcess = System.nanoTime();

					while(!killThread) {
						try {
							Snapshot copy = lastSnapshot;
							lastSnapshot = new Snapshot();

							if(copy.getImage() != null && copy.getTimestamp() != null) {
								File folder = new File(snapshotFolder.getAbsolutePath() + File.separator + copy.getTimestamp().format(folderFormatter));
								folder.mkdirs();
								File file = new File(folder.getAbsolutePath() + File.separator + "Snapshot_" + copy.getTimestamp().format(fileFormatter) + ".jpg");
								FileOutputStream fos = new FileOutputStream(file);
								fos.write(copy.getImage());
								fos.close();
							}
						} catch (Exception e) {
							logger.logException(e);
						}

						long delay = snapshotInterval - (System.nanoTime() - lastTimeProcess) / 1000000;
						if(delay > 0 && delay <= 300000) {
							try { Thread.sleep(delay); } catch (InterruptedException e) { }
						}
						lastTimeProcess = System.nanoTime();
					}
				}
			}.start();

			if(snapshotHistoryDays > 0) {
				new Thread() {
					public void run() {
						while(!killThread) {
							try {
								LocalDateTime now = LocalDateTime.now();
								File[] folders = snapshotFolder.listFiles();
								for(File folder : folders) {
									try {
										if(!folder.isDirectory()) continue;
										if(folder.getName().startsWith(".")) continue;

										LocalDateTime folderDate = LocalDateTime.from(LocalDate.parse(folder.getName(), folderFormatter).atStartOfDay());
										long differenceHours = ChronoUnit.HOURS.between(folderDate, now);
										if(differenceHours > 24L * snapshotHistoryDays + 24L) {
											logger.logLn("Deleting folder: " + folder.getAbsolutePath());
											deleteDir(folder);
										}
									} catch (Exception e) {
										logger.logException(e);
									}
								}
							} catch (Exception e) {
								logger.logException(e);
							}

							for(int i = 0; i < 60; i++) {
								try { Thread.sleep(1000); } catch (InterruptedException e) { }
							}
						}
					}
				}.start();
			}
			
			if(tcpHistoryPort > 0) {
				logger.logLn("Initializing tcp history server");
				
				try {
					Map<Integer, Long> historyIntFlags = new ConcurrentHashMap<>(), historyFrameCounter = new ConcurrentHashMap<>();
					Map<Integer, Boolean> historyIdleFlags = new ConcurrentHashMap<>();
					BlockingQueue<ObjectID> sendNetList = new LinkedBlockingQueue<ObjectID>();
					ExecutorService executor = Executors.newFixedThreadPool(5);
					Server server = new Server(5000000, 5000000);
					
					NetworkKey networkKey = new NetworkKey("Blowfish", "Blowfish", false); // new NetworkKey("AES", "AES/CBC/PKCS5Padding", true);
					javax.crypto.Cipher.getInstance(networkKey.transformation);

					networkKey.key = new SecretKeySpec(MessageDigest.getInstance("MD5").digest(tcpHistoryPassword.getBytes()), networkKey.algorithm);
					if(networkKey.hasIv) networkKey.iv = MessageDigest.getInstance("MD5").digest(networkKey.key.getEncoded());

					Kryo kryo = server.getKryo();
					kryo.register(byte[].class);
					kryo.register(String[].class);
					kryo.register(BooleanWrapper.class);
					Registration r = kryo.register(NetImage.class);
					kryo.register(NetImage.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());
					r = kryo.register(NetDirList.class);
					kryo.register(NetDirList.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());
					r = kryo.register(NetFileList.class);
					kryo.register(NetFileList.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());
					r = kryo.register(IntParameter.class);
					kryo.register(IntParameter.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());

					server.addListener(new Listener() {
						public void connected (Connection connection) {
							try {
								logger.logLn("Opened history connection #" + connection.getID() + " from " + connection.getRemoteAddressTCP().getAddress().getHostAddress());

								connection.setKeepAliveTCP(2000);
								connection.setTimeout(12000);
								connection.setIdleThreshold(0.01f);
							} catch (Exception e) {
								logger.logException(e);
							}
						}

						public void disconnected (Connection connection) {
							try {
								long frames = 0;
								if(historyFrameCounter.containsKey(connection.getID())) frames = historyFrameCounter.get(connection.getID());
								logger.logLn("Closed history connection #" + connection.getID() + " (" + frames + " frames sent)");

								historyIntFlags.remove(connection.getID());
								historyFrameCounter.remove(connection.getID());
								historyIdleFlags.remove(connection.getID());
							} catch (Exception e) {
								logger.logException(e);
							}
						}

						public void received (Connection connection, Object object) {
							try {
								if(object == null) return;
								else if(object instanceof NetImage) {
									boolean authorized = false;
									long number = 0;
									if(historyIntFlags.containsKey(connection.getID())) {
										number = historyIntFlags.get(connection.getID());
										if(((NetImage) object).getNumber() == number) authorized = true;
									}

									if(authorized) {
										if(historyFrameCounter.containsKey(connection.getID())) historyFrameCounter.put(connection.getID(), historyFrameCounter.get(connection.getID()) + 1L);
										else historyFrameCounter.put(connection.getID(), 1L);
										historyIntFlags.put(connection.getID(), number + 1L);
										executor.submit(new Runnable() {
											public void run() {
												try {
													File folder = new File(snapshotFolder, ((NetImage) object).getDir());
													File file = new File(folder, ((NetImage) object).getFile());
													byte[] img = Files.readAllBytes(file.toPath());
													NetImage ni = new NetImage();
													ni.setBytes(img);
													sendNetList.add(new ObjectID(connection.getID(), ni));
												} catch (Exception e) {
													logger.logException(e);
												}
												sendNetList.add(new ObjectID(connection.getID(), new BooleanWrapper(false)));
											}
										});
									}
									else {
										logger.logLn("History connection #" + connection.getID() + " is not authorized");
										connection.close();
									}
								}
								else if(object instanceof NetFileList) {
									boolean authorized = false;
									long number = 0;
									if(historyIntFlags.containsKey(connection.getID())) {
										number = historyIntFlags.get(connection.getID());
										if(((NetFileList) object).getNumber() == number) authorized = true;
									}

									if(authorized) {
										historyIntFlags.put(connection.getID(), number + 1L);
										executor.submit(new Runnable() {
											public void run() {
												try {
													File dir = new File(snapshotFolder, ((NetFileList) object).getDir());
													File[] files = dir.listFiles();
													Arrays.sort(files);

													int i = 0;
													while(i < files.length) {
														String[] chunk = new String[1000];
														int j = 0;
														while(j < chunk.length && i < files.length) {
															if(files[i].isFile()) {
																chunk[j] = files[i].getName();
																j++;
															}
															i++;
														}
														NetFileList nfl = new NetFileList();
														nfl.setFiles(chunk, j);
														sendNetList.add(new ObjectID(connection.getID(), nfl));
													}
												} catch (Exception e) {
													logger.logException(e);
												}
												sendNetList.add(new ObjectID(connection.getID(), new BooleanWrapper(false)));
											}
										});
									}
									else {
										logger.logLn("History connection #" + connection.getID() + " is not authorized");
										connection.close();
									}
								}
								else if(object instanceof NetDirList) {
									boolean authorized = false;
									long number = 0;
									if(historyIntFlags.containsKey(connection.getID())) {
										number = historyIntFlags.get(connection.getID());
										if(((NetDirList) object).getNumber() == number) authorized = true;
									}

									if(authorized) {
										historyIntFlags.put(connection.getID(), number + 1L);
										executor.submit(new Runnable() {
											public void run() {
												try {
													File[] files = snapshotFolder.listFiles();
													Arrays.sort(files);

													int i = 0;
													while(i < files.length) {
														String[] chunk = new String[1000];
														int j = 0;
														while(j < chunk.length && i < files.length) {
															if(files[i].isDirectory()) {
																chunk[j] = files[i].getName();
																j++;
															}
															i++;
														}
														NetDirList ndl = new NetDirList();
														ndl.setDirs(chunk, j);
														sendNetList.add(new ObjectID(connection.getID(), ndl));
													}
												} catch (Exception e) {
													logger.logException(e);
												}
												sendNetList.add(new ObjectID(connection.getID(), new BooleanWrapper(false)));
											}
										});
									}
									else {
										logger.logLn("History connection #" + connection.getID() + " is not authorized");
										connection.close();
									}
								}
								else if(object instanceof IntParameter) {
									String key = ((IntParameter) object).getKey();
									if(key == null) key = "";

									switch(key) {
									case "start":
										logger.logLn("History connection #" + connection.getID() + " requested stream start");
										long start = new Random().nextLong();
										historyIntFlags.put(connection.getID(), start + 1L);
										historyIdleFlags.put(connection.getID(), true);
										connection.sendTCP(new IntParameter("start", start));
										break;
									default:
										break;
									}
								}
							} catch (Exception e) {
								logger.logException(e);
							}
						}

						public void idle (Connection connection) {
							if(historyIdleFlags.containsKey(connection.getID())) historyIdleFlags.put(connection.getID(), true);
						}
					});
					server.start();
					server.bind(tcpHistoryPort);
					
					new Thread() {
						public void run() {
							while(!killThread) {
								try {
									if(sendNetList.isEmpty()) Thread.sleep(0, 100);
									else {
										Object obj = sendNetList.remove();
										int id = ((ObjectID) obj).id;
										Object object = ((ObjectID) obj).object;
										
										if(!historyIdleFlags.containsKey(id)) continue;
										for(Connection connection : server.getConnections()) {
											if (connection.getID() == id) {
												while(connection.isConnected() && !historyIdleFlags.get(connection.getID()) && !killThread) Thread.sleep(0, 100);
												connection.sendTCP(object);
												historyIdleFlags.put(connection.getID(), false);
											}
										}
									}
								} catch (Exception e) {
									logger.logException(e);
								}
							}
						}
					}.start();
				} catch (Exception e) {
					logger.logException(e);
				}
			}
		}

		new Thread() {
			public void run() {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timestampFormat);
					BufferedImage rawImgOld = null;
					int errorCounter = 0, ipCamErrorCounter = 0;
					
					try {
						turboJpegCompressor.setJPEGQuality(quality);
						turboJpegCompressor.setSourceImage(new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR), 0, 0, 0, 0);
						byte[] jpegBuf = turboJpegCompressor.compress(TJ.FLAG_ACCURATEDCT);
						emptyImage = new byte[turboJpegCompressor.getCompressedSize()];
						System.arraycopy(jpegBuf, 0, emptyImage, 0, emptyImage.length);
					} catch (Exception e) {
						logger.logException(e);
					}
					
					try {
						if(isIpCamera) {
							logger.logLn("Testing connection to webcam");
							if(IpCamDeviceRegistry.getIpCameras().get(0).isOnline()) {
								logger.logLn("Opening webcam");
								webcam.setCustomViewSizes(new Dimension[] {new Dimension(width, height)});
								webcam.setViewSize(new Dimension(width, height));
								webcam.open();
								logger.logLn("Webcam opened");
							}
							else logger.logLn("Webcam offline");
						}
						else {
							logger.logLn("Opening webcam");
							webcam.setCustomViewSizes(new Dimension[] {new Dimension(width, height)});
							webcam.setViewSize(new Dimension(width, height));
							webcam.open();
							logger.logLn("Webcam opened");
						}
					} catch (Exception e) {
						logger.logException(e);
					}
					
					long lastTimeProcess = System.nanoTime();
					while(!killThread) {
						if(isIpCamera && ipCamErrorCounter > 40 * maxFps + 2) {
							try {
								ipCamErrorCounter = 0;
								
								if(webcam.isOpen()) {
									logger.logLn("Closing webcam");
									webcam.close();
									logger.logLn("Webcam closed");
								}
								
								logger.logLn("Testing connection to webcam");
								if(IpCamDeviceRegistry.getIpCameras().get(0).isOnline()) {
									logger.logLn("Opening webcam");
									webcam.setCustomViewSizes(new Dimension[] {new Dimension(width, height)});
									webcam.setViewSize(new Dimension(width, height));
									webcam.open();
									logger.logLn("Webcam opened");
								}
								else logger.logLn("Webcam offline");
							} catch (Exception e) {
								logger.logException(e);
							}
						}
						
						BufferedImage rawImg = null;
						if(webcam.isOpen()) rawImg = webcam.getImage();
						LocalDateTime nowDateTime = LocalDateTime.now();
						
						if(errorBufferedImage(rawImg, rawImgOld)) {
							if(errorCounter < Integer.MAX_VALUE) errorCounter++;
							if(ipCamErrorCounter < Integer.MAX_VALUE) ipCamErrorCounter++;
						}
						else {
							errorCounter = 0;
							ipCamErrorCounter = 0;
						}
						rawImgOld = rawImg;

						BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
						Graphics2D graphics = image.createGraphics();
						graphics.drawImage(rawImg, 0, 0, null);
						graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						graphics.setFont(new Font("SansSerif", Font.PLAIN, timestamp ? timestampSize : 15));
						FontMetrics fontMetrics = graphics.getFontMetrics();
						
						if(timestamp) {
							String nowString = nowDateTime.format(formatter);
							Rectangle2D rect = fontMetrics.getStringBounds(nowString, graphics);
					        int stringWidth = (int)Math.round(rect.getWidth());
					        int stringHeight = (int)Math.round(rect.getHeight());
							
							if(timestampPosition == 0) { //center
								if(timestampColor) graphics.setColor(new Color(0, 0, 0, timestampTransparency));
								else graphics.setColor(new Color(255, 255, 255, timestampTransparency));
						        graphics.fillRect(width / 2 - stringWidth / 2 - fontMetrics.getDescent(), height / 2 - stringHeight / 2, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        if(timestampColor) graphics.setColor(Color.white);
						        else graphics.setColor(Color.black);
						        graphics.drawString(nowString, width / 2 - stringWidth / 2, height / 2 - stringHeight / 2 + fontMetrics.getAscent());
							}
							else if(timestampPosition == 1) { //top left
								if(timestampColor) graphics.setColor(new Color(0, 0, 0, timestampTransparency));
								else graphics.setColor(new Color(255, 255, 255, timestampTransparency));
						        graphics.fillRect(0, 0, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        if(timestampColor) graphics.setColor(Color.white);
						        else graphics.setColor(Color.black);
						        graphics.drawString(nowString, fontMetrics.getDescent(), fontMetrics.getAscent());
							}
							else if(timestampPosition == 2) { //top right
								if(timestampColor) graphics.setColor(new Color(0, 0, 0, timestampTransparency));
								else graphics.setColor(new Color(255, 255, 255, timestampTransparency));
						        graphics.fillRect(width - stringWidth - 2 * fontMetrics.getDescent(), 0, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        if(timestampColor) graphics.setColor(Color.white);
						        else graphics.setColor(Color.black);
						        graphics.drawString(nowString, width - stringWidth - fontMetrics.getDescent(), fontMetrics.getAscent());
							}
							else if(timestampPosition == 3) { //bottom left
								if(timestampColor) graphics.setColor(new Color(0, 0, 0, timestampTransparency));
								else graphics.setColor(new Color(255, 255, 255, timestampTransparency));
						        graphics.fillRect(0, height - stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        if(timestampColor) graphics.setColor(Color.white);
						        else graphics.setColor(Color.black);
						        graphics.drawString(nowString, fontMetrics.getDescent(), height - stringHeight + fontMetrics.getAscent());
							}
							else if(timestampPosition == 4) { //bottom right
								if(timestampColor) graphics.setColor(new Color(0, 0, 0, timestampTransparency));
								else graphics.setColor(new Color(255, 255, 255, timestampTransparency));
						        graphics.fillRect(width - stringWidth - 2 * fontMetrics.getDescent(), height - stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        if(timestampColor) graphics.setColor(Color.white);
						        else graphics.setColor(Color.black);
						        graphics.drawString(nowString, width - stringWidth - fontMetrics.getDescent(), height - stringHeight + fontMetrics.getAscent());
							}
						}
						
						if(errorCounter > maxFps * (isIpCamera ? 5 : 1) + 2) {
							String string = "WEBCAM ERROR";
							Rectangle2D rect = fontMetrics.getStringBounds(string, graphics);
					        int stringWidth = (int)Math.round(rect.getWidth());
					        int stringHeight = (int)Math.round(rect.getHeight());
							
							if(timestampPosition == 0 || !timestamp) {
								graphics.setColor(Color.yellow);
						        graphics.fillRect(width / 2 - stringWidth / 2 - fontMetrics.getDescent(), height / 2 - stringHeight / 2 + stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        graphics.setColor(Color.red);
						        graphics.drawString(string, width / 2 - stringWidth / 2, height / 2 - stringHeight / 2 + stringHeight + fontMetrics.getAscent());
							}
							else if(timestampPosition == 1) {
								graphics.setColor(Color.yellow);
						        graphics.fillRect(0, stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        graphics.setColor(Color.red);
						        graphics.drawString(string, fontMetrics.getDescent(), stringHeight + fontMetrics.getAscent());
							}
							else if(timestampPosition == 2) {
								graphics.setColor(Color.yellow);
						        graphics.fillRect(width - stringWidth - 2 * fontMetrics.getDescent(), stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        graphics.setColor(Color.red);
						        graphics.drawString(string, width - stringWidth - fontMetrics.getDescent(), stringHeight + fontMetrics.getAscent());
							}
							else if(timestampPosition == 3) {
								graphics.setColor(Color.yellow);
						        graphics.fillRect(0, height - stringHeight - stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        graphics.setColor(Color.red);
						        graphics.drawString(string, fontMetrics.getDescent(), height - stringHeight - stringHeight + fontMetrics.getAscent());
							}
							else if(timestampPosition == 4) {
								graphics.setColor(Color.yellow);
						        graphics.fillRect(width - stringWidth - 2 * fontMetrics.getDescent(), height - stringHeight - stringHeight, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
						        graphics.setColor(Color.red);
						        graphics.drawString(string, width - stringWidth - fontMetrics.getDescent(), height - stringHeight - stringHeight + fontMetrics.getAscent());
							}
						}

						graphics.dispose();

						try {
							turboJpegCompressor.setJPEGQuality(quality);
							turboJpegCompressor.setSourceImage(image, 0, 0, 0, 0);
							byte[] jpegBuf = turboJpegCompressor.compress(TJ.FLAG_ACCURATEDCT);
							byte[] byteImage = new byte[turboJpegCompressor.getCompressedSize()];
							System.arraycopy(jpegBuf, 0, byteImage, 0, byteImage.length);

							lastWebcamImage.setBytes(byteImage);
							lastSnapshot.update(nowDateTime, byteImage);
						} catch (Exception e) {
							logger.logException(e);

							lastWebcamImage.setBytes(null);
							lastSnapshot.update(null, null);
						}

						long interval = 750 / maxFps; // A little faster than fps
						long delay = interval - (System.nanoTime() - lastTimeProcess) / 1000000;
						if(delay > 0 && delay <= 1000) {
							try { Thread.sleep(delay); } catch (InterruptedException e) { }
						}
						lastTimeProcess = System.nanoTime();
					}
				} catch (Exception e) {
					logger.logException(e);
				}
			}
		}.start();
		
		new Thread() {
			public void run() {
				while(!killThread) {
					System.gc();
					
					try { Thread.sleep(120000); } catch (InterruptedException e) { }
				}
			}
		}.start();

		logger.logLn("Server ready");
	}

	private static class WebServer extends NanoHTTPD {
		public WebServer(int port) {
			super(port);
		}

		@Override
		public Response serve(IHTTPSession session) {
			String uri = session.getUri();

			if(uri.equals("/")) {
				logger.logLn("Opened http connection from: " + session.getRemoteIpAddress());

				try {
					InputStream in = WebcamServer.class.getResourceAsStream("/www/index.html");
					if(in == null) {
						logger.logLn("Http 500 from: " + session.getRemoteIpAddress() + " uri: " + uri);
						return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "500 - Internal Server Error");
					}

					Scanner scanner = new Scanner(in);
					scanner.useDelimiter("\\A");
					String msg = scanner.hasNext() ? scanner.next() : "";
					scanner.close();

					msg = msg.replaceAll("@title@", wwwTitle);
					msg = msg.replaceAll("@interval@", "" + (1000 / maxFps));
					msg = msg.replaceAll("@width@", "" + width);
					msg = msg.replaceAll("@height@", "" + height);

					return Response.newFixedLengthResponse(Status.OK, "text/html", msg);
				} catch (Exception e) {
					logger.logLn("Http 500 from: " + session.getRemoteIpAddress() + " uri: " + uri);
					return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "500 - Internal Server Error");
				}
			}
			else if(uri.equals("/img.jpg")) {
				byte[] bytes = lastWebcamImage.getBytes();
				if(bytes != null) return Response.newFixedLengthResponse(Status.OK, "image/jpeg", bytes);
				else return Response.newFixedLengthResponse(Status.OK, "image/jpeg", emptyImage);
			}
			else {
				try {
					InputStream in = WebcamServer.class.getResourceAsStream("/www" + uri);
					if(in == null) {
						logger.logLn("Http 404 from: " + session.getRemoteIpAddress() + " uri: " + uri);
						return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "404 - Not Found");
					}

					String mime = "text/plain";
					if(uri.toLowerCase().endsWith(".png")) mime = "image/png";
					else if(uri.toLowerCase().endsWith(".xml")) mime = "application/xml";
					else if(uri.toLowerCase().endsWith(".ico")) mime = "image/x-icon";
					else if(uri.toLowerCase().endsWith(".json")) mime = "text/plain";
					else if(uri.toLowerCase().endsWith(".svg")) mime = "image/svg+xml";

					return Response.newChunkedResponse(Status.OK, mime, in);
				} catch (Exception e) {
					logger.logLn("Http 404 from: " + session.getRemoteIpAddress() + " uri: " + uri);
					return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "404 - Not Found");
				}
			}
		}
	}
	
	private static boolean errorBufferedImage(BufferedImage img1, BufferedImage img2) {
		if(img1 == null || img2 == null) return true;
		
		DataBuffer db1 = img1.getRaster().getDataBuffer();
		DataBuffer db2 = img2.getRaster().getDataBuffer();
		try {
			if(db1 instanceof DataBufferByte && db2 instanceof DataBufferByte) return Arrays.equals(((DataBufferByte) db1).getData(), ((DataBufferByte) db2).getData());
			else if(db1 instanceof DataBufferInt && db2 instanceof DataBufferInt) return Arrays.equals(((DataBufferInt) db1).getData(), ((DataBufferInt) db2).getData());
			else if(db1 instanceof DataBufferUShort && db2 instanceof DataBufferUShort) return Arrays.equals(((DataBufferUShort) db1).getData(), ((DataBufferUShort) db2).getData());
			else if(db1 instanceof DataBufferShort && db2 instanceof DataBufferShort) return Arrays.equals(((DataBufferShort) db1).getData(), ((DataBufferShort) db2).getData());
		} catch (Exception e) {
			logger.logException(e);
		}
		
		return true;
	}
	
	private static void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) deleteDir(f);
		}
		file.delete();
	}

	private static int readInt(String message, Scanner scanner, boolean echo, int min, int max) {
		int out = 0;
		boolean exception = false;
		do {
			try {
				System.out.print(message);
				String s = scanner.nextLine().trim();
				if(echo) System.out.println(s);
				out = Integer.parseInt(s);
				exception = false;
			} catch (NumberFormatException e) {
				exception = true;
			} catch (Exception e) {
				System.out.println();
				logger.logLn("End of file");
				System.exit(0);
			}
		} while (out < min || out > max || exception);
		
		System.out.println();
		
		return out;
	}

	private static String readString(String message, Scanner scanner, boolean echo, String[] options, boolean toLowerCase, int minimunLenght) {
		String out = null;
		do {
			try {
				System.out.print(message);
				out = scanner.nextLine().trim();
				if(echo) System.out.println(out);
				if(toLowerCase) out = out.toLowerCase();
			} catch (Exception e) {
				System.out.println();
				logger.logLn("End of file");
				System.exit(0);
			}
		} while ((options != null && !Arrays.asList(options).contains(out)) || out.length() < minimunLenght);
		
		System.out.println();
		
		return out;
	}
}
