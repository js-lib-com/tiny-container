package js.tiny.container.http.encoder;

import java.io.IOException;
import java.io.OutputStreamWriter;

import jakarta.servlet.http.HttpServletResponse;
import js.json.Json;
import js.util.Classes;

/**
 * Return value serializer with JSON encoding. This return value writer delegates {@link Json#stringify(java.io.Writer, Object)}
 * for actual value serialization.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class JsonValueWriter implements ValueWriter {
	/** JSON serializer delegated for return value encoding. */
	private final Json json;

	/** Create JSON value writer. */
	public JsonValueWriter() {
		json = Classes.loadService(Json.class);
	}

	/**
	 * Serialize return value to HTTP response using JSON encoding. This method delegates
	 * {@link Json#stringify(java.io.Writer, Object)} for value serialization.
	 * 
	 * @param httpResponse HTTP response,
	 * @param value return value.
	 * @throws IOException if writing to HTTP response output stream fails.
	 */
	@Override
	public void write(HttpServletResponse httpResponse, Object value) throws IOException {
		json.stringify(new OutputStreamWriter(httpResponse.getOutputStream(), "UTF-8"), value);
	}
}
