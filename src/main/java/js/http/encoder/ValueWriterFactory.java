package js.http.encoder;

import js.http.ContentType;
import js.lang.BugError;

/**
 * Factory for return value writers. This interface is part of a strategy that selects the value writer appropriate for return
 * value serialization to HTTP response.
 * <p>
 * It is performed in two steps: first identify content type suitable for given object value,
 * {@link #getContentTypeForValue(Object)} then select the actual writer for detected content type -
 * {@link #getValueWriter(ContentType)}.
 * 
 * <pre>
 * Object value = ...
 * ContentType contentType = factory.getContentTypeForValue(value);
 * ValueWriter writer = factory.getValueWriter(contentType);
 * ...
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ValueWriterFactory {
	/**
	 * Determine content type usable to retrieve a writer able to handle given object value. This method is used in tandem with
	 * {@link #getValueWriter(ContentType)}.
	 * <p>
	 * Implementation should determine content type based on object value class. If no suitable content type found
	 * implementation should return {@link ContentType#APPLICATION_JSON}.
	 * 
	 * @param value object value.
	 * @return content type for given object value.
	 */
	ContentType getContentTypeForValue(Object value);

	/**
	 * Get return value writer able to handle requested content type. Content type argument should identify a registered value
	 * writer. If no writer found implementation should throw bug error.
	 * <p>
	 * Recommend way to use this method is to provide content type instance returned by {@link #getContentTypeForValue(Object)}.
	 * 
	 * @param contentType content type for return value.
	 * @return return value writer, never null.
	 * @throws BugError if no value writer registered for requested content type.
	 */
	ValueWriter getValueWriter(ContentType contentType);
}
