package sharedObjects;

public class intParameter {
	private volatile String key = null;
	private volatile long value = 0;

	public intParameter() {
		
	}
	
	public intParameter(String key, long value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	
	public long getValue() {
		return value;
	}
}
