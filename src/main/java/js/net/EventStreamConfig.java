package js.net;

/**
 * Event stream configuration object used to send client configuration to server. It provides clients means to configure event
 * stream; current implementation has only keep alive period.
 * <p>
 * Instance of this class is deserialized by {@link EventStreamServlet} from HTTP request body and delivered to
 * {@link EventStream#config(EventStreamConfig)}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class EventStreamConfig {
	/** Event stream keep alive period, in milliseconds or zero if to use server default value. */
	private int keepAlivePeriod;

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
}
