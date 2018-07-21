package js.servlet.test;

import javax.servlet.ServletContext;

import js.unit.ServletConfigStub;

@SuppressWarnings("unchecked")
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
}
