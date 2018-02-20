package webcamClient;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.spec.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;
import java.util.concurrent.*;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import sharedObjects.*;

public class LiveViewFrame extends JFrame {
	private static final long serialVersionUID = 2747390727370492324L;
	
	private volatile ImagePanel panel = new ImagePanel(true);
	private volatile NetworkKey networkKey = new NetworkKey("Blowfish", "Blowfish", false); // new NetworkKey("AES", "AES/CBC/PKCS5Padding", true);
	private volatile Client client = null;
	private volatile long frameCounter = 0;
	private volatile int frameRate = 1;
	private volatile boolean firstFrame = true;
	
	public LiveViewFrame(Component parent, String address, String password, int port) {
		setTitle("Live view - " + address + ":" + port);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(200, 150));
		setSize(300, 200);
		setLocationRelativeTo(parent);
		setContentPane(panel);
		
		panel.update(null, null, "Initializing");
		setVisible(true);
		
		new Thread() {
			public void run() {
				try {
					javax.crypto.Cipher.getInstance(networkKey.transformation);
					ExecutorService executor = Executors.newSingleThreadExecutor();
					
					client = new Client(5000000, 5000000);

					Kryo kryo = client.getKryo();
					kryo.register(byte[].class);
					Registration r = kryo.register(NetImage.class);
					kryo.register(NetImage.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());
					r = kryo.register(IntParameter.class);
					kryo.register(IntParameter.class, new CryptoSerializer(r.getSerializer(), networkKey), r.getId());

					client.addListener(new Listener() {
						long lastTimeTx = 0;

						public void connected (Connection connection) {
							try {
								WebcamClient.logger.logLn("Live connection opened to " + connection.getRemoteAddressTCP().getAddress().getHostAddress());
								panel.update(null, null, "Connected");
								
								connection.setKeepAliveTCP(2000);
								connection.setTimeout(12000);
								connection.setIdleThreshold(0.01f);

								connection.sendTCP(new IntParameter("fps", 0));
								connection.sendTCP(new IntParameter("start", 0));
							} catch (Exception e) {
								WebcamClient.logger.logException(e);
							}
						}

						public void disconnected (Connection connection) {
							WebcamClient.logger.logLn("Live connection closed");
							panel.update(null, null, "Disconnected");
						}

						public void received (Connection connection, Object object) {
							try {
								if(object == null) return;
								else if(object instanceof NetImage) {
									executor.submit(new Runnable() {
										public void run() {
											try {
												byte[] imageBytes = ((NetImage) object).getBytes();
												BufferedImage img = null;
												if(imageBytes != null) {
													InputStream in = new ByteArrayInputStream(imageBytes);
													ImageInputStream iis = new MemoryCacheImageInputStream(in);
													img = ImageIO.read(iis);
												}
												if(firstFrame && img != null) {
													firstFrame = false;
													int x = Toolkit.getDefaultToolkit().getScreenSize().width / 2;
													int y = (int)Math.round(x / ((double)img.getWidth() / (double)img.getHeight()));
													setSize(x + getInsets().left + getInsets().right, y + getInsets().top + getInsets().bottom);
												}
												panel.update(imageBytes, img, null);
												
												File path = panel.getRecordingPath();
												if(path != null && imageBytes != null) {
													String imgFile;
													if(((NetImage) object).getFile() != null) imgFile = ((NetImage) object).getFile();
													else imgFile = "Snapshot_" + System.currentTimeMillis() + ".jpg";
													File file = new File(path, imgFile);
													FileOutputStream fos = new FileOutputStream(file);
													fos.write(imageBytes);
													fos.close();
												}
											} catch (Exception e) {
												WebcamClient.logger.logException(e);
											}

											long interval = 1000 / frameRate;
											long delay = interval - (System.nanoTime() - lastTimeTx) / 1000000;
											if(delay > 0 && delay <= 1000) {
												try { Thread.sleep(delay); } catch (InterruptedException e) { }
											}

											connection.sendTCP(new NetImage(++frameCounter));
											lastTimeTx = System.nanoTime();
										}
									});
								}
								else if(object instanceof IntParameter) {
									String key = ((IntParameter) object).getKey();
									if(key == null) key = "";

									switch(key) {
									case "fps":
										frameRate = (int) ((IntParameter) object).getValue();
										break;
									case "start":
										frameCounter = ((IntParameter) object).getValue();
										connection.sendTCP(new NetImage(++frameCounter));
										lastTimeTx = System.nanoTime();
										break;
									default:
										break;
									}
								}
							} catch (Exception e) {
								WebcamClient.logger.logException(e);
							}
						}

						public void idle (Connection connection) {

						}
					});
					client.start();
					
					networkKey.key = new SecretKeySpec(MessageDigest.getInstance("MD5").digest(password.getBytes()), networkKey.algorithm);
					if(networkKey.hasIv) networkKey.iv = MessageDigest.getInstance("MD5").digest(networkKey.key.getEncoded());
					
					panel.update(null, null, "Connecting");
					client.connect(12000, InetAddress.getByName(address), port);
				} catch (Exception e) {
					WebcamClient.logger.logException(e);
					panel.update(null, null, "Error");
				}
			}
		}.start();
	}
	
	@Override
	public void dispose() {
		try { client.stop(); } catch (Exception ex) { }
		super.dispose();
	}
}
