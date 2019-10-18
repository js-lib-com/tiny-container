package js.tiny.container.net;

import java.security.Principal;

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
	 * Create a new event stream for given user principal and configure it from given configuration object. This method is
	 * invoked from {@link EventStreamServlet} when a request for an event stream is received.
	 * <p>
	 * Both arguments are optional and accept null value. If user principal is missing container creates an unique
	 * {@link EventGuest} user. If configuration object is not provided container uses default values.
	 * 
	 * @param principal optional user principal, null if not in a security context,
	 * @param config optional configuration object, null if to use container default configurations.
	 * @return opened event stream.
	 */
	EventStream createEventStream(Principal principal, EventStreamConfig config);

	/**
	 * Destroy an event stream created by this event stream manager. This method is invoked from {@link EventStreamServlet} when
	 * events connection is closed. Implementation should release all resources allocated for given event stream including event
	 * stream servlet.
	 * <p>
	 * This method should do nothing if event stream is already destroyed.
	 * 
	 * @param eventStream event stream to destroy.
	 */
	void destroyEventStream(EventStream eventStream);
}