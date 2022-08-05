package com.jslib.tiny.container.http.encoder;

import java.io.IOException;
import java.lang.reflect.Type;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Special arguments reader for methods with no formal parameters.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class EmptyArgumentsReader implements ArgumentsReader {
	/** Shared empty arguments array. */
	private static final Object[] EMPTY_ARGUMENTS = new Object[] {};

	/** Reusable instance of empty arguments reader. */
	private static EmptyArgumentsReader instance = new EmptyArgumentsReader();

	/**
	 * Get empty arguments reader.
	 * 
	 * @return empty arguments reader singleton.
	 */
	public static EmptyArgumentsReader getInstance() {
		return instance;
	}

	/**
	 * This method is enacted for empty formal parameters. It just return {@link #EMPTY_ARGUMENTS}.
	 * 
	 * @param httpRequest unused HTTP request,
	 * @param formalParameters unused empty formal parameters list.
	 * @return always return {@link #EMPTY_ARGUMENTS}.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		return EMPTY_ARGUMENTS;
	}

	/** This method does nothing but is requested by interface. */
	@Override
	public void clean() {
	}
}
