package sharedObjects;

public class NetFileList {
	private String[] files = null;
	private int capacity = 0;
	private String dir = null;
	private long n = 0;
	
	public NetFileList() {
		
	}
	
	public NetFileList(long n) {
		this.n = n;
	}
	
	public synchronized long getNumber() {
		return n;
	}
	
	public synchronized void setDir(String dir) {
		this.dir = dir;
	}
	
	public synchronized String getDir() {
		return dir;
	}
	
	public synchronized void setFiles(String[] files, int capacity) {
		this.files = files;
		this.capacity = capacity;
	}
	
	public synchronized String[] getFiles() {
		return files;
	}
	
	public synchronized int getCapacity() {
		return capacity;
	}
}
