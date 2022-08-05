package com.jslib.tiny.container.http;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * A resource is any entity serializable back to client. Classic example is the HTML page generated dynamically but by no means
 * is limited to. A resource can be about anything: images, any type of documents, media, redirection request... anything client
 * knows to interpret. Please note that this abstraction deals only with dynamically generated resource, static resource being
 * served by servlet container or front-end HTTP server. In fact requests for static resources never reach Tiny Container.
 * 
 * Resource is also responsible for setting HTTP response headers, notably <code>Content-Type</code>. For content type there is
 * a {@link #setContentType(String) setter}.
 * 
 * @author Iulian Rotaru
 */
public interface Resource {

	/**
	 * Set resource content type. This setter is optional; default value for resource content type is
	 * <code>text/html;charset=UTF-8</code>.
	 * 
	 * @param contentType resource content type.
	 */
	default void setContentType(String contentType) {
	}

	/**
	 * Serialize this resource to given HTTP response. Resource content is serialized on response output stream and response
	 * headers are properly set. <code>Content-Type</code> header is mandatory.
	 * 
	 * @param httpResponse HTTP response.
	 * @throws IOException if serialization process fails.
	 * @throws IllegalStateException if HTTP response is committed.
	 */
	void serialize(HttpServletResponse httpResponse) throws IOException;

}
