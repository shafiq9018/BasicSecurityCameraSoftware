package sharedObjects;

import javax.crypto.spec.SecretKeySpec;

public class networkKey {
	public volatile SecretKeySpec key;
	public volatile String algorithm, transformation;
	public volatile boolean hasIv = false;
	public volatile byte[] iv = null;
	
	public networkKey() {
		
	}
	
	public networkKey(String algorithm, String transformation, boolean hasIv) {
		this.algorithm = algorithm;
		this.transformation = transformation;
		this.hasIv = hasIv;
	}
	
	public networkKey(String algorithm, String transformation, SecretKeySpec key, byte[] iv) {
		this.algorithm = algorithm;
		this.transformation = transformation;
		this.key = key;
		this.iv = iv;
	}
}
