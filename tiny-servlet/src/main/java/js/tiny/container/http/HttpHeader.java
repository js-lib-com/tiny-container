package js.tiny.container.http;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP headers constants and related helpers.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class HttpHeader {
	/** Content-Types that are acceptable for the response. */
	public static final String ACCEPT = "Accept";
	/** Character sets that are acceptable. */
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	/** List of acceptable encodings. */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	/** List of acceptable human languages for response. */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	/** Authentication credentials for HTTP authentication. */
	public static final String AUTHORIZATION = "Authorization";
	/** Used to specify directives that must be obeyed by all caching mechanisms along the request-response chain. */
	public static final String CACHE_CONTROL = "Cache-Control";
	/** An HTTP cookie previously sent by the server with Set-Cookie. */
	public static final String COOKIE = "Cookie";
	/** Controls whether or not the network connection stays open after the current transaction finishes. */
	public static final String CONNECTION = "Connection";
	/** The type of encoding used on the data. */
	public static final String CONTENT_ENCODING = "Content-Encoding";
	/** The natural language or languages of the intended audience for the enclosed content. */
	public static final String CONTENT_LANGUAGE = "Content-Language";
	/** The length of the response body in 8-bit bytes. */
	public static final String CONTENT_LENGTH = "Content-Length";
	/** An alternate location for the returned data. */
	public static final String CONTENT_LOCATION = "Content-Location";
	/** The MIME type of the body of the HTTP message. */
	public static final String CONTENT_TYPE = "Content-Type";
	/** The date and time that the message was originated, RFC 7231 Date/Time Formats. */
	public static final String DATE = "Date";
	/** An identifier for a specific version of a resource, often a message digest. */
	public static final String ETAG = "ETag";
	/** Gives the date/time after which the response is considered stale, RFC 7231 Date/Time Formats. */
	public static final String EXPIRES = "Expires";
	/**
	 * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening. The port
	 * number may be omitted if the port is the standard port for the service requested.
	 */
	public static final String HOST = "Host";
	/** Only perform the action if the client supplied entity matches the same entity on the server. */
	public static final String IF_MATCH = "If-Match";
	/** Allows a 304 Not Modified to be returned if content is unchanged. */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	/** Allows a 304 Not Modified to be returned if content is unchanged. */
	public static final String IF_NONE_MATCH = "If-None-Match";
	/** Only send the response if the entity has not been modified since a specific time. */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	/** Used in redirection, or when a new resource has been created. */
	public static final String LOCATION = "Location";
	/** Implementation-specific fields that may have various effects anywhere along the request-response chain. */
	public static final String PRAGMA = "Pragma";
	/** This is the address of the previous web page from which a link to the currently requested page was followed. */
	public static final String REFERER = "Referer";
	/** SOAP action encoded into HTTP header. */
	public static final String SOAP_ACTION = "SOAPAction";
	/** The user agent string of the user agent. */
	public static final String USER_AGENT = "User-Agent";
	/**
	 * Tells downstream proxies how to match future request headers to decide whether the cached response can be used rather
	 * than requesting a fresh one from the origin server.
	 */
	public static final String VARY = "Vary";
	/** Indicates the authentication scheme that should be used to access the requested entity. */
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	/** An HTTP cookie. */
	public static final String SET_COOKIE = "Set-Cookie";
	/** j(s)-lib extension header for location redirect via 200 OK. */
	public static final String X_HEADER_LOCATION = "X-JSLIB-Location";
	/** Mainly used to identify AJAX requests. */
	public static final String X_REQUESTED_WITH = "X-Requested-With";

	/** Value for Connection header that requires to keep network connection opened after current HTTP transaction ends. */
	public static final String KEEP_ALIVE = "keep-alive";

	/** Cache control header value for disabled cache. */
	public static final String NO_CACHE = "no-cache";
	/** Cache control header value for disabled cache. */
	public static final String NO_STORE = "no-store";

	/** Header value for requested with AJAX. */
	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	/** Header value for requested from Android library. */
	private static final String ANDROID_USER_AGENT = "j(s)-lib android";

	/** Prevent default constructor synthesis but allow sub-classing. */
	protected HttpHeader() {
	}

	/**
	 * Test if given HTTP request is performed via XMLHttpRequest.
	 * 
	 * @param httpRequest HTTP request.
	 * @return true if X-Requested-With header is XMLHttpRequest.
	 */
	public static boolean isXHR(HttpServletRequest httpRequest) {
		String requestedWith = httpRequest.getHeader(X_REQUESTED_WITH);
		return requestedWith != null ? requestedWith.equalsIgnoreCase(XML_HTTP_REQUEST) : false;
	}

	/**
	 * Test if HTTP request is from Android.
	 * 
	 * @param httpRequest HTTP request.
	 * @return true if X-Requested-With header contains j(s)-lib android signature.
	 */
	public static boolean isAndroid(HttpServletRequest httpRequest) {
		String requestedWith = httpRequest.getHeader(X_REQUESTED_WITH);
		return requestedWith != null ? requestedWith.equalsIgnoreCase(ANDROID_USER_AGENT) : false;
	}

	/**
	 * Predicate to test for SOAP request. Accordingly SOAP specifications every SOAP request MUST include SOAPAction header.
	 * 
	 * @param httpRequest HTTP request.
	 * @return true if SOAPAction header is present.
	 */
	public static boolean isSOAP(HttpServletRequest httpRequest) {
		return httpRequest.getHeader(SOAP_ACTION) != null;
	}
}
