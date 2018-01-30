package sharedObjects;

public class NetDirList {
	private volatile String[] dirs = null;
	private volatile int capacity = 0;
	private volatile long n = 0;
	
	public NetDirList() {
		
	}
	
	public NetDirList(long n) {
		this.n = n;
	}
	
	public synchronized long getNumber() {
		return n;
	}
	
	public synchronized void setDirs(String[] dirs, int capacity) {
		this.dirs = dirs;
		this.capacity = capacity;
	}
	
	public synchronized String[] getDirs() {
		return dirs;
	}
	
	public synchronized int getCapacity() {
		return capacity;
	}
}
