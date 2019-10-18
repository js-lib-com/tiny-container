package js.tiny.container.unit;

import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

public class FilterConfigStub implements FilterConfig {
	@Override
	public String getFilterName() {
		throw new UnsupportedOperationException("getFilterName()");
	}

	@Override
	public String getInitParameter(String name) {
		throw new UnsupportedOperationException("getInitParameter(String)");
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		throw new UnsupportedOperationException("getInitParameterNames()");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("getServletContext()");
	}
}
