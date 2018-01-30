package webcamClient;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = -3760276416077100890L;
	
	private volatile byte[] byteImage = null;
	private volatile BufferedImage bufferedImage = null;
	private volatile String message = null;

	public ImagePanel() {
		setLayout(null);
		
		JButton button = new JButton("\ue412");
		button.setFont(WebcamClient.iconFont);
		button.setMargin(new Insets(-100, -100, -100, -100));
		button.setBounds(0, 0, 27, 27);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						byte[] compressed = byteImage;
						if(compressed == null) return;
						
						JFileChooser dialog = new JFileChooser();
						FileNameExtensionFilter filter = new FileNameExtensionFilter("Picture", "jpg");
						dialog.setAcceptAllFileFilterUsed(false);
						dialog.addChoosableFileFilter(filter);
						
						if(dialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
							File file = dialog.getSelectedFile();
							if(!file.getName().endsWith(".jpg")) file = new File(file.getParentFile(), file.getName() + ".jpg");

							if(file.isFile() && file.exists()) {
								int response = JOptionPane.showConfirmDialog(null, "Overwrite the file?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
								if(response != JOptionPane.YES_OPTION) file = new File(file.getParentFile(), "NEW " + file.getName());
							}

							try{
								FileOutputStream fos = new FileOutputStream(file);
								fos.write(compressed);
								fos.close();
							} catch(Exception e) {
								WebcamClient.logger.logException(e);
							}
						}
					}
				});
			}
		});
		add(button);
	}
	
	public synchronized byte[] getByteImage() {
		return byteImage;
	}

	public synchronized void update(byte[] byteImage, BufferedImage bufferedImage, String message) {
		this.byteImage = byteImage;
		this.bufferedImage = bufferedImage;
		this.message = message;
		
		this.repaint();
	}
	
	public synchronized void updateMessage(String message) {
		this.message = message;
		
		this.repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		BufferedImage imageCopy = bufferedImage;
		if(imageCopy != null) {
			double ratio = Math.min((double)this.getWidth() / imageCopy.getWidth(), (double)this.getHeight() / imageCopy.getHeight());
			int w = (int)Math.round(imageCopy.getWidth() * ratio), h = (int)Math.round(imageCopy.getHeight() * ratio);
			int wOff = (this.getWidth() - w) / 2, hOff = (this.getHeight() - h) / 2;
			graphics.drawImage(imageCopy, wOff, hOff, w, h, null);
		}
		
		String messageCopy = message;
		if(messageCopy != null) {
			graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
			FontMetrics fontMetrics = graphics.getFontMetrics();
			Rectangle2D rect = fontMetrics.getStringBounds(messageCopy, graphics);
			int stringWidth = (int)Math.round(rect.getWidth());
			int stringHeight = (int)Math.round(rect.getHeight());
			
			if(imageCopy != null) {
				graphics.setColor(Color.white);
				graphics.fillRect(this.getWidth() / 2 - stringWidth / 2 - fontMetrics.getDescent(), this.getHeight() / 2 - stringHeight / 2, stringWidth + 2 * fontMetrics.getDescent(), stringHeight);
			}
			graphics.setColor(Color.black);
			graphics.drawString(messageCopy, this.getWidth() / 2 - stringWidth / 2, this.getHeight() / 2 - stringHeight / 2 + fontMetrics.getAscent());
		}
	}
}