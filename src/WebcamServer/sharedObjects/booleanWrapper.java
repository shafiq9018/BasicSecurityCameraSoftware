package sharedObjects;

public class booleanWrapper {
	private volatile boolean b = false;

	public booleanWrapper() {
		
	}
	
	public booleanWrapper(boolean b) {
		this.b = b;
	}
	
	public synchronized boolean getValue() {
		return b;
	}
	
	public synchronized void setValue(boolean b) {
		this.b = b;
	}
}
