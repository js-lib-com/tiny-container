package js.tiny.container.servlet;

import javax.servlet.ServletContext;

import js.tiny.container.unit.ServletConfigStub;

public class MockServletConfig extends ServletConfigStub {
	public String servletName;
	public ServletContext servletContext;

	@Override
	public String getServletName() {
		return servletName;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}
}
