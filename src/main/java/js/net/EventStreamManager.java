package js.net;

/**
 * Management interface allows client subscription to a certain event stream.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface EventStreamManager {
	/**
	 * Subscribe to events stream and returns session ID. This method is remote accessible and public. It returns a session ID
	 * with a short life time, for about 10 seconds. Subscriber should use session ID to immediately opens the events stream.
	 * 
	 * @param config events stream configuration object.
	 * @return events stream session ID.
	 */
	String subscribe(EventStreamConfig config);
}