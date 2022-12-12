package com.jslib.container.http.encoder;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.util.Strings;

/**
 * Value writer for plain text. This value writer simply serialize given string value to HTTP response stream. Uses
 * {@link Converter} to convert value to string, if is not already string.
 * 
 * @author Iulian Rotaru
 */
public class PlainTextValueWriter implements ValueWriter {
	private final Converter converter = ConverterRegistry.getConverter();

	/**
	 * Serialize value to HTTP response stream. Convert value to string if necessary.
	 * 
	 * @param httpResponse HTTP response,
	 * @param value value to send on HTTP response.
	 * @throws IOException if writing to HTTP response output stream fails.
	 */
	@Override
	public void write(HttpServletResponse httpResponse, Object value) throws IOException {
		Strings.save(converter.asString(value), httpResponse.getOutputStream());
	}
}
