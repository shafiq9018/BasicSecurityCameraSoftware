package sharedObjects;

public class BooleanWrapper {
	private volatile boolean b = false;

	public BooleanWrapper() {
		
	}
	
	public BooleanWrapper(boolean b) {
		this.b = b;
	}
	
	public synchronized boolean getValue() {
		return b;
	}
	
	public synchronized void setValue(boolean b) {
		this.b = b;
	}
}
