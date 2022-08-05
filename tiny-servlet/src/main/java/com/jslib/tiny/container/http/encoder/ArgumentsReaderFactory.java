package com.jslib.tiny.container.http.encoder;

import java.lang.reflect.Type;

import jakarta.servlet.http.HttpServletRequest;
import com.jslib.lang.IllegalArgumentException;

/**
 * Factory for invocation arguments readers. This interface implements a strategy that select appropriate arguments reader
 * instance for currently processing HTTP request. It also creates reader instances for multipart mixed messages.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ArgumentsReaderFactory {
	/**
	 * Get invocation arguments reader able to process arguments from HTTP request accordingly expected argument types.
	 * Implementation may create new arguments reader instance or can reuse it from internal pool.
	 * <p>
	 * If HTTP request has no content type, implementation should return {@link EncoderKey#APPLICATION_JSON} reader since tiny
	 * container default content type is JSON. This departs from HTTP standard that mandates using bytes stream when no content
	 * type is provided on request.
	 * <p>
	 * If formal parameters are provided implementation should try to retrieve a reader for expected argument types. If none
	 * found should try using content type solely.
	 * 
	 * @param httpRequest HTTP request carrying arguments to read,
	 * @param formalParameters expected argument types, possible empty.
	 * @return invocation arguments reader, never null.
	 * @throws IllegalArgumentException if no reader found for requested HTTP request and expected argument types.
	 */
	ArgumentsReader getArgumentsReader(HttpServletRequest httpRequest, Type[] formalParameters);

	/**
	 * Get argument reader for entity body of a multipart mixed message. Implementation may create new reader instance or can
	 * reuse it from internal pool. This factory method tries to retrieve a reader for expected argument type. If none found it
	 * tries using content type solely.
	 * <p>
	 * This factory method is designed for framework and extensions usage only. Application should refrain to use it.
	 * 
	 * @param contentType content type of the entity body from a multipart mixed message,
	 * @param parameterType expected Java type.
	 * @return argument reader instance, never null.
	 * @throws IllegalArgumentException if no reader found for requested content and Java types.
	 */
	ArgumentPartReader getArgumentPartReader(String contentType, Type parameterType);
}
