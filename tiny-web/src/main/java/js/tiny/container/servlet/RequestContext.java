package js.tiny.container.servlet;

import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.InstanceScope;

/**
 * Request context stored on current HTTP servlet request thread. This class allows access to container public services and to
 * relatively low level servlet abstractions like {@link HttpServletRequest}. Since exposes tiny container and servlet APIs this
 * class is primarily for framework internal needs. Anyway, there is no formal restriction on using it from applications.
 * <p>
 * Request context instance has {@link InstanceScope#THREAD} scope but is valid to be used only within HTTP request boundaries.
 * Request context instance is {@link #attach(HttpServletRequest, HttpServletResponse)}ed on new HTTP request thread and
 * {@link #detach()}ed on HTTP request end.
 * <p>
 * RequestContext is a managed class and can be retrieved from application context or can be injected on fields or constructors.
 * In sample below request context instance is retrieved from application context. Request context is attached to HTTP request
 * thread that runs <code>addSuggestion</code> method .
 * 
 * <pre>
 * class Controller {
 * 	&#064;Inject
 * 	private AppContext appContext;
 *  
 * 	void addSuggestion(Suggestion suggestion) {
 * 		RequestContext requestContext = appContext.getInstance(RequestContext.class);
 * 		log.debug(&quot;Request from |%s|.&quot;, requestContext.getRemoteAddr());
 * 	}
 * 	... 
 * }
 * </pre>
 * <p>
 * Request context can also be injected into field. Now, controller is a managed class with {@link InstanceScope#APPLICATION}
 * scope whereas request context scope is {@link InstanceScope#THREAD}. Since request context life span is smaller than
 * controller, request context may become invalid while controller still handles requests. Container dependency injection takes
 * care and replace injected request context instance with a Java proxy.
 * 
 * <pre>
 * class Controller {
 * 	&#064;Inject
 * 	private RequestContext context;
 * 	... 
 * }
 * </pre>
 * <p>
 * The same goes for constructor injection: request context instance is wrapped by a Java proxy that routes method invocations
 * to the instance attached to current HTTP request.
 * 
 * <pre>
 * class Controller {
 * 	private final RequestContext context;
 * 
 * 	Controller(RequestContext context) {
 * 		this.context = context;
 * 	}
 * 	...
 * }
 * </pre>
 * <p>
 * <b>RequestContext is usable only inside HTTP request</b>. Trying to used this class from user defined threads, perhaps with
 * life span exceeding request thread, is both meaningless and dangerous; for this reason is forbidden. In fact bug error is
 * thrown if attempt to use any method of this class from outside request thread. One can use {@link #isAttached()} to test if
 * request context is safe to use.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class RequestContext {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(RequestContext.class);

	private final Converter converter;

	/** Parent container. */
	private final ITinyContainer container;

	/**
	 * Original, not pre-processed by {@link RequestPreprocessor} request URI including query string, if any. This value is
	 * merely used for request logging.
	 */
	private String requestURL;

	/** Request locale. Locale value can be loaded by {@link RequestPreprocessor} or from HTTP request. */
	private Locale locale;

	/**
	 * Optional security domain set if application is configured with servlet container provided security. See
	 * {@link RequestPreprocessor} for security domain description.
	 */
	private String securityDomain;

	/**
	 * Request path is the part of request URI that identifies managed method to be invoked. It is the last part of the request
	 * URI - after context path, optional locale and security domain, and its structure depends on HTTP request protocol.
	 * Anyway, it always starts with path separator.
	 * <p>
	 * If application is configured with request filter, this request path is computed by {@link RequestPreprocessor}. If not,
	 * is loaded from request URI when attach this request context.
	 */
	private String requestPath;

	/** Wrapped HTTP servlet request. */
	private HttpServletRequest httpRequest;

	/** HTTP servlet response related to wrapped HTTP servlet request. */
	private HttpServletResponse httpResponse;

	/** Current request cookies. */
	private Cookies cookies;

	/**
	 * Flag true while request context instance is attached to HTTP servlet request. It is set to true by
	 * {@link #attach(HttpServletRequest, HttpServletResponse)} and reset to false by {@link #detach()}.
	 */
	private volatile boolean attached;

	/**
	 * Create request context instance for given parent container.
	 * 
	 * @param container parent container.
	 */
	public RequestContext(ITinyContainer container) {
		this.converter = ConverterRegistry.getConverter();
		this.container = container;
	}

	/**
	 * Get parent container.
	 * 
	 * @return parent container.
	 * @see #container
	 */
	public ITinyContainer getContainer() {
		return container;
	}

	/**
	 * Set request URL. This setter is invoked by {@link RequestPreprocessor} with original request URI and query string.
	 * 
	 * @param requestURL original request URI with optional query string.
	 * @see #requestURL
	 */
	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}

	/**
	 * Set request locale. This setter is invoked if application is configured with multiple locale support. See
	 * {@link RequestPreprocessor} for multiple locale configuration.
	 * 
	 * @param locale request locale to set.
	 * @see #locale
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Set security domain. This setter is invoked if application has security provided by servlet container. See
	 * {@link RequestPreprocessor} for security domain description.
	 * 
	 * @param securityDomain request security domain.
	 * @see #securityDomain
	 */
	public void setSecurityDomain(String securityDomain) {
		this.securityDomain = securityDomain;
	}

	/**
	 * Set request path used to identify managed method to be invoked. This setter is invoked by {@link RequestPreprocessor}
	 * after removing context path and optional locale and security domain from request URI.
	 * 
	 * @param requestPath request path.
	 * @see #requestPath
	 */
	public void setRequestPath(String requestPath) {
		this.requestPath = requestPath;
	}

	/**
	 * Attach this instance to HTTP servlet request. Load this instance state from HTTP servlet request and mark it as attached.
	 * 
	 * @param httpRequest current HTTP request object,
	 * @param httpResponse HTTP response object associated to current request.
	 */
	public void attach(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		// takes care to not override request URL, locale and request path values if set by request pre-processor
		if (requestURL == null) {
			requestURL = httpRequest.getRequestURI();
		}
		if (locale == null) {
			locale = httpRequest.getLocale();
		}
		if (requestPath == null) {
			// request URI and context path cannot ever be null
			requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
		}

		this.httpRequest = httpRequest;
		this.httpResponse = httpResponse;
		this.attached = true;
	}

	/**
	 * Detach request context instance from HTTP servlet request. Invoking getters on this instance after detaching is
	 * considered a bug.
	 */
	public void detach() {
		attached = false;
		locale = null;
		securityDomain = null;
		cookies = null;
		requestPath = null;
		requestURL = null;
	}

	/**
	 * Get instance attached state.
	 * 
	 * @return instance attached state.
	 * @see #attached
	 */
	public boolean isAttached() {
		return attached;
	}

	/**
	 * Get request locale. Request locale is loaded by {@link RequestPreprocessor} filter or from HTTP servlet request if
	 * application is not configured with multiple locale.
	 * 
	 * @return request locale.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 * @see #locale
	 */
	public Locale getLocale() {
		assertAttached();
		return locale;
	}

	/**
	 * Get security domain from request URI or null if servlet container security is not enabled. See
	 * {@link RequestPreprocessor} for security domain description.
	 * 
	 * @return request URI security domain, possible null.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 * @see #securityDomain
	 */
	public String getSecurityDomain() {
		assertAttached();
		return securityDomain;
	}

	/**
	 * Get request path used to identify managed method to be invoked.
	 * 
	 * @return request path.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 * @see #requestPath
	 */
	public String getRequestPath() {
		assertAttached();
		return requestPath;
	}

	/**
	 * Get URI of the request that trigger this request context creation. If application has request filter active request URI
	 * is pre-processed by {@link RequestPreprocessor}.
	 * 
	 * @return request URI.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public String getRequestURI() {
		assertAttached();
		return httpRequest.getRequestURI();
	}

	/**
	 * Get request URI including query string, if any. If application has request filter active request URI is pre-processed by
	 * {@link RequestPreprocessor}. This method returns null if request URL is not initialized by {@link RequestPreprocessor}.
	 * 
	 * @return request URI with optional query string or null if request URL is not initialized.
	 * @see #requestURL
	 */
	public String getRequestURL() {
		return requestURL;
	}

	/**
	 * Get request cookies.
	 * 
	 * @return request cookies.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public Cookies getCookies() {
		assertAttached();
		if (cookies == null) {
			cookies = new Cookies(httpRequest, httpResponse);
		}
		return cookies;
	}

	/**
	 * Returns the host name of the Internet Protocol (IP) interface on which the request was received.
	 * 
	 * @return IP address on which the request was received.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public String getLocalName() {
		assertAttached();
		return httpRequest.getLocalName();
	}

	/**
	 * Returns the IP port number of the interface on which HTTP request was received.
	 * 
	 * @return an integer specifying the port number.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public int getLocalPort() {
		assertAttached();
		return httpRequest.getLocalPort();
	}

	/**
	 * Returns the fully qualified name of the client or the last proxy that sent the request. If the engine cannot or chooses
	 * not to resolve the hostname (to improve performance), this method returns the dotted-string form of the IP address.
	 * 
	 * @return IP address of the client that sent HTTP request.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public String getRemoteHost() {
		assertAttached();
		return httpRequest.getRemoteHost();
	}

	/**
	 * Returns the IP source port of the client or last proxy that sent HTTP request.
	 * 
	 * @return an integer specifying the port number.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public int getRemotePort() {
		assertAttached();
		return httpRequest.getRemotePort();
	}

	public String getForwardContextPath() {
		assertAttached();
		return (String) httpRequest.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH);
	}

	/**
	 * Allows access to servlet request interface.
	 * 
	 * @return low level servlet request.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public HttpServletRequest getRequest() {
		assertAttached();
		return httpRequest;
	}

	/**
	 * Allows access to servlet response interface.
	 * 
	 * @return low level servlet response.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public HttpServletResponse getResponse() {
		assertAttached();
		return httpResponse;
	}

	/**
	 * Get HTTP session this request context is part of or null if session is not or could not be created. If
	 * <code>create</code> flag is not provided this method return current HTTP session as it is, that is, return it if already
	 * created or null if not.
	 * <p>
	 * If <code>create</code> flag is present and is true this method returns current HTTP session, if there is one associated
	 * with current request, or create a new one. This method never returns null if requested to create session but can throw
	 * illegal state exception if attempt to create session after response commit.
	 * 
	 * @param create optional create flag.
	 * @return current HTTP session possible null.
	 * @throws IllegalStateException if attempt to create new HTTP session after response was committed.
	 * @throws BugError if attempt to use this getter on instance not attached to a HTTP servlet request.
	 */
	public HttpSession getSession(boolean... create) {
		assertAttached();
		if (create.length == 0) {
			return httpRequest.getSession();
		}
		// if create flag is true next call can throw IllegalStateException if HTTP response is committed
		return httpRequest.getSession(create[0]);
	}

	/**
	 * Return value of the named context-wide initialization parameter, or <code>null</code> if the parameter does not exist.
	 *
	 * @param parameterName parameter name.
	 * @return value of the context-wide initialization parameter, possible null.
	 */
	public <T> T getInitParameter(Class<T> type, String parameterName) {
		String value = getRequest().getServletContext().getInitParameter(parameterName);
		return converter.asObject(value, type);
	}

	/** Dump this request context state to error logger. If this instance is not attached this method is NOP. */
	public void dump() {
		if (!attached) {
			return;
		}
		StringBuilder message = new StringBuilder();
		message.append("Request context |");
		message.append(httpRequest.getRequestURI());
		message.append("|:");

		message.append(System.lineSeparator());
		message.append("\t- remote-address: ");
		message.append(httpRequest.getRemoteHost());

		message.append(System.lineSeparator());
		message.append("\t- method: ");
		message.append(httpRequest.getMethod());

		message.append(System.lineSeparator());
		message.append("\t- query-string: ");
		if (httpRequest.getQueryString() != null) {
			message.append(httpRequest.getQueryString());
		}

		Enumeration<String> headerNames = httpRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			message.append(System.lineSeparator());
			String headerName = headerNames.nextElement();
			message.append("\t- ");
			message.append(headerName);
			message.append(": ");
			message.append(httpRequest.getHeader(headerName));
		}
		log.error(message.toString());
	}

	/**
	 * Assert this instance is attached to a HTTP servlet request.
	 * 
	 * @throws BugError if this instance is not attached.
	 * @see #attached
	 */
	private void assertAttached() {
		if (!attached) {
			throw new BugError("Attempt to use request context outside HTTP request thread.");
		}
	}
}
