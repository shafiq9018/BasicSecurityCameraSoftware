package webcamClient;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.spec.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.GroupLayout.*;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import sharedObjects.*;

public class HistoryViewFrame extends JFrame {
	private static final long serialVersionUID = -702757229682623362L;
	
	private volatile NetworkKey networkKey = new NetworkKey("Blowfish", "Blowfish", false); // new NetworkKey("AES", "AES/CBC/PKCS5Padding", true);
	private volatile Client client = null;
	private volatile long frameCounter = 0;
	private volatile boolean busy = false, enableFileChangeListener = true;
	private volatile int lastDownloadFileFolder = -1, lastDownloadFileFile = -1, lastDownloadFolderFolder = -1;
	private volatile java.util.List<String> tempList = null;
	private volatile String textOverlay = null;
	private volatile ExecutorService executor = Executors.newSingleThreadExecutor();
	private volatile DefaultListModel<String> dlmFolder = new DefaultListModel<String>(), dlmFile = new DefaultListModel<String>();

	private JPanel contentPane;
	private JList<String> folderList;
	private JList<String> fileList;
	private ImagePanel imagePanel = new ImagePanel(false);
	private JButton prev1Button;
	private JButton next1Button;
	private JScrollPane folderScroller;
	private JScrollPane fileScroller;
	private JPanel listPanel;
	private JPanel viewPanel;
	private JPanel buttonControlPanel;
	private JPanel buttonListPanel;
	private JButton prev10Button;
	private JButton next10Button;
	private JButton prev100Button;
	private JButton next100Button;
	private JButton prev1000Button;
	private JButton next1000Button;
	private JButton downloadButton;
	private JButton refreshButton;
	private JToggleButton playButton;
	
	public HistoryViewFrame(Component parent, String address, String password, int port) {
		setTitle("History view - " + address + ":" + port);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(870, 350));
		setSize(900, 500);
		setLocationRelativeTo(parent);
		
		imagePanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		folderScroller = new JScrollPane();
		folderScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		folderList = new JList<String>(dlmFolder);
		folderScroller.setViewportView(folderList);
		folderList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(folderList.getSelectedIndices().length > 1) return;
				
				folderList.ensureIndexIsVisible(folderList.getSelectedIndex());
				
				executor.submit(new Runnable() {
					public void run() {
						downloadFileList(folderList.getSelectedIndex());
					}
				});
			}
		});
		folderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		folderList.setLayoutOrientation(JList.VERTICAL);
		
		fileScroller = new JScrollPane();
		fileScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		fileList = new JList<String>(dlmFile);
		fileScroller.setViewportView(fileList);
		fileList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(fileList.getSelectedIndices().length > 1) return;
				
				fileList.ensureIndexIsVisible(fileList.getSelectedIndex());
				
				if(!enableFileChangeListener) return;
				
				executor.submit(new Runnable() {
					public void run() {
						downloadImage(folderList.getSelectedIndex(), fileList.getSelectedIndex(), null);
					}
				});
			}
		});
		fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		fileList.setLayoutOrientation(JList.VERTICAL);
		
		prev1Button = new JButton("<");
		prev1Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection - 1;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		next1Button = new JButton(">");
		next1Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection + 1;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		prev10Button = new JButton("< 10");
		prev10Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection - 10;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		next10Button = new JButton("10 >");
		next10Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection + 10;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		prev100Button = new JButton("< 100");
		prev100Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection - 100;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		next100Button = new JButton("100 >");
		next100Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection + 100;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		prev1000Button = new JButton("< 1000");
		prev1000Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection - 1000;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		next1000Button = new JButton("1000 >");
		next1000Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int prevSelection = fileList.getSelectedIndex();
				int selection = prevSelection + 1000;
				if(selection < 0) selection = 0;
				if(selection >= dlmFile.size()) selection = dlmFile.size() - 1;
				if(selection != prevSelection) fileList.setSelectedIndex(selection);
			}
		});
		
		playButton = new JToggleButton("\ue037");
		playButton.setFont(WebcamClient.iconFont);
		playButton.setMinimumSize(new Dimension(27, 27));
		playButton.setMaximumSize(new Dimension(27, 27));
		playButton.setPreferredSize(new Dimension(27, 27));
		playButton.setMargin(new Insets(-100, -100, -100, -100));
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(playButton.isSelected()) {
					executor.submit(new Runnable() {
						public void run() {
							play();
						}
					});
				}
			}
		});
		
		downloadButton = new JButton("Download selection");
		downloadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				executor.submit(new Runnable() {
					public void run() {
						downloadSelection(folderList.getSelectedIndex(), fileList.getSelectedIndices());
					}
				});
			}
		});
		
		refreshButton = new JButton("Refresh list");
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				executor.submit(new Runnable() {
					public void run() {
						downloadFolderList();
					}
				});
			}
		});
		
		listPanel = new JPanel();
		viewPanel = new JPanel();
		buttonControlPanel = new JPanel();
		buttonControlPanel.add(prev1000Button);
		buttonControlPanel.add(prev100Button);
		buttonControlPanel.add(prev10Button);
		buttonControlPanel.add(prev1Button);
		buttonControlPanel.add(playButton);
		buttonControlPanel.add(next1Button);
		buttonControlPanel.add(next10Button);
		buttonControlPanel.add(next100Button);
		buttonControlPanel.add(next1000Button);
		buttonListPanel = new JPanel();
		buttonListPanel.add(refreshButton);
		buttonListPanel.add(downloadButton);
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
						.addComponent(listPanel, 300, 300, 300)
						.addComponent(viewPanel))
				);
		gl_contentPane.setVerticalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING, gl_contentPane.createSequentialGroup()
						.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(viewPanel)
								.addComponent(listPanel)))
				);
		
		GroupLayout gl_viewPanel = new GroupLayout(viewPanel);
		gl_viewPanel.setHorizontalGroup(
				gl_viewPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_viewPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(buttonControlPanel))
				.addGroup(gl_viewPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(imagePanel))
				);
		gl_viewPanel.setVerticalGroup(
				gl_viewPanel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_viewPanel.createSequentialGroup()
						.addGap(3)
						.addComponent(imagePanel)
						.addGap(3)
						.addComponent(buttonControlPanel, 37, 37, 37))
				);
		
		GroupLayout gl_listPanel = new GroupLayout(listPanel);
		gl_listPanel.setHorizontalGroup(
				gl_listPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_listPanel.createSequentialGroup()
						.addComponent(folderScroller))
				.addGroup(gl_listPanel.createSequentialGroup()
						.addComponent(fileScroller))
				.addGroup(gl_listPanel.createSequentialGroup()
						.addComponent(buttonListPanel, 300, 300, 300))
				);
		gl_listPanel.setVerticalGroup(
				gl_listPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_listPanel.createSequentialGroup()
						.addComponent(folderScroller, 150, 150, 150)
						.addGap(5)
						.addComponent(fileScroller)
						.addComponent(buttonListPanel, 37, 37, 37))
				);
		
		listPanel.setLayout(gl_listPanel);
		viewPanel.setLayout(gl_viewPanel);
		contentPane.setLayout(gl_contentPane);
		
		imagePanel.update(null, null, "Initializing");
		setVisible(true);
		
		new Thread() {
			public void run() {
				try {
					javax.crypto.Cipher.getInstance(networkKey.transformation);
					
					client = new Client(5000000, 5000000);

					Kryo kryo = client.getKryo();
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

					client.addListener(new Listener() {
						public void connected (Connection connection) {
							try {
								WebcamClient.logger.logLn("History connection opened to " + connection.getRemoteAddressTCP().getAddress().getHostAddress());
								imagePanel.update(null, null, "Connected");
								
								connection.setKeepAliveTCP(2000);
								connection.setTimeout(12000);
								connection.setIdleThreshold(0.01f);

								connection.sendTCP(new IntParameter("start", 0));
							} catch (Exception e) {
								WebcamClient.logger.logException(e);
							}
						}

						public void disconnected (Connection connection) {
							WebcamClient.logger.logLn("History connection closed");
							imagePanel.update(null, null, "Disconnected");
						}

						public void received (Connection connection, Object object) {
							try {
								if(object == null) return;
								else if(object instanceof BooleanWrapper) {
									busy = ((BooleanWrapper) object).getValue();
								}
								else if(object instanceof NetImage) {
									byte[] imageBytes = ((NetImage) object).getBytes();
									BufferedImage img = null;
									if(imageBytes != null) {
										InputStream in = new ByteArrayInputStream(imageBytes);
										ImageInputStream iis = new MemoryCacheImageInputStream(in);
										img = ImageIO.read(iis);
									}
									imagePanel.update(imageBytes, img, textOverlay);
								}
								else if(object instanceof NetFileList) {
									String[] files = ((NetFileList) object).getFiles();
									if(files != null) {
										int max = ((NetFileList) object).getCapacity();
										for(int i = 0; i < max; i++) tempList.add(files[i]);
									}
									imagePanel.update(null, null, tempList.size() + " Files (downloading)");
								}
								else if(object instanceof NetDirList) {
									String[] dirs = ((NetDirList) object).getDirs();
									if(dirs != null) {
										int max = ((NetDirList) object).getCapacity();
										for(int i = 0; i < max; i++) tempList.add(dirs[i]);
									}
									imagePanel.update(null, null, tempList.size() + " Directories (downloading)");
								}
								else if(object instanceof IntParameter) {
									String key = ((IntParameter) object).getKey();
									if(key == null) key = "";

									switch(key) {
									case "start":
										frameCounter = ((IntParameter) object).getValue();
										executor.submit(new Runnable() {
											public void run() {
												downloadFolderList();
											}
										});
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

					imagePanel.update(null, null, "Connecting");
					client.connect(12000, InetAddress.getByName(address), port);
				} catch (Exception e) {
					WebcamClient.logger.logException(e);
					imagePanel.update(null, null, "Error");
				}
			}
		}.start();
	}

	private void downloadFolderList() {
		try {
			if(!client.isConnected()) return;
			
			imagePanel.update(null, null, "Downloading folder list");
			
			BooleanWrapper threadBusy = new BooleanWrapper(true);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					dlmFile.removeAllElements();
					dlmFolder.removeAllElements();
					threadBusy.setValue(false);
				}
			});
			while(threadBusy.getValue()) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}
			
			busy = true;
			tempList = new ArrayList<String>();
			lastDownloadFolderFolder = -1;
			lastDownloadFileFolder = -1;
			lastDownloadFileFile = -1;
			client.sendTCP(new NetDirList(++frameCounter));
			while(busy) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}

			threadBusy.setValue(true);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					for(int i = 0; i < tempList.size(); i++) dlmFolder.addElement(tempList.get(i));
					imagePanel.update(null, null, tempList.size() + " Directories");
					threadBusy.setValue(false);
				}
			});
			while(threadBusy.getValue()) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}
		} catch (Exception e) {
			WebcamClient.logger.logException(e);
		}
	}

	private void downloadFileList(int selection) {
		try {
			if(selection == lastDownloadFolderFolder) return;
			if(!client.isConnected()) return;
			
			imagePanel.update(null, null, "Downloading file list");
			lastDownloadFolderFolder = selection;
			
			BooleanWrapper threadBusy = new BooleanWrapper(true);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					dlmFile.removeAllElements();
					threadBusy.setValue(false);
				}
			});
			while(threadBusy.getValue()) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}
			
			busy = true;
			tempList = new ArrayList<String>();
			lastDownloadFileFolder = -1;
			lastDownloadFileFile = -1;
			NetFileList nfl = new NetFileList(++frameCounter);
			nfl.setDir(dlmFolder.get(selection));
			client.sendTCP(nfl);
			while(busy) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}

			threadBusy.setValue(true);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					for(int i = 0; i < tempList.size(); i++) dlmFile.addElement(tempList.get(i));
					imagePanel.update(null, null, tempList.size() + " Files");
					threadBusy.setValue(false);
				}
			});
			while(threadBusy.getValue()) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}
		} catch (Exception e) {
			WebcamClient.logger.logException(e);
		}
	}

	private void downloadSelection(int folderIndex, int[] selection) {
		try {
			if(selection == null || selection.length < 1) return;
			if(!client.isConnected()) return;
			
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				lastDownloadFileFolder = -1;
				lastDownloadFileFile = -1;
				int selectionSize = selection.length, selectionIndex = 1;
				for(int index : selection) {
					downloadImage(folderIndex, index, "Downloading - " + String.format("%.2f", (double)selectionIndex / (double)selectionSize * 100.0) + "%");
					File out = new File(chooser.getSelectedFile(), dlmFile.get(index));
					try{
						FileOutputStream fos = new FileOutputStream(out);
						fos.write(imagePanel.getByteImage());
						fos.close();
					} catch(Exception e) {
						WebcamClient.logger.logException(e);
					}
					
					selectionIndex++;
				}
				imagePanel.updateMessage(null);
				
				BooleanWrapper threadBusy = new BooleanWrapper(true);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fileList.setSelectedIndex(selection[selection.length - 1]);
						threadBusy.setValue(false);
					}
				});
				while(threadBusy.getValue()) {
					try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
				}
			}
		} catch (Exception e) {
			WebcamClient.logger.logException(e);
		}
	}
	
	private void play() {
		try {
			if(dlmFile.size() < 1) {
				playButton.setSelected(false);
				return;
			}
			if(!client.isConnected()) {
				playButton.setSelected(false);
				return;
			}
			int selection = fileList.getSelectedIndex();
			if(selection < 1) selection = 0;
			
			DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");
			enableFileChangeListener = false;
			
			long lastTimeProcess = System.nanoTime();
			LocalDateTime actualDateTime = LocalDateTime.parse(leaveOnlyTimestamp(dlmFile.getElementAt(selection)), fileFormatter);
			while(selection < dlmFile.size() && playButton.isSelected()) {
				long difference = 0;
				if(selection < dlmFile.size() - 1) {
					LocalDateTime nextDateTime = LocalDateTime.parse(leaveOnlyTimestamp(dlmFile.getElementAt(selection + 1)), fileFormatter);
					
					difference = ChronoUnit.MILLIS.between(actualDateTime, nextDateTime);
					if(difference < 0) difference = 0;
					if(difference > 1000) difference = 1000;
					
					actualDateTime = nextDateTime;
				}
				
				int index = selection;
				BooleanWrapper threadBusy = new BooleanWrapper(true);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fileList.setSelectedIndex(index);
						threadBusy.setValue(false);
					}
				});
				while(threadBusy.getValue()) {
					try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
				}
				downloadImage(folderList.getSelectedIndex(), selection, null);
				
				selection++;
				
				long delay = difference - (System.nanoTime() - lastTimeProcess) / 1000000;
				if(delay > 0 && delay <= 1000) {
					try { Thread.sleep(delay); } catch (InterruptedException e) { }
				}
				lastTimeProcess = System.nanoTime();
			}
			playButton.setSelected(false);
		} catch (Exception e) {
			WebcamClient.logger.logException(e);
		}
		
		enableFileChangeListener = true;
	}

	private void downloadImage(int selectionFolder, int selectionFile, String overlay) {
		try {
			if(selectionFolder < 0 || selectionFolder >= dlmFolder.size() || selectionFile < 0 || selectionFile > dlmFile.size() || (selectionFolder == lastDownloadFileFolder && selectionFile == lastDownloadFileFile)) return;
			if(!client.isConnected()) return;
			
			lastDownloadFileFolder = selectionFolder;
			lastDownloadFileFile = selectionFile;
			
			busy = true;
			textOverlay = overlay;
			NetImage ni = new NetImage(++frameCounter);
			ni.setLocation(dlmFolder.get(selectionFolder), dlmFile.get(selectionFile));
			client.sendTCP(ni);
			while(busy) {
				try { Thread.sleep(0, 100); } catch (InterruptedException e) { }
			}
		} catch (Exception e) {
			WebcamClient.logger.logException(e);
		}
	}
	
	private String leaveOnlyTimestamp(String s) {
		for(int i = 0; i < s.length(); i++) {
			if(Character.isDigit(s.charAt(i))) {
				s = s.substring(i);
				break;
			}
		}
		for(int i = s.length() - 1; i >= 0; i--) {
			if(Character.isDigit(s.charAt(i))) {
				s = s.substring(0, i + 1);
				break;
			}
		}
		return s;
	}
	
	@Override
	public void dispose() {
		try { client.stop(); } catch (Exception ex) { }
		super.dispose();
	}
}
