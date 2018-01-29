package sharedObjects;

public class NetImage {
	private byte[] byteImage = null;
	private String dir = null, file = null;
	private long n = 0;

	public NetImage() {
		
	}
	
	public NetImage(long n) {
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
