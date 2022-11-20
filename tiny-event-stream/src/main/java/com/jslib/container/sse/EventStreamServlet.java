package com.jslib.container.sse;

import java.io.IOException;
import java.security.Principal;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.http.ContentType;
import com.jslib.container.http.HttpHeader;
import com.jslib.container.servlet.AppServlet;
import com.jslib.container.servlet.RequestContext;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.jslib.api.json.Json;
import com.jslib.api.json.JsonException;
import com.jslib.util.Classes;

/**
 * Servlet that handle HTTP requests for W3C Server-Sent Events. It is designed to work closely with {@link EventStream} and
 * basically creates associated event stream instance and run its {@link EventStream#loop()}.
 * <p>
 * Servlet instance is running in a separated request thread and execute events stream loop as far client remains connected,
 * that is, there is an thread running this Servlet instance for every opened events stream. Of course there is a limit on the
 * number of concurrent events streams due to limited execution threads and sockets usable by a process.
 * 
 * @author Iulian Rotaru
 */
public class EventStreamServlet extends AppServlet {
	private static final long serialVersionUID = 1856456540830763376L;

	private static final Log log = LogFactory.getLog(EventStreamServlet.class);

	/** Event stream manager reference. It is a managed instance with application scope. */
	private EventStreamManager eventStreamManager;

	public EventStreamServlet() {
		log.trace("EventStreamServlet()");
	}

	/**
	 * Beside initialization inherited from {@link AppServlet#init(ServletConfig)} this method takes care to initialize event
	 * stream manager reference.
	 * 
	 * @param config servlet configuration object.
	 * @throws UnavailableException if servlet initialization fails.
	 * @throws ServletException if servlet initialization fails.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		log.trace("init(ServletConfig)");
		eventStreamManager = getContainer().getInstance(EventStreamManager.class);
	}

	/**
	 * Create {@link EventStream} for session ID from request URI and run {@link EventStream#loop()} as far it returns true.
	 * This method takes also care to handle connections errors due to network or client failure. At event stream loop exit,
	 * graceful or due to error, destroy the event stream from manager.
	 * <p>
	 * If event stream cannot be created due to bad session ID responds with bad request, error code 400.
	 * <p>
	 * Note that this method does not return as long client requesting for event stream is connected, keeping HTTP request
	 * thread and servlet allocated, even there is no event to send for this particular client.
	 * 
	 * @param context HTTP request context.
	 * @throws IOException for HTTP response output stream fail.
	 */
	@Override
	protected void handleRequest(RequestContext context) throws IOException {
		log.trace("handleRequest(RequestContext)");
		log.debug("Event stream request from |{remote_host}|.", context.getRemoteHost());
		final HttpServletResponse httpResponse = context.getResponse();

		final EventStreamConfig config = getEventStreamConfig(context.getRequest());
		final Principal principal = getContainer().getUserPrincipal();
		EventStream eventStream = eventStreamManager.createEventStream(principal, config);

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
			log.debug("Event stream |{event_stream}| opened.", eventStream);
			while (eventStream.loop()) {
			}
		} finally {
			log.debug("Close servlet for event stream |{event_stream}|.", eventStream);
			eventStream.onClose();
			eventStreamManager.destroyEventStream(eventStream);
		}
	}

	private static EventStreamConfig getEventStreamConfig(HttpServletRequest request) throws JsonException, ClassCastException, IOException {
		// getContentLength() returns -1 if Content-Length header is not set 
		if (request.getContentLength() <= 0) {
			return null;
		}
		if(!"POST".equalsIgnoreCase(request.getMethod())) {
			log.warn("Invalid event stream request from |{remote_host}|. It has body but is not a POST.", request.getRemoteHost());
			return null;
		}
		ContentType contentType = new ContentType(request.getContentType());
		if (!contentType.isJSON()) {
			log.warn("Invalid event stream request from |{remote_host}|. It has body but with bad content type |{http_type}|.", request.getRemoteHost(), contentType);
			return null;
		}
		Json json = Classes.loadService(Json.class);
		return json.parse(request.getReader(), EventStreamConfig.class);
	}
}
