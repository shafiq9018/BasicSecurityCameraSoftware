package webcamServer;

import java.time.LocalDateTime;

public class Snapshot {
	private LocalDateTime timestamp = null;
	private byte[] image = null;

	public Snapshot() {
		
	}
	
	public synchronized void update(LocalDateTime timestamp, byte[] image) {
		this.timestamp = timestamp;
		this.image = image;
	}
	
	public synchronized LocalDateTime getTimestamp() {
		return timestamp;
	}
	
	public synchronized byte[] getImage() {
		return image;
	}
}
