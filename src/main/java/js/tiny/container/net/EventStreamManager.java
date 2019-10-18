package js.tiny.container.net;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

import js.lang.Event;
import js.tiny.container.annotation.Inject;
import js.tiny.container.core.AppContext;

/**
 * Event stream manager facilitates server event push to connected clients. There are two events pushing scenarios: on security
 * context and on public, not authenticated context. For public context uses {@link #push(Event)} method that sends server
 * events to all clients with opened event stream. For this, event stream manager should keep track to all created event
 * streams.
 * <p>
 * On server logic the usual usage pattern is to inject event stream manager into a managed instance. In sample below injection
 * is performed on constructor but is also possible to use {@link Inject} annotation.
 * 
 * <pre>
 * public class Service {
 * 	private final EventStreamManager eventStream;
 * 
 * 	public Service(EventStreamManager eventStream) {
 * 		this.eventStream = eventStream;
 * 	}
 * 
 * 	public void method() {
 * 		eventStream.push(new ServerMessageEvent("text"));
 * 	}
 * }
 * </pre>
 * <p>
 * For authenticated contexts uses {@link #push(Principal, Event)} to send event to a specific user or use
 * {@link #push(List, Event)} if in need to send the same event to multiple users. One can get authenticated user principal from
 * {@link AppContext}, see sample code.
 * 
 * <pre>
 * public class Service {
 * 	private final AppContext context;
 * 	private final EventStreamManager eventStream;
 * 
 * 	public Service(AppContext context, EventStreamManager eventStream) {
 * 		this.context = context;
 * 		this.eventStream = eventStream;
 * 	}
 * 
 * 	public void method() {
 * 		User user = context.getUserPrincipal();
 * 		eventStream.push(user, new ServerMessageEvent("text"));
 * 	}
 * }
 * </pre>
 * <p>
 * Even if in an authenticated context one still can use {@link #push(Event)} to broadcast the event to all connected clients,
 * including guests, if any.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface EventStreamManager {
	/**
	 * Push event to all created event streams.
	 * 
	 * @param event event to push.
	 */
	void push(Event event);

	/**
	 * Push event to user event stream.
	 * 
	 * @param principal user principal,
	 * @param event event to push.
	 */
	void push(Principal principal, Event event);

	/**
	 * Convenient method to push the same event to a group of users. Event sending order is given collection iterator order.
	 * 
	 * @param principals users group,
	 * @param event event to push.
	 */
	void push(Collection<? extends Principal> principals, Event event);
}