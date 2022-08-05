package com.jslib.container.mvc;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import com.jslib.container.http.Resource;
import com.jslib.util.Params;

/**
 * Resource implementation specialized in HTTP temporary redirect with status code 302. This class is a very thin wrapper for
 * {@link HttpServletResponse#sendRedirect(String)} and rely on it for relative location processing.
 * <p>
 * Excerpt from Servlet 3.0 Specification regarding sendRedirect utility method:
 * <em>It is legal to call this method with a relative URL path, however the underlying container must translate the relative path to a fully qualified URL for transmission back to the client.</em>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class Redirect implements Resource {
	/** Redirect location, relative current location or absolute to root context. */
	private final String location;

	/**
	 * Construct redirect instance for given location, relative or absolute. See
	 * {@link HttpServletResponse#sendRedirect(String)} for a discussion about location syntax and processing.
	 * 
	 * @param location redirect location, absolute or relative.
	 * @throws IllegalArgumentException if <code>location</code> argument is null.
	 */
	public Redirect(String location) {
		Params.notNull(location, "Redirect location.");
		this.location = location;
	}

	/**
	 * Send temporary redirect to HTTP response. This method just delegates {@link HttpServletResponse#sendRedirect(String)}
	 * with {@link #location}.
	 * 
	 * @param httpResponse HTTP response.
	 * @throws IOException writing to HTTP response fails.
	 * @throws IllegalStateException if HTTP response is committed or fails to convert relative location to absolute.
	 */
	public void serialize(HttpServletResponse httpResponse) throws IOException {
		httpResponse.sendRedirect(location);
	}
}
