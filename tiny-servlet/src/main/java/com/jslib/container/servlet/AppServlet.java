package com.jslib.container.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.concurrent.atomic.AtomicInteger;

import com.jslib.api.json.Json;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.http.ContentType;
import com.jslib.container.http.HttpHeader;
import com.jslib.container.spi.Factory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.ITinyContainer;
import com.jslib.lang.InvocationException;
import com.jslib.rmi.BusinessException;
import com.jslib.rmi.RemoteExceptionContext;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Base for all application's servlets. Implements common request processing and provides utility functions. Concrete servlet
 * implementation should focus only on request handling, see {@link #handleRequest(RequestContext)}.
 * <p>
 * This base servlet does not deal directly with authorization exceptions but provides utility function for sending unauthorized
 * access response. For this reason abstract request handler does not accept authorization exception in its signature. Rationale
 * for this design is that security handling depends on specific protocol employed by concrete servlet. For example, rejecting
 * an access for a resource is handled by redirecting to login page whereas for a service simple send back unauthorized access
 * response. Also a protocol may be designed to send back HTTP OK and serialize authorization exception in its own format.
 * <p>
 * Usually XHR requests comes from tight coupled clients that are part of a client-server application. It is expected that a
 * well behaving client to not send XHR requests for private resources outside an authenticated scope. Anyway, if XHR request
 * breaks security this framework logic enact {@link #sendUnauthorized(RequestContext)}.
 * <p>
 * XHR specification mandates for client agent to handle transparently unauthorized access. This results into browser login
 * form, that is not always the desired solution. This framework choose to send 200 OK with {@link HttpHeader#X_HEADER_LOCATION}
 * custom header. Client script can redirect now to sent location. Anyway, for this solution to work application should have
 * login support, see {@link IContainer#getLoginPage()}. If application login page is not configured unauthorized XHR is still
 * handled by client agent form.
 * <p>
 * Also this base class takes care to initialize logger context, {@link #logContext} with context path, remote address and
 * request ID for current processing request. This diagnostic context data is bound to current thread and accessible to all log
 * messages generated by methods involved in request processing, no matter nesting level.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public abstract class AppServlet extends HttpServlet {
	/** Java serialization version. */
	private static final long serialVersionUID = -1261992764546153452L;

	/** Class logger. */
	private static final Log log = LogFactory.getLog(AppServlet.class);

	/** Sequence generator for request ID. Every new HTTP request got an ID guaranteed to be unique for practical purposes. */
	private static final AtomicInteger requestID = new AtomicInteger();

	/**
	 * Request handler should be implemented by concrete servlet. It basically should extract data from request, process it and
	 * send back results on response. Also handler implementation should deal with exception like resource not found and
	 * unauthorized access. Anyway, exception that are not directly related to request processing are bubbled up, e.g.
	 * exceptions on IO and servlet container services.
	 * 
	 * @param context execution context for current request.
	 * @throws IOException if reading from request or writing to response fails.
	 * @throws ServletException for fails on executing servlet container services.
	 */
	protected abstract void handleRequest(RequestContext context) throws IOException, ServletException;

	/** Application, context and servlet names, merely for logging. */
	protected String appName;
	protected String contextName;
	protected String servletName;

	/**
	 * Container of the application on behalf of which this servlet instance is acquired. From servlet instance perspective
	 * container is a singleton and its reference can be safely stored.
	 */
	private transient volatile ITinyContainer container;

	public ITinyContainer getContainer() {
		if (container == null) {
			throw new IllegalStateException("Attempt to use Tiny container instance before servlet initialization.");
		}
		return container;
	}

	private String previewContextPath;

	/**
	 * Servlet life cycle callback executed at this servlet instance initialization. Mainly takes care to initialize parent
	 * container reference. If there is no servlet context attribute with the name {@link TinyContainer#ATTR_INSTANCE} this
	 * initialization fails with servlet permanently unavailable.
	 * <p>
	 * Parent container instance has application life span and its reference is valid for entire life span of this servlet
	 * instance.
	 * 
	 * @param config servlet configuration object.
	 * @throws ServletException if servlet configuration fails.
	 * @throws UnavailableException if container reference is not on servlet context attributes.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		final ServletContext context = config.getServletContext();
		appName = context.getServletContextName();
		contextName = context.getContextPath().isEmpty() ? CT.ROOT_CONTEXT : context.getContextPath().substring(1);
		servletName = config.getServletName();

		updateLogContext();
		log.debug("Initializing servlet {app_name}#{servlet_name}.", appName, servletName);

		previewContextPath = context.getInitParameter(CT.PARAMETER_PREVIEW_CONTEXT);
		container = (ITinyContainer) context.getAttribute(TinyContainer.ATTR_INSTANCE);
		if (container == null) {
			log.fatal("Tiny container instance not properly created, probably misconfigured. Servlet {servlet} permanently unvailable.", config.getServletName());
			throw new UnavailableException("Tiny container instance not properly created, probably misconfigured.");
		}
	}

	/**
	 * Servlet life cycle callback executed on servlet instance destroying. Current implementation just log the event for
	 * debugging.
	 */
	@Override
	public void destroy() {
		updateLogContext();
		log.debug("Destroying servlet {app_name}#{servlet_name}.", appName, servletName);
		super.destroy();
	}

	/**
	 * Prepare execution context and delegates the actual HTTP request processing to abstract handler. Initialize request
	 * context bound to current thread and delegates {@link #handleRequest(RequestContext)}. After request handler execution
	 * takes care to cleanup request context.
	 * <p>
	 * This method also initialize logger context, see {@link #logContext} with remote address of current request so that
	 * logging utility can include contextual diagnostic data into log messages. Just before exiting, this service request
	 * cleanups the logger context.
	 * 
	 * @throws IOException if reading from request or writing to response fail.
	 * @throws ServletException for fails on executing servlet container services.
	 */
	@Override
	protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {
		updateLogContext();

		LogFactory.getLogContext().put(CT.LOG_REMOTE_HOST, httpRequest.getRemoteHost());
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			LogFactory.getLogContext().put(CT.LOG_SESSION_ID, session.getId());
		}
		String traceId = httpRequest.getHeader(HttpHeader.TRACE_ID);
		if (traceId == null) {
			traceId = Integer.toString(requestID.getAndIncrement(), Character.MAX_RADIX);
		}
		LogFactory.getLogContext().put(CT.LOG_TRACE_ID, traceId);

		Factory.bind(container);

		if (isEmptyUriRequest(httpRequest)) {
			log.debug("Empty URI request for {http_url}. Please check for <img> with empty 'src' or <link>, <script> with empty 'href' in HTML source or script resulting in such condition.", httpRequest.getRequestURI());
			return;
		}
		final long start = System.nanoTime();
		log.trace("Processing request {http_method}:{http_url}.", httpRequest.getMethod(), httpRequest.getRequestURI());

		// request context has THREAD scope and this request thread may be reused by servlet container
		RequestContext requestContext = container.getInstance(RequestContext.class);
		// takes care to properly initialize (attach) request context on every HTTP request
		requestContext.attach(httpRequest, httpResponse);

		// if this request was forwarded from preview servlet ensure container is authenticated
		// current context should declare context parameter js.tiny.container.preview.context
		String forwardContextPath = (String) httpRequest.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH);
		if (previewContextPath != null && forwardContextPath != null && forwardContextPath.equals(previewContextPath)) {
			container.authenticate(new PreviewUser());
		}

		try {
			handleRequest(requestContext);
		} catch (IOException | ServletException | Error | RuntimeException t) {
			// last line of defense; dump request context and throwable then dispatch exception to servlet container
			// servlet container will generate response page using internal templates or <error-page>, if configured
			dumpError(requestContext, t);
			throw t;
		} finally {
			log.info("{http_method} {http_url} processed in {processing_time} msec.", httpRequest.getMethod(), requestContext.getRequestURL(), (System.nanoTime() - start) / 1000000.0);
			// cleanup remote address from logger context and detach request context instance from this request
			LogFactory.getLogContext().clear();
		}
	}

	private void updateLogContext() {
		LogFactory.getLogContext().put(CT.LOG_APP_NAME, appName);
		LogFactory.getLogContext().put(CT.LOG_CONTEXT_NAME, contextName);
		LogFactory.getLogContext().put(CT.LOG_SERVLET_NAME, servletName);
	}

	/**
	 * Detect self-referenced request URI. It seems there are browsers considering empty string as valid URL pointing to current
	 * loaded page. If we have an <code>img</code> element with empty <code>src</code> attribute browser will try to load that
	 * image from current URL, that is, the content of current page. This means is possible to invoke a controller many times
	 * for a single page request.
	 * <p>
	 * To avoid such condition check if this request comes from the same page, i.e. referrer is current request. Note that this
	 * is also true for <code>link</code> and <code>script</code> with empty <code>href</code>. Finally worthy to mention is
	 * that this check is not performed on request accepting <code>text/html</code>.
	 * 
	 * @param httpRequest HTTP request.
	 * @return true only this request is a <code>GET</code> and referrer is request itself.
	 */
	private static boolean isEmptyUriRequest(HttpServletRequest httpRequest) {
		if (!"GET".equals(httpRequest.getMethod())) {
			return false;
		}
		String acceptValue = httpRequest.getHeader(HttpHeader.ACCEPT);
		if (acceptValue != null && acceptValue.contains(ContentType.TEXT_HTML.getMIME())) {
			return false;
		}

		String referer = httpRequest.getHeader(HttpHeader.REFERER);
		if (referer == null) {
			return false;
		}

		StringBuilder uri = new StringBuilder(httpRequest.getRequestURI());
		String query = httpRequest.getQueryString();
		if (query != null) {
			if (query.charAt(0) != '?') {
				uri.append('?');
			}
			uri.append(query);
		}
		return referer.toLowerCase().endsWith(uri.toString().toLowerCase());
	}

	/**
	 * Send unauthorized access response. This method send back a response with status code
	 * {@link HttpServletResponse#SC_UNAUTHORIZED} and response header {@link HttpHeader#WWW_AUTHENTICATE} set to basic
	 * authentication method and {@link IContainer#getLoginRealm()} authentication realm.
	 * <p>
	 * If request is from an agent using XHR this method behaves a little different. XHR specification mandates that
	 * unauthorized access to be handled transparently by client agent that usually displays client agent login form, not very
	 * well integrated with application. Below is a snippet from this framework script library.
	 * <p>
	 * For not authorized XHR requests this method sends {@link HttpServletResponse#SC_OK} and custom response header
	 * {@link HttpHeader#X_HEADER_LOCATION} set to application login page, see {@link IContainer#getLoginPage()}. Client script
	 * can handle this response and redirect to given login page.
	 * 
	 * <pre>
	 * var redirect = this._request.getResponseHeader(&quot;X-JSLIB-Location&quot;);
	 * if (redirect) {
	 * 	WinMain.assign(redirect);
	 * }
	 * </pre>
	 * <p>
	 * Anyway, if application is not configured with login page, for rejected XHR requests this method still uses standard
	 * {@link HttpServletResponse#SC_UNAUTHORIZED}.
	 * 
	 * @param context current request context.
	 */
	protected static void sendUnauthorized(RequestContext context) {
		final HttpServletResponse httpResponse = context.getResponse();

		if (httpResponse.isCommitted()) {
			log.fatal("Abort HTTP transaction. Attempt to send reponse after response already commited.");
			return;
		}
		log.error("Reject unauthorized request for private resource or service: {http_url}.", context.getRequestURI());

		final String realm = String.format("Basic realm=%s", context.getRequest().getServletContext().getServletContextName());
		log.trace("Send WWW-Authenticate |{http_realm}| for rejected request: {http_url}", realm, context.getRequestURI());
		httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		httpResponse.setHeader(HttpHeader.WWW_AUTHENTICATE, realm);
	}

	/**
	 * Send response for bad request with request URI as message. This utility method is used when a request URI is not well
	 * formed, as expected by concrete servlet implementation. It dumps request context and delegates servlet container,
	 * {@link HttpServletResponse#sendError(int, String)} to send {@link HttpServletResponse#SC_BAD_REQUEST}.
	 * 
	 * @param context request context.
	 * @throws IOException if writing to HTTP response fails.
	 */
	protected static void sendBadRequest(RequestContext context) throws IOException {
		log.error("Bad request format for resource or service: {http_url}.", context.getRequestURI());
		context.dump();
		context.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, context.getRequestURI());
	}

	/**
	 * Send response for resource or service not found, containing the exception that describes missing entity. This method
	 * sends back exception object wrapped in {@link RemoteException}. Response is encoded JSON and status code is
	 * {@link HttpServletResponse#SC_NOT_FOUND}.
	 * 
	 * @param context current request context,
	 * @param exception exception describing missing entity.
	 * @throws IOException if writing to response stream fails.
	 */
	protected static void sendNotFound(RequestContext context, Exception exception) throws IOException {
		log.error("Request for missing resource or service: {http_url}.", context.getRequestURI());
		sendJsonObject(context, new RemoteExceptionContext(exception), HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Send server error response with given exception serialized JSON. This utility method dumps stack trace and request
	 * context to application logger and send back throwable object. Response is encoded JSON and status code is
	 * {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR}.
	 * 
	 * There is a special case for {@link BusinessException}. This exception signals broken business constrain and is send back
	 * also as JSON object but with status code {@link HttpServletResponse#SC_BAD_REQUEST}. Also does not dump exception stack
	 * trace or request context.
	 * 
	 * @param context current request context,
	 * @param throwable exception describing server error.
	 * @throws IOException if writing to response stream fails.
	 */
	protected static void sendError(RequestContext context, Throwable throwable) throws IOException {
		if (throwable instanceof InvocationException && throwable.getCause() != null) {
			throwable = throwable.getCause();
		}
		if (throwable instanceof InvocationTargetException) {
			Throwable t = ((InvocationTargetException) throwable).getTargetException();
			if (t == null) {
				t = throwable.getCause();
			}
			if (t != null) {
				throwable = t;
			}
		}
		if (throwable instanceof BusinessException) {
			// business constrains exception is generated by user space code and sent to client using HTTP response
			// status 400 - HttpServletResponse.SC_BAD_REQUEST, as JSON serialized object
			log.debug("Send business constrain exception |{business_code}|.", ((BusinessException) throwable).getErrorCode());
			sendJsonObject(context, throwable, HttpServletResponse.SC_BAD_REQUEST);
		} else {
			dumpError(context, throwable);
			sendJsonObject(context, new RemoteExceptionContext(throwable), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Dump throwable stack trace and request context to application logger.
	 * 
	 * @param context request context,
	 * @param throwable throwable to dump stack trace for.
	 */
	protected static void dumpError(RequestContext context, Throwable throwable) {
		log.dump(throwable);
		context.dump();
	}

	/**
	 * Send object back to client encoded JSON with given HTTP status code. Take care to set content type, length and language.
	 * Content language is extracted from request context locale.
	 * 
	 * @param context running request context,
	 * @param object object to serialize back to client,
	 * @param statusCode response status code.
	 * @throws IOException if serialization process fails.
	 */
	protected static void sendJsonObject(RequestContext context, Object object, int statusCode) throws IOException {
		final HttpServletResponse httpResponse = context.getResponse();
		if (httpResponse.isCommitted()) {
			log.fatal("Abort HTTP transaction. Attempt to send JSON object after reponse commited.");
			return;
		}

		Json json = context.getContainer().getInstance(Json.class);
		String buffer = json.stringify(object);
		log.trace("Send response object {java_type}. Object dump:{__message_extra__}", object.getClass(), buffer);
		byte[] bytes = buffer.getBytes("UTF-8");

		httpResponse.setStatus(statusCode);
		httpResponse.setContentType(ContentType.APPLICATION_JSON.getValue());
		httpResponse.setContentLength(bytes.length);
		httpResponse.setHeader("Content-Language", context.getLocale().toLanguageTag());

		httpResponse.getOutputStream().write(bytes);
		httpResponse.getOutputStream().flush();
	}

	protected static void sendRedirect(RequestContext context, String location) throws IOException {
		sendRedirect(context, HttpServletResponse.SC_MOVED_TEMPORARILY, location);
	}

	protected static void sendRedirect(RequestContext context, int statusCode, String location) throws IOException {
		final HttpServletResponse httpResponse = context.getResponse();
		if (httpResponse.isCommitted()) {
			log.fatal("Abort HTTP transaction. Attempt to redirect after reponse commited.");
			return;
		}
		log.trace("Send redirect |{http_status}| to |{http_location}|.", statusCode, location);
		httpResponse.setStatus(statusCode);
		httpResponse.setHeader("Location", location);
		httpResponse.getOutputStream().flush();
	}

	private static class PreviewUser implements Principal {
		@Override
		public String getName() {
			return "preview-user";
		}
	}

	// --------------------------------------------------------------------------------------------
	// unit tests access

	String servletName() {
		return servletName;
	}

	IContainer container() {
		return container;
	}

	String previewContextPath() {
		return previewContextPath;
	}
}
