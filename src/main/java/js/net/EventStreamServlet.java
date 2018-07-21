package js.net;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import js.http.HttpHeader;
import js.log.Log;
import js.log.LogFactory;
import js.servlet.AppServlet;
import js.servlet.RequestContext;

/**
 * Servlet that handle HTTP requests for W3C Server-Sent Events. It is designed to work closely with {@link EventStream} and
 * basically creates associated event stream instance and run its {@link EventStream#loop()}.
 * <p>
 * Servlet instance is running in a separated request thread and execute events stream loop as far client remains connected,
 * that is, there is an thread running this Servlet instance for every opened events stream. Of course there is a limit on the
 * number of concurrent events streams due to limited execution threads and sockets usable by a process.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class EventStreamServlet extends AppServlet {
	/** Java serialization version. */
	private static final long serialVersionUID = 1856456540830763376L;

	/** Class logger. */
	private static final Log log = LogFactory.getLog(EventStreamServlet.class);

	/** Event stream manager. */
	private EventStreamManagerSPI eventStreamManager;

	/** Default constructor. */
	public EventStreamServlet() {
		log.trace("EventStreamServlet()");
	}

	/**
	 * Beside initialization inherited from {@link AppServlet#init(ServletConfig)} this method takes care to initialize event
	 * stream manager reference.
	 * 
	 * @param config servlet configuration object.
	 * @throws UnavailableException if servlet initialization fails.
	 */
	@Override
	public void init(ServletConfig config) throws UnavailableException {
		super.init(config);
		log.trace("init(ServletConfig)");
		eventStreamManager = container.getInstance(EventStreamManager.class);
	}

	/**
	 * Create {@link EventStream} for session ID from request URI and run {@link EventStream#loop()} as far it returns true.
	 * This method takes also care to handle connections errors due to network or client failure. At event stream loop exit,
	 * graceful or due to error, unbind event stream from manager.
	 * <p>
	 * If event stream cannot be created due to bad session ID responds with bad request, error code 400.
	 * <p>
	 * Note that this method does not return as long client requesting for event stream is connected, keeping HTTP request
	 * thread and servlet allocated, even there is no event to send for this particular client.
	 * 
	 * @param context HTTP request context.
	 * @throws IOException if HTTP response output stream fail to open.
	 */
	@Override
	protected void handleRequest(RequestContext context) throws IOException {
		log.trace("handleRequest(RequestContext)");
		final HttpServletResponse httpResponse = context.getResponse();
		final String sessionID = getEventStreamSessionID(context.getRequestPath());

		EventStream eventStream = eventStreamManager.createEventStream(sessionID);
		if (eventStream == null) {
			// a bad or heavy loaded client may send this request after session expiration or attempt to reuse old session ID
			sendBadRequest(context);
			return;
		}

		// 1. headers and content type should be initialized before response commit
		// 2. it seems that ServletResponse#getWriter() updates character set
		// excerpt from apidoc:
		// If the response's character encoding has not been specified as described in getCharacterEncoding (i.e., the method
		// just returns the default value ISO-8859-1), getWriter updates it to ISO-8859-1.

		// so need to be sure headers are updated before writer opened

		httpResponse.setContentType("text/event-stream;charset=UTF-8");
		// no need to explicitly set character encoding since is already set by content type
		// httpResponse.setCharacterEncoding("UTF-8");

		httpResponse.setHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_CACHE);
		httpResponse.addHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_STORE);
		httpResponse.setHeader(HttpHeader.PRAGMA, HttpHeader.NO_CACHE);
		httpResponse.setDateHeader(HttpHeader.EXPIRES, 0);

		httpResponse.setHeader(HttpHeader.CONNECTION, HttpHeader.KEEP_ALIVE);

		eventStream.setRemoteHost(context.getRemoteHost());
		eventStream.setWriter(httpResponse.getWriter());
		try {
			eventStream.onOpen();
			log.debug("Event stream |%s| opened.", eventStream);
			while (eventStream.loop()) {
			}
		} finally {
			eventStream.onClose();
			eventStreamManager.destroyEventStream(eventStream);
			log.debug("Event stream |%s| closed.", sessionID);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Extract event stream session ID from request path. This method extracts last path component from request path with
	 * extension removed, if present. It is a sort of <code>basename</code> from file path. All next request paths with return
	 * the same session ID, <code>1234</code>:
	 * <ul>
	 * <li>/1234.event,
	 * <li>/event/1234,
	 * <li>/1234,
	 * <li>/admin/1234.event
	 * <li>/admin/event/1234
	 * </ul>
	 * This allows for flexible event stream servlet mapping on deployment descriptor. Both mapping by path and extension can be
	 * used. Also extension can be anything. Anyway, this flexibility comes with a constraint: session ID cannot contain slash
	 * (/) or dot (.).
	 * <p>
	 * Session ID is generated by event stream manager when client subscribes, see
	 * {@link EventStreamManager#subscribe(EventStreamConfig)}, then client sends session ID back to this servlet, as a HTTP
	 * request.
	 * 
	 * @param requestPath request path, not null and with leading path separator.
	 * @return event stream session ID, never null.
	 */
	private static String getEventStreamSessionID(String requestPath) {
		int extensionSeparator = requestPath.lastIndexOf('.');
		if (extensionSeparator == -1) {
			extensionSeparator = requestPath.length();
		}

		// request URI is guaranteed to start with path separator
		// anyway, if missing below pathSeparator will be -1 + 1 = 0, pointing to entire request URI
		int pathSeparator = requestPath.lastIndexOf('/', extensionSeparator) + 1;
		return requestPath.substring(pathSeparator, extensionSeparator);
	}
}
