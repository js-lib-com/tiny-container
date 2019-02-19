package js.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import js.converter.ConverterRegistry;
import js.json.Json;
import js.lang.BugError;
import js.lang.Event;
import js.log.Log;
import js.log.LogFactory;
import js.util.Base64;
import js.util.Classes;
import js.util.Strings;

/**
 * Push W3C Server-Sent Events to a writer that is connecting an event stream client. {@link EventStreamServlet} creates an
 * instance of this class for every event stream request and block on it while {@link #loop()} returns true.
 * <p>
 * As mentioned, event stream instance is created when a HTTP request arrive for an event stream. In order to create an
 * application defined event stream one needs to extends this class. Please remember to declare subclass into application
 * descriptor, <code>managed-classes</code> section like in snippet below. Event stream declaration is in fact a managed class
 * overload since it is already declared into library descriptor with LOCAL scope; it should be local because a new instance is
 * created for every request.
 * 
 * <pre>
 *  &lt;managed-classes&gt;
 *      &lt;event-stream class="AppEventStream" interface="js.net.EventStream" /&gt;
 *      ...
 * </pre>
 * <p>
 * Once custom event stream created there are two usage patterns. First is to create new thread that observe system state,
 * create events and push them using {@link #push(Event)} method.
 * 
 * <pre>
 * public class AppEventStream extends EventStream implements Runnable {
 * 	void run() {
 * 		// wait for system state updates, create event then push it
 * 		pushEvent(event);
 * 	}
 * }
 * </pre>
 * 
 * The second option is to override {@link #loop()} method that is executed into HTTP request thread.
 * 
 * <pre>
 * public class AppEventStream extends EventStream {
 * 	protected boolean loop() {
 * 		// wait for system state updates and create event then invoke inherited event sender
 * 		sendEvent(event);
 * 		// returns true to allow for event stream loop continuation
 * 		return true;
 * 	}
 * }
 * </pre>
 * 
 * First usage pattern is using internal events queue and rely on implemented loop method to send keep alive events but is using
 * a separated thread. The second uses HTTP request thread but user code should take care of keep alive sending.
 * 
 * <h3>Keep Alive</h3> This events stream is basically a long connection using chunked transfer. Client sends HTTP request then
 * block on HTTP response waiting for events. If server side or network goes down client logic has no means to realize this; it
 * simply continue waiting. For this reason event stream takes care to periodically send <code>KeepAliveEvent</code>.
 * <p>
 * The second reason for keep alive events is routers idle connection timeout. Although TCP/IP does not require keep alive or
 * dead connection detect packets there is usual practice for routers to disconnect idle connections in order to free resources.
 * <p>
 * This event stream takes care to send a keep alive event immediately a client is connected, see {@link #onOpen()}. This way
 * client side has the chance to quickly know that event stream is properly initialized.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class EventStream implements Closeable {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(EventStream.class);

	/** Events queue timeout used to guard queue offer operation, in milliseconds. */
	private static final int EVENTS_QUEUE_PUSH_TIMEOUT = 4000;

	/**
	 * Default keep alive timeout loaded from application descriptor. See {@link #keepAlivePeriod} for a discussion about this
	 * timeout value and configuration.
	 */
	private static int KEEP_ALIVE_TIMEOUT = 40000;

	/**
	 * Every event stream has its own unique ID used merely for debugging. Stream ID is unique only on current application run;
	 * it is not preserved after application restart.
	 */
	private static int STREAM_ID;

	/** JSON serializer. */
	private final JsonSerializer json;

	/** Flag indicating this event stream instance is active. */
	private final AtomicBoolean active = new AtomicBoolean(true);

	/**
	 * Blocking events queue. This queue is used from two threads: this events stream thread is waiting for the queue to acquire
	 * an event, see {@link #loop()} and second, the event service thread uses {@link #push(Event)} to actually enqueue the
	 * event.
	 */
	private BlockingQueue<Event> eventsQueue;

	/**
	 * Underlying characters stream used to actually carry serialized event to the client. Please remember that Java print
	 * writer does not throw IO exceptions and provide {@link PrintWriter#checkError()} for that. Event stream uses error
	 * checking mechanism to break {@link #loop()} on errors. This helps closing event stream and parent servlet instance when
	 * remote client closes its connection.
	 */
	private PrintWriter writer;

	/** Optional symmetric key used to encrypt the event message using AES chiper. Default to null. */
	private SecretKey secretKey;

	/**
	 * Send keep alive if there are no events sent for this amount of time, in milliseconds. Keep alive events have double role:
	 * <ol>
	 * <li>Allows for both client and this server side event stream to know that peer is up,
	 * <li>Avoid routers with idle timeout to drop connections when no events are sent.
	 * </ol>
	 * <p>
	 * For second case this timeout value is critical. If is larger than router idle connection timeout, connection will be lost
	 * and client will reconnect periodically; in this case events stream behaves like a polling mechanism with resources waste
	 * and bad responsiveness.
	 * <p>
	 * Routers idle connection timeout is manufacturer specific and / or network administrator customizable and can vary
	 * greatly, including complete disabling. In fact it is a not standard behavior. TCP/IP does not require such timeout; it is
	 * merely a technological limitation used to save routers internal resources. Anyway, from practice is observed that the
	 * lower idle connection timeout value found is one minute. This event stream class uses 40 seconds in order to cope with
	 * delays generated by server heavy loading and network latency. It is a heuristic / brute force approach. A better solution
	 * may be an adaptive timeout that client may learn and communicate to server when opens events stream.
	 * <p>
	 * Now, some considerations about keep alive packages bandwidth consumption. Believe me or not, keep alive event data length
	 * is exactly 40 bytes. With current 40 seconds period results in exactly one byte per second, a perfectly acceptable
	 * overload. And even if include TCP/IP headers and acknowledge packet bandwidth consumption will not exceed 3 B/s.
	 * <p>
	 * Finally, events stream implementation is a managed class and this keep alive timeout value is customizable per server,
	 * like in sample code below.
	 * 
	 * <pre>
	 *    &lt;events-stream&gt;
	 *        &lt;static-field name="KEEP_ALIVE_TIMEOUT" value="300" /&gt;
	 *    &lt;/events-stream&gt;
	 * </pre>
	 */
	private int keepAlivePeriod = KEEP_ALIVE_TIMEOUT;

	/** Parameters map for request used to enact this event stream service. */
	private Map<String, String> parameters;

	/** This events stream string representation used mostly for debugging. */
	private String string;

	// --------------------------------------------------------------------------------------------
	// CONSTRUCTION AND CONFIGURATION

	/** Default constructor. This class should not be instantiated using new operator. */
	protected EventStream() {
		this.json = new JsonSerializer(Classes.loadService(Json.class));
		this.eventsQueue = new LinkedBlockingQueue<>();
	}

	/**
	 * Configure events stream instance from configuration object.
	 * 
	 * @param config event stream configuration object.
	 */
	protected void config(EventStreamConfig config) {
		if (config.hasSecretKey()) {
			secretKey = config.getSecretKey();
		}
		if (config.hasKeepAlivePeriod()) {
			keepAlivePeriod = config.getKeepAlivePeriod();
		}
		parameters = config.getParameters();
	}

	// --------------------------------------------------------------------------------------------
	// PUBLIC METHODS

	/**
	 * Push event to this events stream client. This method just stores the event on {@link #eventsQueue events queue} being
	 * executed into invoker thread. This events stream thread is blocked on the events queue; after this method execution it
	 * will unblock and process the event, see {@link #loop()} method.
	 * <p>
	 * Queue offer operation is guarded by {@link #EVENTS_QUEUE_PUSH_TIMEOUT}. This timeout can occur only in a very improbable
	 * condition of events flood combined with system resources starvation. For this reason there is no attempt to recover;
	 * event is simple lost with warning on application logger.
	 * 
	 * @param event event instance to push on event stream.
	 * @throws BugError if trying to use this method after stream close.
	 */
	public void push(Event event) {
		if (!active.get()) {
			throw new BugError("Event stream |%s| is closed.", this);
		}
		// BlockingQueue is thread safe so we do not need to synchronize this method
		try {
			if (!eventsQueue.offer(event, EVENTS_QUEUE_PUSH_TIMEOUT, TimeUnit.MILLISECONDS)) {
				log.warn("Timeout trying to push event on events queue. Event |%s| not processed.", event);
			}
		} catch (InterruptedException unused) {
			log.warn("Thread interruption on event stream |%s| while trying to push event to queue. Event |%s| not processed.", this, event);
			Thread.currentThread().interrupt();
		}
	}

	/** Close this event stream loop and waits for associated {@link EventStreamServlet} exit. */
	@Override
	public void close() {
		if (!active.get()) {
			return;
		}
		log.debug("Closing event stream |%s| ...", this);

		push(new ShutdownEvent());
		active.set(false);
		log.debug("Event stream |%s| was closed.", this);
	}

	/**
	 * Get event stream instance string representation.
	 * 
	 * @return this event stream instance string representation.
	 */
	@Override
	public String toString() {
		return string;
	}

	// --------------------------------------------------------------------------------------------
	// PROTECTED METHODS

	/**
	 * Set the host address for client connected to this event stream.
	 * 
	 * @param remoteHost client remote host address.
	 */
	protected void setRemoteHost(String remoteHost) {
		if (string == null) {
			string = Strings.concat('#', STREAM_ID++, ':', remoteHost);
		}
	}

	/**
	 * Set the output characters stream used to convey the events.
	 * 
	 * @param writer output characters stream.
	 */
	protected void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	/**
	 * Event stream main loop repeated for every event. Waits for an event and push it to the client via {@link #writer}, writer
	 * initialized by {@link EventStreamServlet} on this event stream creation. Note that this logic is executed into HTTP
	 * request thread; it blocks the thread till an event become available. This method always returns true allowing event
	 * stream to continue. It returns false only when got {@link ShutdownEvent}, pushed on queue by {@link #close()} method or
	 * there is an error on print writer. Checking for print writer errors is especially useful to detect that remote client
	 * closes its socket and to gracefully close this stream and release parent servlet instance.
	 * <p>
	 * Also this method takes care to periodically send keep alive events, see {@link #keepAlivePeriod}. Keep alive is used to
	 * ensure server side and client logic that peer is still running and to avoid connection drop due to routers idle
	 * connection timeout.
	 * 
	 * @return true if event stream should continue running.
	 */
	protected boolean loop() {
		Event event = null;
		try {
			event = eventsQueue.poll(keepAlivePeriod, TimeUnit.MILLISECONDS);
		} catch (InterruptedException unused) {
			if (!active.get()) {
				log.debug("Events stream |%s| thread is interrupted. Break events stream loop.", this);
				return false;
			}
			// in a perfect world, now would be the right moment to stop the events stream, returning false...
			// but i'm not sure interruption occurs only when current thread is interrupted
			// for now i play safe, allowing events stream to continue and use shutdown event to break it
			log.warn("Events stream |%s| thread is interrupted. Continue events stream loop.", this);
			return true;
		}

		if (event == null) {
			// we are here due to keep-alive period expiration
			// returns true to signal event stream should continue
			sendKeepAlive();
			log.debug("Keep-alive was sent to event stream |%s|.", this);
			return !writer.checkError();
		}

		// close method puts this event into queue; returns false to break this events stream loop
		if (event instanceof ShutdownEvent) {
			log.debug("Got shutdown event. Break event stream loop.");
			return false;
		}

		sendEvent(event);
		onSent(event);
		log.debug("Event |%s| was sent to event stream |%s|.", event, this);
		return !writer.checkError();
	}

	/**
	 * Get named parameter throwing exception if not found.
	 * 
	 * @param name requested parameter name,
	 * @param type desired parameter type.
	 * @param <T> parameter type.
	 * @return named parameter instance.
	 * @throws BugError if named parameter does not exist.
	 */
	protected <T> T getParameter(String name, Class<T> type) {
		if (parameters == null) {
			throw new BugError("Event stream |%s| parameters not configured.", this);
		}
		String value = parameters.get(name);
		if (value == null) {
			throw new BugError("Missing event stream parameter |%s| of expected type |%s|.", name, type);
		}
		return ConverterRegistry.getConverter().asObject(value, type);
	}

	/**
	 * Hook method invoked at event stream instance creation. This method is intended to be overridden by subclasses. Anyway, it
	 * takes care to send a keep alive event so that client can quickly know event stream is properly working. If subclass needs
	 * this feature it should explicitly call super from its overriding method.
	 */
	protected void onOpen() {
		sendKeepAlive();
	}

	/**
	 * Hook method invoked on this event stream closing. This method is called by {@link EventStreamServlet} just before HTTP
	 * request processing end. After this method execution event stream instance becomes invalid and attempting to use any of
	 * its methods is considered a bug.
	 * <p>
	 * If subclass override this method it is its responsibility to call super.
	 */
	protected void onClose() {
	}

	/**
	 * Hook method invoked just after event sent. This method is called by {@link #loop()} immediately after event was sent to
	 * output stream and stream flushed.
	 * 
	 * @param event event that was sent.
	 */
	protected void onSent(Event event) {
	}

	/**
	 * Send event instance to this event stream consumer. Compile a W3C Server-Sent event from <code>event</code> instance
	 * argument and write it to this {@link #writer}. This event stream implementation does not use all W3C server-sent event
	 * fields: only <code>event</code> and <code>data</code>, as folows:
	 * <ul>
	 * <li>event field stores event argument canonical class name,
	 * <li>data field value is event argument instance serialized JSON.
	 * </ul>
	 * Just for those curious here is an sample of a serialized hypothetical event, as it is on wire:
	 * 
	 * <pre>
	 * event:bb.hub.event.NotificationCRLF
	 * data:{"id":213,"title":"Baby Server Admin","text":"Baby server was stopped.","timestamp":"2014-04-10T19:07:05Z","priority":"DEFAULT"}CRLF
	 * CRLF
	 * </pre>
	 * 
	 * @param event event instance.
	 */
	protected void sendEvent(Event event) {
		write("data:");

		if (secretKey == null) {
			json.serialize(writer, event);
		} else {
			try {
				Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm());
				cipher.init(Cipher.ENCRYPT_MODE, secretKey);

				// this overload of doFinal from Cipher class is able to perform single step encrypting
				// excerpt from API:
				// Encrypts or decrypts data in a single-part operation, or finishes a multiple-part operation.
				byte[] encryptedMessage = cipher.doFinal(json.serialize(event));

				// encode encrypted event message with Base64 since content type should be text
				write(Base64.encode(encryptedMessage));
			} catch (InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
				// we step here only for bugs on encryption provider:
				// secret key size is incorrect or key is not properly initialized
				// single step encryption using directly Cipher#doFinal is supposed to process all input at once
				// missing JVM default padding mechanism
				// bad padding occurring on encryption
				throw new BugError(e);
			} catch (NoSuchAlgorithmException e) {
				throw new BugError("Missing support for |%s| cryptographic algorithm.", secretKey.getAlgorithm());
			}
		}

		// end data field
		crlf();

		// single end of line is the mark for event end
		crlf();
		flush();
	}

	/**
	 * Send keep alive to this event stream counterpart. Create a W3C Server-Sent event with <code>event</code> field containing
	 * canonical class name for <code>KeepAliveEvent</code>. On the wire things looks like bellow, so minimal of overhead. Note
	 * that specification mandates <code>data</code> field, for which reason envoy it with an empty JSON object.
	 * 
	 * <pre>
	 * event:js.net.KeepAliveEventCRLF
	 * data:{}CRLF
	 * CRLF
	 * </pre>
	 */
	protected void sendKeepAlive() {
		write("data:");
		crlf();

		// single end of line is the mark for event end
		crlf();
		flush();
	}

	// --------------------------------------------------------------------------------------------
	// PRIVATE METHODS

	/**
	 * Write given string to this event stream.
	 * 
	 * @param string string to send.
	 */
	private void write(String string) {
		writer.write(string);
	}

	/**
	 * Write end of line to this event stream. It is used to mark end of the field; remember that an empty event field, that is,
	 * only end of line is used to mark the event end. Note that W3C accepts single CR or LF as end of line; this implementation
	 * always uses CRLF.
	 */
	private void crlf() {
		write("\r\n");
	}

	/** Flush this event stream. This method is called after every event sent, including keep alive. */
	private void flush() {
		writer.flush();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY CLASSES

	/**
	 * JSON serializer for event objects. Thin wrapper for {@link Json} service used with print writer that does not generate IO
	 * exceptions.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static final class JsonSerializer {
		/** JSON service implementation. */
		private final Json json;

		/**
		 * Create JSON serializer instance.
		 * 
		 * @param json JSON service implementation.
		 */
		public JsonSerializer(Json json) {
			this.json = json;
		}

		/**
		 * Serialize event as JSON stream to given writer. Since writer is a {@link PrintWriter} it does not throw
		 * {@link IOException}. As a consequence this method does not throw exception too.
		 * 
		 * @param writer print writer,
		 * @param event event object.
		 */
		public void serialize(PrintWriter writer, Event event) {
			try {
				json.stringifyObject(writer, event);
			} catch (IOException e) {
				// print writer never throws IO exceptions
				throw new BugError(e);
			}
		}

		/**
		 * Serialize event object to JSON string representation and return UTF-8 bytes.
		 * 
		 * @param event event object.
		 * @return bytes of event object converted to JSON string.
		 */
		public byte[] serialize(Event event) {
			StringWriter jsonEvent = new StringWriter();
			// accordingly API there is no need to close string writer
			try {
				json.stringifyObject(jsonEvent, event);
				return jsonEvent.toString().getBytes("UTF-8");
			} catch (IOException e) {
				// hard to believe erroneous conditions:
				// - IO exceptions on string writer
				// - JVM with not support for UTF-8
				throw new BugError(e);
			}
		}
	}

	/**
	 * Internal event used to shutdown events stream. It is pushed on events queue by {@link EventStream#close()}; as a result
	 * events stream {@link EventStream#loop() loop} is stopped and {@link EventStreamServlet} is released.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static final class ShutdownEvent implements Event {
	}
}
