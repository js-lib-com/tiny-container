package js.net;

/**
 * Internal service interface for event stream manager. This interface complements public interface {@link EventStreamManager}
 * with container private services. It has methods to create and destroy event streams.
 * <p>
 * For a full use case description see {@link EventStreamManagerImpl}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface EventStreamManagerSPI {

	/**
	 * Create a new event stream for requested event stream session ID. This method is invoked from {@link EventStreamServlet}
	 * when a request for an event stream is received.
	 * <p>
	 * Session ID is valid only once and for a short period of time. If session ID is not valid or is stale, implementation
	 * should reject event stream request and return null.
	 * 
	 * @param sessionID unique opening session ID.
	 * @return opened event stream, null if session ID is not accepted.
	 * @throws IllegalArgumentException if session ID is null or empty.
	 */
	EventStream createEventStream(String sessionID);

	/**
	 * Destroy an event stream created by this event stream manager. This method is invoked from {@link EventStreamServlet} when
	 * events connection is closed.
	 * <p>
	 * Event stream to destroy should one returned by {@link #createEventStream(String)}. Implementation should release all
	 * resources allocated for given event stream.
	 * <p>
	 * This method should do nothing if event stream is already destroyed or not created by this event stream manager.
	 * 
	 * @param eventStream event stream to destroy.
	 */
	void destroyEventStream(EventStream eventStream);
}