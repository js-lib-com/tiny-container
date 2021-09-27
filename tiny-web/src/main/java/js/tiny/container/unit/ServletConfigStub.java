package js.tiny.container.unit;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class ServletConfigStub implements ServletConfig {
	@Override
	public String getInitParameter(String arg0) {
		throw new UnsupportedOperationException("getInitParameter(String)");
	}

	@Override
	public Enumeration getInitParameterNames() {
		throw new UnsupportedOperationException("getInitParameterNames()");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("getServletContext()");
	}

	@Override
	public String getServletName() {
		throw new UnsupportedOperationException("getServletName()");
	}
}
