package js.tiny.container.http.encoder;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.http.HttpServletResponse;
import js.io.StreamHandler;

/**
 * Write bytes to output stream of the HTTP response. This return value writer uses {@link StreamHandler} to inject bytes from
 * application code to HTTP response output stream.
 * <p>
 * This value writer executes {@link StreamHandler#invokeHandler(OutputStream)} with HTTP response output stream. Application
 * code implements stream handler that gets output stream provided by this value writer.
 * 
 * <pre>
 * class Controller {
 * 	public StreamHandler&lt;OutputStream&gt; downloadStream() {
 * 		return new StreamHandler&lt;OutputStream&gt;(OutputStream.class) {
 * 			&#064;Override
 * 			public void handle(OutputStream outputStream) throws IOException {
 * 				// output stream is from HTTP response
 * 				outputStream.write(&quot;output stream&quot;.getBytes(&quot;UTF-8&quot;));
 * 			}
 * 		};
 * 	}
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class StreamValueWriter implements ValueWriter {
	/**
	 * Write bytes to output stream of HTTP response. This method invokes {@link StreamHandler#invokeHandler(OutputStream)} with
	 * HTTP response output stream. Stream handler instance is created by application and allows application logic access to
	 * HTTP response stream.
	 * 
	 * @param httpResponse HTTP response,
	 * @param value object value is a stream handler instance created by application code.
	 * @throws IOException if writing to HTTP response output stream fails.
	 */
	@Override
	public void write(HttpServletResponse httpResponse, Object value) throws IOException {
		((StreamHandler<?>) value).invokeHandler(httpResponse.getOutputStream());
	}
}
