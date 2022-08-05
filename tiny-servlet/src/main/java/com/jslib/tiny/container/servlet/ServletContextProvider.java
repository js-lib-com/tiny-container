package com.jslib.tiny.container.servlet;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Provider;
import jakarta.servlet.ServletContext;

/**
 * Inject provider for servlet context.
 * 
 * @author Iulian Rotaru
 */
public class ServletContextProvider implements Provider<ServletContext> {
	private ServletContext servletContext;

	public void createContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void destroyContext() {
		this.servletContext = null;
	}

	@Override
	public ServletContext get() {
		if (servletContext == null) {
			throw new ContextNotActiveException("Servlet context not initialized.");
		}
		return servletContext;
	}
}
