package js.tiny.container.http.encoder;

import java.util.Map;

import js.tiny.container.http.ContentType;

/**
 * Service interface for HTTP encoders providers for both invocation arguments readers and return value writers. If an encoder
 * kind is not provided implementation should return empty map, not null.
 * <p>
 * This interface allows for HTTP encoders extension using Java service loader. Implementation should be deployed as Java
 * service archive, that is, should declare service implementation in META-INF/services directory.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface HttpEncoderProvider {
	/**
	 * Get extension for invocation arguments readers. Returned map is used by {@link ServerEncoders} to register arguments
	 * readers. If extension has no arguments readers returns empty map.
	 * 
	 * @return map of invocation arguments readers, possible empty.
	 */
	Map<EncoderKey, ArgumentsReader> getArgumentsReaders();

	/**
	 * Get extension for return value writers. Returned map is used by {@link ServerEncoders} to register value writers. If
	 * extension has no value writers returns empty map.
	 * 
	 * @return map of return value writers, possible empty.
	 */
	Map<ContentType, ValueWriter> getValueWriters();
}
