package sharedObjects;

public class NetFileList {
	private volatile String[] files = null;
	private volatile int capacity = 0;
	private volatile String dir = null;
	private volatile long n = 0;
	
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
