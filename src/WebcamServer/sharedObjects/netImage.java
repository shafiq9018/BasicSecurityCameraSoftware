package sharedObjects;

public class netImage {
	private volatile byte[] byteImage = null;
	private volatile String dir = null, file = null;
	private volatile long n = 0;

	public netImage() {
		
	}
	
	public netImage(byte[] byteImage, String dir, String file) {
		this.byteImage = byteImage;
		this.dir = dir;
		this.file = file;
	}
	
	public netImage(byte[] byteImage, String dir, String file, long n) {
		this.byteImage = byteImage;
		this.dir = dir;
		this.file = file;
		this.n = n;
	}
	
	public netImage(long n) {
		this.n = n;
	}
	
	public synchronized long getNumber() {
		return n;
	}
	
	public synchronized void setLocation(String dir, String file) {
		this.dir = dir;
		this.file = file;
	}
	
	public synchronized String getDir() {
		return dir;
	}
	
	public synchronized String getFile() {
		return file;
	}
	
	public synchronized void setBytes(byte[] byteImage) {
		this.byteImage = byteImage;
	}
	
	public synchronized byte[] getBytes() {
		return byteImage;
	}
}
