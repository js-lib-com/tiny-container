package js.net;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Event stream configuration object used on client subscription. Provides to client means to configure event stream; for
 * example if use encryption for event data content.
 * <p>
 * On {@link EventStreamManager#subscribe(EventStreamConfig)} client provides this configuration object. Manager uses it to
 * {@link EventStream#config(EventStreamConfig)} when create event stream.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class EventStreamConfig {
	/** Optional secret key bytes, null if events stream is not secure. */
	private byte[] key;

	/** Optional name of the secret key algorithm, null if events stream is not secure. */
	private String algorithm;

	/** Event stream keep alive period, in milliseconds or zero if to use server default value. */
	private int keepAlivePeriod;

	/**
	 * Custom parameters. This parameters are stored by client and used by event stream custom implementation. Allows for event
	 * stream implementation user defined parameters. Event stream implementation should provide public description for
	 * supported parameters.
	 */
	private final Map<String, String> parameters = new HashMap<>();

	/**
	 * Set secret key used for event data symmetric encryption.
	 * 
	 * @param secretKey encryption secret key.
	 */
	public void setSecretKey(SecretKey secretKey) {
		this.key = secretKey.getEncoded();
		this.algorithm = secretKey.getAlgorithm();
	}

	/**
	 * Test if configuration object has secret key initialized.
	 * 
	 * @return true if this configuration object has secret key.
	 */
	public boolean hasSecretKey() {
		return key != null && algorithm != null;
	}

	/**
	 * Get configured secret key.
	 * 
	 * @return secret key used for event data symmetric encryption.
	 */
	public SecretKey getSecretKey() {
		return new SecretKeySpec(key, algorithm);
	}

	/**
	 * Set keep alive period.
	 * 
	 * @param keepAlivePeriod keep alive period.
	 * @see #keepAlivePeriod
	 */
	public void setKeepAlivePeriod(int keepAlivePeriod) {
		this.keepAlivePeriod = keepAlivePeriod;
	}

	/**
	 * Test if this configuration object has keep alive period. Returns true if {@link #keepAlivePeriod} is not zero.
	 * 
	 * @return true if this configuration object has keep alive period.
	 * @see #keepAlivePeriod
	 */
	public boolean hasKeepAlivePeriod() {
		return keepAlivePeriod != 0;
	}

	/**
	 * Get keep alive period or 0 if not initialized. If this value is not set event stream is configured with server default
	 * value.
	 * 
	 * @return keep alive period, possible 0.
	 * @see #keepAlivePeriod
	 */
	public int getKeepAlivePeriod() {
		return keepAlivePeriod;
	}

	/**
	 * Set custom parameter value.
	 * 
	 * @param name custom parameter name,
	 * @param value custom parameter value.
	 * @see #parameters
	 */
	public void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	/**
	 * Get custom parameters.
	 * 
	 * @return custom parameters.
	 * @see #parameters
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}
}
