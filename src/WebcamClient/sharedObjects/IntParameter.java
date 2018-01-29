package sharedObjects;

public class IntParameter {
	private String key = null;
	private long value = 0;

	public IntParameter() {
		
	}
	
	public IntParameter(String key, long value) {
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
