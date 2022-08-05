package com.jslib.container.servlet;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Inject provider for HTTP servlet request.
 * 
 * @author Iulian Rotaru
 */
public class HttpRequestProvider implements Provider<HttpServletRequest> {
	private static final ThreadLocal<HttpServletRequest> httpRequest = new ThreadLocal<>();

	public static void createContext(HttpServletRequest httpRequest) {
		HttpRequestProvider.httpRequest.set(httpRequest);
	}

	public static void destroyContext(HttpServletRequest httpRequest) {
		HttpRequestProvider.httpRequest.remove();
	}

	@Override
	public HttpServletRequest get() {
		HttpServletRequest httpRequest = HttpRequestProvider.httpRequest.get();
		if (httpRequest == null) {
			throw new ContextNotActiveException("Attempt to use servlet request instance outside HTTP request thread.");
		}
		return httpRequest;
	}
}
