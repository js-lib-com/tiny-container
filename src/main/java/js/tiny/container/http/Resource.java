package js.tiny.container.http;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * A resource is any entity serializable back to client. Classic example is the HTML page generated dynamically but by no means
 * is limited to. A resource can be about anything: images, any type of documents, media, redirection request... anything client
 * knows to interpret. Please note that this abstraction deals only with dynamically generated resource, static resource being
 * served by servlet container or front-end HTTP server. In fact requests for static resources never reach Tiny Container.
 * <p>
 * Resource is also responsible for setting HTTP response headers, notably <code>Content-Type</code>.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface Resource {
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
