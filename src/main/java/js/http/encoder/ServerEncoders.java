package js.http.encoder;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.servlet.http.HttpServletRequest;

import js.dom.Document;
import js.http.ContentType;
import js.io.StreamHandler;
import js.lang.BugError;
import js.lang.IllegalArgumentException;
import js.log.Log;
import js.log.LogFactory;

/**
 * Server encoders registry for invocation arguments readers and return value writers. This registry is a singleton. On its
 * construction loads readers and writers, both built-in and provided by service implementation of {@link HttpEncoderProvider}
 * interface.
 * <p>
 * This registry has methods to retrieve reader and writer instances able to handle requested content and Java types, see
 * {@link #getArgumentsReader(HttpServletRequest, Type[])} and {@link #getValueWriter(ContentType)}. There is also a method to
 * determine content type appropriate to handle a requested object instance, see {@link #getContentTypeForValue(Object)}. This
 * method is a companion for return value writers.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class ServerEncoders implements ArgumentsReaderFactory, ValueWriterFactory {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(ServerEncoders.class);

	/** Server encoders singleton. */
	private static final ServerEncoders instance = new ServerEncoders();

	/**
	 * Get server encoders instance.
	 * 
	 * @return server encoders instance.
	 */
	public static ServerEncoders getInstance() {
		return instance;
	}

	/** Registered invocation arguments readers. */
	private final Map<EncoderKey, ArgumentsReader> readers = new HashMap<>();

	/** Registered return value writers. */
	private final Map<ContentType, ValueWriter> writers = new HashMap<>();

	/**
	 * Construct server encoders instance. Loads invocation arguments readers and return value writers, both built-in and
	 * provided by service implementation of {@link HttpEncoderProvider} interface.
	 */
	private ServerEncoders() {
		readers.put(null, new UrlQueryArgumentsReader());
		readers.put(new EncoderKey(ContentType.APPLICATION_JSON), new JsonArgumentsReader());
		readers.put(new EncoderKey(ContentType.APPLICATION_STREAM), new StreamArgumentsReader());
		readers.put(new EncoderKey(ContentType.MULTIPART_MIXED), new MultipartMixedArgumentsReader(this));
		readers.put(new EncoderKey(ContentType.TEXT_XML), new XmlArgumentsReader());
		readers.put(new EncoderKey(ContentType.MULTIPART_FORM), new MultipartFormArgumentsReader());
		readers.put(new EncoderKey(ContentType.URLENCODED_FORM), new FormUrlArgumentsReader());

		writers.put(ContentType.TEXT_XML, new XmlValueWriter());
		writers.put(ContentType.APPLICATION_STREAM, new StreamValueWriter());
		writers.put(ContentType.APPLICATION_JSON, new JsonValueWriter());

		for (HttpEncoderProvider encoderProvider : ServiceLoader.load(HttpEncoderProvider.class)) {
			for (Map.Entry<EncoderKey, ArgumentsReader> entry : encoderProvider.getArgumentsReaders().entrySet()) {
				log.debug("Register arguments reader |%s| for |%s|.", entry.getValue().getClass(), entry.getKey());
				readers.put(entry.getKey(), entry.getValue());
			}
			for (Map.Entry<ContentType, ValueWriter> entry : encoderProvider.getValueWriters().entrySet()) {
				log.debug("Register value writer |%s| for |%s|.", entry.getValue().getClass(), entry.getKey());
				writers.put(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Get arguments reader able to process arguments from HTTP request accordingly expected argument types. If HTTP request has
	 * no content type returns {@link EncoderKey#APPLICATION_JSON} reader, that is, tiny container default content type is JSON.
	 * This departs from HTTP standard that mandates using bytes stream when no content type is provided on request.
	 * <p>
	 * If formal parameters are empty returns {@link EmptyArgumentsReader}. If formal parameters are provided attempts to
	 * retrieve a reader for expected argument types. If none found try using content type solely.
	 * 
	 * @param httpRequest HTTP request carrying arguments to read,
	 * @param formalParameters expected argument types, possible empty.
	 * @return arguments reader, never null.
	 * @throws IllegalArgumentException if no reader found for requested HTTP request and expected argument types.
	 */
	@Override
	public ArgumentsReader getArgumentsReader(HttpServletRequest httpRequest, Type[] formalParameters) {
		if (formalParameters.length == 0) {
			return EmptyArgumentsReader.getInstance();
		}
		if (httpRequest.getQueryString() != null) {
			return readers.get(null);
		}

		return getArgumentsReader(httpRequest.getContentType(), formalParameters[0]);
	}

	@Override
	public ArgumentPartReader getArgumentPartReader(String contentType, Type parameterType) {
		ArgumentsReader reader = getArgumentsReader(contentType, parameterType);
		if (!(reader instanceof ArgumentPartReader)) {
			throw new IllegalArgumentException("Invalid content type for mixed part. Content type |%s| not supported.", contentType);
		}
		return (ArgumentPartReader) reader;
	}

	/**
	 * Get arguments reader for requested content and parameter types. This factory method tries to retrieve a reader for
	 * expected parameter type. If none found it tries using content type solely.
	 * <p>
	 * If content type is null this utility method returns {@link EncoderKey#APPLICATION_JSON} reader since tiny container
	 * default content type is JSON. This departs from HTTP standard that mandates using bytes stream when no content type is
	 * provided on request.
	 * 
	 * @param contentType content type, possible null,
	 * @param parameterType parameter type.
	 * @return reader instance, never null.
	 * @throws IllegalArgumentException if no reader found for requested content and parameter types.
	 */
	private ArgumentsReader getArgumentsReader(String contentType, Type parameterType) {
		if (contentType == null) {
			return readers.get(EncoderKey.APPLICATION_JSON);
		}

		EncoderKey key = new EncoderKey(ContentType.valueOf(contentType), parameterType);
		ArgumentsReader reader = readers.get(key);
		if (reader != null) {
			return reader;
		}

		key = new EncoderKey(ContentType.valueOf(contentType));
		reader = readers.get(key);
		if (reader == null) {
			throw new IllegalArgumentException("Unsupported content type |%s|. There is no arguments reader registered for it.", key);
		}
		return reader;
	}

	/**
	 * Determine content type usable to retrieve a writer able to handle given object value. This method is used in tandem with
	 * {@link #getValueWriter(ContentType)}.
	 * <p>
	 * There is a heuristic to determine content type based on object value class. If no suitable content type found returns
	 * {@link ContentType#APPLICATION_JSON}.
	 * 
	 * @param value object value.
	 * @return content type for given object value.
	 */
	@Override
	public ContentType getContentTypeForValue(Object value) {
		if (value instanceof Document) {
			return ContentType.TEXT_XML;
		}
		if (value instanceof StreamHandler) {
			return ContentType.APPLICATION_STREAM;
		}
		return ContentType.APPLICATION_JSON;
	}

	/**
	 * Get return value writer able to handle requested content type. Content type argument should identify a registered value
	 * writer. If no writer found throws bug error.
	 * <p>
	 * Recommend way to use this method is to provide content type instance returned by {@link #getContentTypeForValue(Object)}.
	 * 
	 * <pre>
	 * Object value = ...
	 * ContentType contentType = encoders.getContentTypeForValue(value);
	 * ValueWriter writer = encoders.getValueWriter(contentType);
	 * ...
	 * </pre>
	 * 
	 * @param contentType content type for return value.
	 * @return return value writer, never null.
	 * @throws BugError if no value writer registered for requested content type.
	 */
	@Override
	public ValueWriter getValueWriter(ContentType contentType) {
		ValueWriter writer = writers.get(contentType);
		if (writer == null) {
			throw new BugError("No return value writer for content type |%s|.", contentType);
		}
		return writer;
	}
}
