package js.tiny.container.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.converter.Converter;
import js.converter.ConverterException;
import js.converter.ConverterRegistry;
import js.util.Params;

/**
 * Cookies utility provide means for individual cookie handling and cookies collection iteration.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class Cookies implements Iterable<Cookie> {
	/** Reference to current HTTP response. */
	private HttpServletResponse httpResponse;

	/** Current HTTP request cookies provided by servlet. */
	private Cookie[] cookies;

	/**
	 * Create cookies utility instance and load internal cookies from HTTP request.
	 * 
	 * @param httpRequest current HTTP request,
	 * @param httpResponse current HTTP response.
	 */
	public Cookies(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		this.httpResponse = httpResponse;
		this.cookies = httpRequest.getCookies();
	}

	/**
	 * Test if named cookie exists.
	 * 
	 * @param name cookie name, not null or empty.
	 * @return true if named cookie exists.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 */
	public boolean has(String name) {
		return get(name) != null;
	}

	/**
	 * Get value of the named cookie or null if cookie does not exist.
	 * 
	 * @param name cookie name, not null or empty.
	 * @return cookie cookie value or null if there is no cookie with requested name.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 */
	public String get(String name) {
		Params.notNullOrEmpty(name, "Cookie name");
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	/**
	 * Add cookie to HTTP response of the current request context. Override cookie value if already exists.
	 * 
	 * @param name cookie name, not null or empty,
	 * @param value cookie value, not null or empty.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 * @throws IllegalArgumentException if <code>value</code> argument is null or empty.
	 */
	public void add(String name, String value) {
		Params.notNullOrEmpty(name, "Cookie name");
		Params.notNull(value, "Cookie value");
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		httpResponse.addCookie(cookie);
	}

	/**
	 * Convert value object to string and delegates {@link #add(String, String)}. String conversion is performed by
	 * {@link Converter} and may throw {@link ConverterException}.
	 * 
	 * @param name cookie name, not null or empty,
	 * @param value cookie value, not null or empty.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 * @throws IllegalArgumentException if <code>value</code> argument is null or empty.
	 * @throws ConverterException if value object has no converter registered or object value serialization fails.
	 */
	public void add(String name, Object value) {
		add(name, ConverterRegistry.getConverter().asString(value));
	}

	/**
	 * Remove cookie from HTTP response. If named cookie does not exist this method does nothing.
	 * 
	 * @param name the name of the cookie to be removed, not null or empty.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 */
	public void remove(String name) {
		Params.notNullOrEmpty(name, "Cookie name");
		if (cookies == null) {
			return;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				cookie.setMaxAge(0);
				cookie.setValue("");
				cookie.setPath("/");
				httpResponse.addCookie(cookie);
				break;
			}
		}
	}

	/**
	 * Get cookies iterator. Return empty iterator if not cookies on HTTP request.
	 * 
	 * @return cookies iterator, possible empty.
	 */
	public Iterator<Cookie> iterator() {
		if (cookies == null) {
			return Collections.emptyIterator();
		}
		return Arrays.asList(cookies).iterator();
	}
}
