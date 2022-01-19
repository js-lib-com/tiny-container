package js.tiny.container.net;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import js.lang.Event;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;

/**
 * Implementation for event stream manager. This class implements event stream life cycle management and events push interface
 * for user space code.
 * <p>
 * Here is overall process description and related sample code.
 * <p>
 * When server gets a request for an event stream, {@link EventStreamServlet} gets user principal from container - that could be
 * null if not authenticated context, and optional event stream configuration object from HTTP request body then delegates
 * {@link #createEventStream(Principal, EventStreamConfig)} to create event stream.
 * <p>
 * Then servlet blocks on {@link EventStream#loop()} till client disconnects. Application code uses {@link #push(Event)} that
 * delegates {@link EventStream#push(Event)} to push events that client reads in a loop. When client disconnects, servlet uses
 * {@link #destroyEventStream(EventStream)} to release resources.
 * 
 * <pre>
 * // on client, event stream manager is declared as remote managed class
 * EventStreamManager eventStreamManager = container.getInstance(EventStreamManager.class);
 * 
 * EventStreamConfig config = new EventStreamConfig();
 * config.setKeepAlivePeriod(KEEP_ALIVE_PERIOD);
 * 
 * URL pushServiceURL = new URL("http://server.com/service/");
 * HttpURLConnection connection = (HttpURLConnection) pushServiceURL.openConnection();
 * 
 * json.stringify(connection.getWriter(), config);
 * 
 * // event stream servlet gets request and uses stream manager to create event stream
 * 
 * // read events from connection input stream,	 pushed by server event stream
 * EventReader eventReader = new EventReader(connection.getInputStream());
 * for (;;) {
 * 	Event event = eventReader.read();
 * 	if (event == null) {
 * 		break;
 * 	}
 * ...
 * </pre>
 * 
 * @author Iulian Rotaru
 */
class EventStreamManagerImpl implements EventStreamManager {
	private static final Log log = LogFactory.getLog(EventStreamManagerImpl.class);

	/** Parent container back reference. */
	private final IContainer container;

	/**
	 * Storage for running event stream references. It is updated by {@link #createEventStream(String)} and
	 * {@link #destroyEventStream(EventStream)} and used by {@link #preDestroy()} to release event streams still opened at event
	 * stream manager destroy.
	 * <p>
	 * This list is used from different threads and is synchronized.
	 */
	private final Map<Principal, EventStream> eventStreams;

	/**
	 * Construct event stream manager instance and inject application factory.
	 * 
	 * @param container parent container reference.
	 */
	@Inject
	public EventStreamManagerImpl(IContainer container) {
		this.container = container;
		this.eventStreams = new HashMap<>();
	}

	/**
	 * Test constructor.
	 * 
	 * @param container container mock,
	 * @param eventStreams mock for events streams storage.
	 */
	public EventStreamManagerImpl(IContainer container, Map<Principal, EventStream> eventStreams) {
		this.container = container;
		this.eventStreams = eventStreams;
	}

	/** Closes all event streams still opened when event stream manager is destroyed. */
	@PreDestroy
	public void preDestroy() {
		synchronized (eventStreams) {
			if (eventStreams.isEmpty()) {
				return;
			}

			// EventStream#close signals stream loop that breaks
			// as a consequence, EventStreamServlet ends current request processing and call this#closeEventStream
			// this#closeEventStream removes event stream from this#eventStreams list resulting in concurrent change
			// to cope with this concurrent change uses a temporary array

			EventStream[] eventStreamsArray = new EventStream[eventStreams.size()];
			int index = 0;
			for (EventStream eventStream : eventStreams.values()) {
				eventStreamsArray[index++] = eventStream;
			}

			for (EventStream eventStream : eventStreamsArray) {
				log.debug("Force close stale event stream |%s|.", eventStream);
				eventStream.close();
			}
		}
	}

	@Override
	public EventStream createEventStream(Principal principal, EventStreamConfig config) {
		if (principal == null) {
			principal = new EventGuest();
		}
		EventStream eventStream = container.getInstance(EventStream.class);
		if (config != null) {
			eventStream.config(config);
		}
		synchronized (eventStreams) {
			eventStreams.put(principal, eventStream);
		}
		return eventStream;
	}

	@Override
	public void destroyEventStream(EventStream eventStream) {
		synchronized (eventStreams) {
			eventStreams.values().remove(eventStream);
		}
	}

	@Override
	public void push(Event event) {
		synchronized (eventStreams) {
			for (EventStream eventStream : eventStreams.values()) {
				eventStream.push(event);
			}
		}
	}

	@Override
	public void push(Principal principal, Event event) {
		synchronized (eventStreams) {
			EventStream eventStream = eventStreams.get(principal);
			if (eventStream != null) {
				eventStream.push(event);
			}
		}
	}
}
