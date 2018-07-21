package js.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import js.annotation.Public;
import js.annotation.Remote;
import js.core.AppFactory;
import js.lang.Event;
import js.lang.ManagedPreDestroy;
import js.log.Log;
import js.log.LogFactory;
import js.util.Params;
import js.util.Strings;

/**
 * Implementation for event stream manager. This class implements event stream opening handshake and stream destroying, server
 * side logic. Basically client subscribes using {@link #subscribe(EventStreamConfig)} and got an unique session ID then send it
 * as parameter on URL used to connect the event stream service. Note that session ID is usable once and only for a short period
 * of time.
 * <p>
 * When server gets a request for an event stream, {@link EventStreamServlet} extracts session ID from request URL and uses
 * {@link #createEventStream(String)} to create event stream. Then servlet blocks on {@link EventStream#loop()} till client
 * disconnects. Server uses {@link EventStream#push(Event)} to push events that client reads in a loop. When client disconnects,
 * servlet uses {@link #destroyEventStream(EventStream)} to release resources.
 * 
 * <pre>
 * // on client, event stream manager is declared as remote managed class
 * EventStreamManager eventStreamManager = factory.getInstance(EventStreamManager.class);
 * 
 * EventStreamConfig config = new EventStreamConfig();
 * config.setKeepAlivePeriod(KEEP_ALIVE_PERIOD);
 * String sessionID = eventStreamManager.subscribe(config);
 * 
 * // prepare push service request URL based on session ID, e.g. http://server.com/service/1234.event
 * URL pushServiceURL = getEventStreamURL(sessionID);
 * HttpURLConnection connection = (HttpURLConnection) pushServiceURL.openConnection();
 * 
 * // event stream servlet gets request, extracts session ID and uses stream manager to create event stream
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
 * @version final
 */
final class EventStreamManagerImpl implements EventStreamManager, ManagedPreDestroy, EventStreamManagerSPI {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(EventStreamManagerImpl.class);

	/**
	 * Event stream subscribe transaction TTL. It is the session ID validity period between
	 * {@link #subscribe(EventStreamConfig)} and {@link #createEventStream(String)}. When this period is exceeded session ID
	 * become stale and URL request for event stream is rejected, in which case {@link EventStreamServlet} responds with bad
	 * request, 400.
	 */
	private static int SUBSCRIBE_TTL = 10000;

	/**
	 * Storage for running event stream references. It is updated by {@link #createEventStream(String)} and
	 * {@link #destroyEventStream(EventStream)} and used by {@link #preDestroy()} to release event streams still opened at event
	 * stream manager destroy.
	 * <p>
	 * This list is used from different threads and is synchronized.
	 */
	private final List<EventStream> eventStreams;

	/** Application factory. */
	private final AppFactory factory;

	/**
	 * Registered sessions for event stream opening handshake. This map content is volatile. It keeps session ID for a short
	 * period of time between {@link #subscribe(EventStreamConfig)} and {@link #createEventStream(String)}.
	 */
	private final Map<SessionID, EventStreamConfig> sessions;

	/**
	 * Construct event stream manager instance and inject application factory.
	 * 
	 * @param factory application factory.
	 */
	public EventStreamManagerImpl(AppFactory factory) {
		this.eventStreams = Collections.synchronizedList(new ArrayList<EventStream>());
		this.factory = factory;
		this.sessions = new HashMap<>();
	}

	/** Closes all event streams still opened when event stream manager is destroyed. */
	@Override
	public void preDestroy() {
		if (eventStreams.isEmpty()) {
			return;
		}

		// EventStream#close signals stream loop that breaks
		// as a consequence, EventStreamServlet ends current request processing and call this#closeEventStream
		// this#closeEventStream removes event stream from this#eventStreams list resulting in concurrent change
		// to cope with this concurrent change uses a temporary array

		// toArray API is a little confusing for me regarding returned array
		// to be on safe side let toArray to determine array size
		// also I presume returned array is not altered by list updates

		for (EventStream eventStream : eventStreams.toArray(new EventStream[0])) {
			log.debug("Force close stale event stream |%s|.", eventStream);
			eventStream.close();
		}
	}

	/**
	 * Subscribes to events stream and returns session ID. This method is remote accessible and public. It returns a session ID
	 * with a short life time, for about 10 seconds.
	 * <p>
	 * This method creates a new {@link SessionID} and stores given configuration object to {@link #sessions} map, with created
	 * session ID as key. Session storage is ephemere. It lasts only for {@link #SUBSCRIBE_TTL} period of time; after that
	 * session ID becomes stale.
	 * <p>
	 * This method should be followed by {@link #createEventStream(String)}, with returned session ID as argument.
	 * 
	 * @param config events stream configuration object.
	 * @return events stream session ID.
	 */
	@Remote
	@Public
	public String subscribe(EventStreamConfig config) {
		SessionID sessionID = new SessionID();
		log.debug("Store event stream parameters for session |%s|.", sessionID);
		sessions.put(sessionID, config);
		return sessionID.getValue();
	}

	@Override
	public EventStream createEventStream(String sessionID) {
		Params.notNullOrEmpty(sessionID, "Session ID");

		long staleTimestamp = System.currentTimeMillis() - SUBSCRIBE_TTL;
		EventStreamConfig config = null;

		synchronized (eventStreams) {
			Iterator<Map.Entry<SessionID, EventStreamConfig>> it = sessions.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<SessionID, EventStreamConfig> entry = it.next();
				if (entry.getKey().getTimestamp() < staleTimestamp) {
					log.warn("Stale event stream session |%s|. Remove it.", entry.getKey());
					it.remove();
					continue;
				}
				if (sessionID.equals(entry.getKey().getValue())) {
					config = entry.getValue();
					it.remove();
				}
			}
		}

		if (config == null) {
			log.warn("Ignore missing session ID |%s|. Most probably was remove because of life time expiration. It is also possible to be a forged event stream request. Check server logs for removed stale event stream sessions.", sessionID);
			return null;
		}

		// this will allow for multiple, named, event streams per application
		EventStream eventStream = factory.getInstance(EventStream.class);
		eventStream.config(config);
		eventStreams.add(eventStream);
		return eventStream;
	}

	@Override
	public void destroyEventStream(EventStream eventStream) {
		eventStreams.remove(eventStream);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY CLASSES

	/**
	 * Unique session ID used on event stream opening handshake. See {@link EventStreamManagerImpl} for event stream opening
	 * handshake.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static class SessionID {
		/** Seed for session ID sequential generation. */
		private final static AtomicInteger ID_SEED = new AtomicInteger();
		/** Session ID generated in sequence based on {@link #ID_SEED}. */
		private String id;
		/** Value is a string guaranteed to be unique, see {@link Strings#UUID()}. */
		private String value;
		/** Timestamp for session ID creation. */
		private long timestamp;

		/** Create new event stream session ID. */
		SessionID() {
			this.id = Integer.toString(ID_SEED.incrementAndGet());
			this.value = Strings.UUID();
			this.timestamp = System.currentTimeMillis();
		}

		/**
		 * UUID value of event stream session ID.
		 * 
		 * @return session ID value.
		 * @see #value
		 */
		String getValue() {
			return value;
		}

		/**
		 * Session ID timestamp.
		 * 
		 * @return session ID timestamp.
		 * @see #timestamp
		 */
		long getTimestamp() {
			return timestamp;
		}

		/** Instance string representation. */
		@Override
		public String toString() {
			return id;
		}
	}
}
