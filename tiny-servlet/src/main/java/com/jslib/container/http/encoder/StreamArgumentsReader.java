package com.jslib.container.http.encoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.servlet.http.HttpServletRequest;
import com.jslib.lang.IllegalArgumentException;
import com.jslib.util.Files;

/**
 * Provide application access to HTTP request input stream. This arguments reader mitigates application access to HTTP request
 * input stream via a Java type supported by {@link StreamFactory}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class StreamArgumentsReader implements ArgumentsReader, ArgumentPartReader {
	/**
	 * Cache closeable object till HTTP request is processed by application logic, in order to close it after. See
	 * {@link #clean()}. Uses thread local storage since arguments reader instance is reusable and cannot hold state.
	 */
	private final ThreadLocal<Closeable> threadLocal = new ThreadLocal<Closeable>();

	/**
	 * Provides application access to HTTP request input stream. Formal parameters should have only one type that is supported
	 * by {@link StreamFactory} otherwise illegal argument exception is thrown.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return arguments array.
	 * @throws IOException if opening HTTP request input stream fails.
	 * @throws IllegalArgumentException if formal parameters does not contain exactly one type.
	 * @throws IllegalArgumentException if formal parameter type is parameterized.
	 * @throws IllegalArgumentException if formal parameter type is not supported by {@link StreamFactory}.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		if (formalParameters.length != 1) {
			throw new IllegalArgumentException("Bad parameters count for multipart form. Method must have exactly one formal parameter but has |%d|.", formalParameters.length);
		}
		if (formalParameters[0] instanceof ParameterizedType) {
			throw new IllegalArgumentException("Current implementation does not support parameterized types |%s|.", formalParameters[0]);
		}
		return new Object[] { read(httpRequest.getInputStream(), formalParameters[0]) };
	}

	/**
	 * This method is a this wrapper for {@link StreamFactory#getInstance(InputStream, Type)}.
	 * 
	 * @param inputStream input stream,
	 * @param type expected type.
	 * @return stream instance accordingly requested type.
	 * @throws IOException if newly stream creation fails.
	 * @throws IllegalArgumentException if there requested type is not supported.
	 */
	@Override
	public Object read(InputStream inputStream, Type type) throws IOException {
		Closeable closeable = StreamFactory.getInstance(inputStream, type);
		threadLocal.set(closeable);
		return closeable;
	}

	/** Close closeable object from current thread storage then remove the storage. */
	@Override
	public void clean() {
		Files.close(threadLocal.get());
		threadLocal.remove();
	}
}
