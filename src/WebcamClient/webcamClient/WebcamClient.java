package webcamClient;

import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import sharedObjects.*;

public class WebcamClient extends JFrame {
	private static final long serialVersionUID = -6230405403723444365L;
	
	private static final String version = "1.2";
	public static final Logger logger = new Logger();
	public static Font iconFont = null;
	private static WebcamClient thisFrame;
	private static JDialog errorDialog;
	private static boolean scanAtStartup = true;

	private static JPanel contentPane;
	private static JLabel lblLiveTcpPort;
	private static JLabel lblHistoryTcpPort;
	private static JLabel lblDiscoveryUdpPort;
	private static JTextField livePortTextField;
	private static JTextField historyPortTextField;
	private static JTextField discoveryPortTextField;
	private static JLabel lblAddress;
	private static JLabel lblPassword;
	private static JPasswordField passwordField;
	private static JComboBox<String> addressComboBox;
	private static JButton scanServerButton;
	private static JButton liveViewButton;
	private static JButton historyViewButton;
	
	public static void main(String[] args) {
		logger.logLn("Webcam client " + version + " started");

		logger.logLn("Initializing");
		
		JOptionPane.setDefaultLocale(Locale.ENGLISH);
		Locale.setDefault(new Locale("en", "US"));

		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					
					// Fix for minimum thumb size
					LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
					UIDefaults defaults = lookAndFeel.getDefaults();
					defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));
					
					String os = System.getProperty("os.name");
					if (os.toLowerCase().contains("darwin") || os.toLowerCase().contains("mac")) {
						Collection<InputMap> ims = new ArrayList<>();
						ims.add((InputMap) UIManager.get("TextField.focusInputMap"));
						ims.add((InputMap) UIManager.get("TextArea.focusInputMap"));
						ims.add((InputMap) UIManager.get("EditorPane.focusInputMap"));
						ims.add((InputMap) UIManager.get("FormattedTextField.focusInputMap"));
						ims.add((InputMap) UIManager.get("PasswordField.focusInputMap"));
						ims.add((InputMap) UIManager.get("TextPane.focusInputMap"));

						int meta = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
						for (InputMap im : ims) {
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, meta), DefaultEditorKit.copyAction);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, meta), DefaultEditorKit.pasteAction);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, meta), DefaultEditorKit.cutAction);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, meta), DefaultEditorKit.selectAllAction);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.logException(e);
		}
		
		try {
			iconFont = Font.createFont(Font.TRUETYPE_FONT, WebcamClient.class.getResourceAsStream("MaterialIcons-Regular.ttf"));
			iconFont = iconFont.deriveFont(23.0f);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(iconFont);
		} catch (Exception e) {
			logger.logException(e);
		}
		
		thisFrame = new WebcamClient();
		
		new Thread() {
			public void run() {
				parseArguments(args);
				
				passwordField.requestFocus();
				if(scanAtStartup) scanServers();
			}
		}.start();
		
		new Thread() {
			public void run() {
				while(thisFrame.isVisible()) {
					System.gc();
					
					try { Thread.sleep(120000); } catch (InterruptedException e) { }
				}
			}
		}.start();
		
		logger.logLn("Ready");
	}

	public WebcamClient() {
		setTitle("Webcam client " + version);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(444, 152);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		lblLiveTcpPort = new JLabel("Live TCP port:");
		lblLiveTcpPort.setHorizontalAlignment(SwingConstants.RIGHT);
		lblLiveTcpPort.setBounds(6, 6, 118, 27);
		contentPane.add(lblLiveTcpPort);
		
		lblHistoryTcpPort = new JLabel("History TCP port:");
		lblHistoryTcpPort.setHorizontalAlignment(SwingConstants.RIGHT);
		lblHistoryTcpPort.setBounds(6, 36, 118, 27);
		contentPane.add(lblHistoryTcpPort);
		
		lblDiscoveryUdpPort = new JLabel("Discovery UDP port:");
		lblDiscoveryUdpPort.setHorizontalAlignment(SwingConstants.RIGHT);
		lblDiscoveryUdpPort.setBounds(6, 66, 118, 27);
		contentPane.add(lblDiscoveryUdpPort);
		
		livePortTextField = new JTextField();
		livePortTextField.setText("8081");
		livePortTextField.setBounds(124, 5, 60, 27);
		contentPane.add(livePortTextField);
		livePortTextField.setColumns(10);
		
		historyPortTextField = new JTextField();
		historyPortTextField.setText("8082");
		historyPortTextField.setBounds(124, 36, 60, 27);
		contentPane.add(historyPortTextField);
		historyPortTextField.setColumns(10);
		
		discoveryPortTextField = new JTextField();
		discoveryPortTextField.setText("8084");
		discoveryPortTextField.setBounds(124, 66, 60, 27);
		contentPane.add(discoveryPortTextField);
		discoveryPortTextField.setColumns(10);
		
		lblAddress = new JLabel("Address:");
		lblAddress.setHorizontalAlignment(SwingConstants.RIGHT);
		lblAddress.setBounds(195, 36, 62, 27);
		contentPane.add(lblAddress);
		
		lblPassword = new JLabel("Password:");
		lblPassword.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPassword.setBounds(195, 66, 62, 27);
		contentPane.add(lblPassword);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(257, 66, 180, 27);
		contentPane.add(passwordField);
		
		addressComboBox = new JComboBox<String>();
		addressComboBox.setEditable(true);
		addressComboBox.setBounds(257, 36, 180, 27);
		contentPane.add(addressComboBox);
		
		scanServerButton = new JButton("Scan servers");
		scanServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scanServers();
			}
		});
		scanServerButton.setBounds(257, 6, 180, 27);
		contentPane.add(scanServerButton);
		
		liveViewButton = new JButton("Live view");
		liveViewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int port = 0;
				try {
					port = Integer.parseInt(livePortTextField.getText().trim());
				} catch (Exception ex) { }
				
				if(port > 0 && port < 65535) {
					String addr = (String) addressComboBox.getSelectedItem();
					if(addr == null) addr = "";
					else addr = addr.trim();
					
					String pass = new String(passwordField.getPassword());
					pass = pass.trim();
					
					new LiveViewFrame(thisFrame, addr.length() > 0 ? addr : "127.0.0.1", pass, port);
				}
				else errorMessage("Invalid live port");
			}
		});
		liveViewButton.setBounds(6, 105, 210, 40);
		contentPane.add(liveViewButton);
		
		historyViewButton = new JButton("History view");
		historyViewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int port = 0;
				try {
					port = Integer.parseInt(historyPortTextField.getText().trim());
				} catch (Exception ex) { }
				
				if(port > 0 && port < 65535) {
					String addr = (String) addressComboBox.getSelectedItem();
					if(addr == null) addr = "";
					else addr = addr.trim();
					
					String pass = new String(passwordField.getPassword());
					pass = pass.trim();
					
					new HistoryViewFrame(thisFrame, addr.length() > 0 ? addr : "127.0.0.1", pass, port);
				}
				else errorMessage("Invalid history port");
			}
		});
		historyViewButton.setBounds(227, 105, 210, 40);
		contentPane.add(historyViewButton);
		
		setVisible(true);
		setSize(getSize().width + getInsets().left + getInsets().right, getSize().height + getInsets().top + getInsets().bottom);
	}
	
	private static void parseArguments(String[] args) {
		if(args == null) return;
		
		String command = null;
		for(String s : args) {
			s = s.trim();
			if(s.equals("-noscan")) {
				scanAtStartup = false;
				command = null;
			}
			else if(s.equals("-liveport")) command = s;
			else if(s.equals("-historyport")) command = s;
			else if(s.equals("-discoveryport")) command = s;
			else if(s.equals("-address")) command = s;
			else if(s.equals("-password")) command = s;
			else if(s.equals("-openlive")) {
				liveViewButton.doClick();
				command = null;
			}
			else if(s.equals("-openhistory")) {
				historyViewButton.doClick();
				command = null;
			}
			else if(command != null) {
				if(command.equals("-liveport")) livePortTextField.setText(s);
				else if(command.equals("-historyport")) historyPortTextField.setText(s);
				else if(command.equals("-discoveryport")) discoveryPortTextField.setText(s);
				else if(command.equals("-address")) {
					addressComboBox.addItem(s);
					addressComboBox.setSelectedIndex(addressComboBox.getItemCount() - 1);
				}
				else if(command.equals("-password")) passwordField.setText(s);
				command = null;
			}
		}
	}
	
	private static void scanServers() {
		new Thread() {
			public void run() {
				addressComboBox.setEnabled(false);
				scanServerButton.setEnabled(false);
				addressComboBox.removeAllItems();
				
				logger.logLn("Scanning servers");
				
				int port = 0;
				try {
					port = Integer.parseInt(discoveryPortTextField.getText().trim());
				} catch (Exception e) { }
				
				if(port > 0 && port <= 65535) {
					try {
						java.util.List<String> outList = new ArrayList<>();
						
						byte[] receiveData = new byte[1024];
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						byte[] sendData = "_WEBCAM DISCOVERY PACKET_".getBytes();

						DatagramSocket discoverySocket = new DatagramSocket();
						discoverySocket.setSoTimeout(1000);

						BooleanWrapper transmit = new BooleanWrapper(true);
						final int scanPort = port;
						new Thread() {
							public void run() {
								try {
									while(transmit.getValue()) {
										try {
											discoverySocket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getLoopbackAddress(), scanPort));
										} catch (Exception e) {
											logger.logException(e);
										}

										try {
											discoverySocket.send(new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), scanPort));
										} catch (Exception e) {
											logger.logException(e);
										}

										Thread.sleep(75);
									}
								} catch (Exception e) { }
							}
						}.start();

						long time = System.nanoTime() + 975000000l;
						while (System.nanoTime() < time) {
							try {
								discoverySocket.receive(receivePacket);
								String element = receivePacket.getAddress().getHostAddress();
								if(outList.contains(element)) continue;
								if (InetAddress.getByName("127.0.0.1").getHostAddress().equals(element)) outList.add(0, receivePacket.getAddress().getHostAddress());
								else outList.add(receivePacket.getAddress().getHostAddress());
							} catch (SocketTimeoutException e) {
								break;
							} catch (Exception e) {
								logger.logException(e);
							}
						}

						transmit.setValue(false);
						discoverySocket.close();
						
						for(String addr : outList) addressComboBox.addItem(addr);
					} catch (Exception e) {
						logger.logException(e);
					}
				}
				else errorMessage("Invalid discovery port");
				
				addressComboBox.setEnabled(true);
				scanServerButton.setEnabled(true);
			}
		}.start();
	}
	
	private static void errorMessage(String err) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				if(errorDialog != null) errorDialog.dispose();
				JOptionPane errorPanel = new JOptionPane(err, JOptionPane.ERROR_MESSAGE);
				errorDialog = errorPanel.createDialog(thisFrame, "Error");
				errorDialog.setVisible(true);
			}
		});
	}
}
