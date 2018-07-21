package js.http.encoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.lang.IllegalArgumentException;
import js.util.Classes;
import js.util.Files;
import js.util.Types;

/**
 * Handle XML input stream from HTTP request. This method expects a single formal parameter, current implementation supporting
 * {@link Document} and {@link InputStream}.
 * <p>
 * If formal parameter is a XML document, create a new document instance and load it from XML input stream. If formal parameter
 * is an input stream just hand over HTTP request input stream as it is.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class XmlArgumentsReader implements ArgumentsReader, ArgumentPartReader {
	/**
	 * Cache closeable object till HTTP request is processed by application logic, in order to close it after. See
	 * {@link #clean()}. Uses thread local storage since arguments reader instance is reusable and cannot hold state.
	 */
	private ThreadLocal<Closeable> threadLocal = new ThreadLocal<Closeable>();

	/** XML document builder. */
	private final DocumentBuilder documentBuilder;

	/** Construct XML argument reader. */
	public XmlArgumentsReader() {
		this.documentBuilder = Classes.loadService(DocumentBuilder.class);
	}

	/**
	 * Handle XML input stream from HTTP request. This method expects a single formal parameter, current implementation
	 * supporting {@link Document} and {@link InputStream}.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return arguments array.
	 * @throws IOException if reading from HTTP request input stream fails.
	 * @throws IllegalArgumentException if formal parameters does not contain exactly one type.
	 * @throws IllegalArgumentException if formal parameter type is parameterized.
	 * @throws IllegalArgumentException if formal parameter type is not supported by this method.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		if (formalParameters.length != 1) {
			throw new IllegalArgumentException("Bad parameters count. Should be exactly one but is |%d|.", formalParameters.length);
		}
		if (formalParameters[0] instanceof ParameterizedType) {
			throw new IllegalArgumentException("Parameterized type |%s| is not supported.", formalParameters[0]);
		}
		return new Object[] { read(httpRequest.getInputStream(), formalParameters[0]) };
	}

	/**
	 * Process XML input stream accordingly requested type. Current implementation supports two type: XML {@link Document} and
	 * XML input stream.
	 * 
	 * @param inputStream XML input stream,
	 * @param type expected Java type.
	 * @return XML stream processing result.
	 * @throws IOException if XML stream reading fails.
	 * @throws IllegalArgumentException if requested Java type is not supported.
	 */
	@Override
	public Object read(InputStream inputStream, Type type) throws IOException {
		if (Types.isKindOf(type, Document.class)) {
			return documentBuilder.loadXML(inputStream);
		} else if (Types.isKindOf(type, InputStream.class)) {
			threadLocal.set(inputStream);
			return inputStream;
		} else {
			throw new IllegalArgumentException("Unsupported formal parameter type |%s| for XML content type.", type);
		}
	}

	/** Close closeable object from current thread storage then remove the storage. */
	@Override
	public void clean() {
		Files.close(threadLocal.get());
		threadLocal.remove();
	}
}
