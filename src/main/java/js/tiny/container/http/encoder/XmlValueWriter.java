package js.tiny.container.http.encoder;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletResponse;

import js.dom.Document;

/**
 * Write XML document to HTTP response.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class XmlValueWriter implements ValueWriter {
	/**
	 * Serialize XML document to output stream of given HTTP response.
	 * 
	 * @param httpResponse HTTP response,
	 * @param value value object is a XML document.
	 * @throws IOException if output stream write operation fails.
	 */
	@Override
	public void write(HttpServletResponse httpResponse, Object value) throws IOException {
		final Document document = (Document) value;
		document.serialize(new OutputStreamWriter(httpResponse.getOutputStream(), "UTF-8"));
	}
}
