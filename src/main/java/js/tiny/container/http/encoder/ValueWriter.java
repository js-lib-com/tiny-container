package js.tiny.container.http.encoder;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

/**
 * Net method return value writer to HTTP response. Value writer serializes value returned by a net method to the HTTP response
 * body. Implementation may set additional response headers, e.g. <code>Content-Length</code>. This is the reason method
 * {@link #write(HttpServletResponse, Object)} signature uses {@link HttpServletResponse} and not {@link OutputStream}.
 * <p>
 * Implementation should not close HTTP response output stream, but flush is recommended. HTTP response close is performed by
 * outer logic.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ValueWriter {
	/**
	 * Serialize value to HTTP response output stream and optionally set specific headers.
	 * 
	 * @param httpResponse HTTP response,
	 * @param value return value.
	 * @throws IOException if writing to HTTP response output stream fails.
	 */
	void write(HttpServletResponse httpResponse, Object value) throws IOException;
}
