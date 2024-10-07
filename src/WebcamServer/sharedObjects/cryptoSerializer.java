package sharedObjects;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryo.io.*;

@SuppressWarnings("rawtypes")
public class cryptoSerializer extends Serializer {
	private final Serializer serializer;
	private volatile sharedObjects.networkKey networkKey;

	public cryptoSerializer(Serializer serializer, sharedObjects.networkKey networkKey) {
		this.serializer = serializer;
		this.networkKey = networkKey;
	}

	@SuppressWarnings("unchecked")
	public void write (Kryo kryo, Output output, Object object) {
		Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
		CipherOutputStream cipherStream = new CipherOutputStream(output, cipher);
		Output cipherOutput = new Output(cipherStream, 256) {
			public void close () throws KryoException {
				// Don't allow the CipherOutputStream to close the output.
			}
		};
		serializer.write(kryo, cipherOutput, object);
		cipherOutput.flush();
		try {
			cipherStream.close();
		} catch (Exception ex) {
			throw new KryoException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	public Object read (Kryo kryo, Input input, Class type) {
		Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
		CipherInputStream cipherInput = new CipherInputStream(input, cipher);
		return serializer.read(kryo, new Input(cipherInput, 256), type);
	}

	@SuppressWarnings("unchecked")
	public Object copy (Kryo kryo, Object original) {
		return serializer.copy(kryo, original);
	}

	private Cipher getCipher (int mode) {
		try {
			Cipher cipher = Cipher.getInstance(networkKey.transformation);
			if(networkKey.iv != null) cipher.init(mode, networkKey.key, new IvParameterSpec(networkKey.iv));
			else cipher.init(mode, networkKey.key);
			return cipher;
		} catch (Exception ex) {
			throw new KryoException(ex);
		}
	}
}
