package js.tiny.container.unit;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class ServletContextStub implements ServletContext {
	@Override
	public String getVirtualServerName() {
		throw new UnsupportedOperationException("getVirtualServerName()");
	}

	@Override
	public Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		throw new UnsupportedOperationException("addFilter(String filterName, Class<? extends Filter> filterClass)");
	}

	@Override
	public Dynamic addFilter(String filterName, Filter filter) {
		throw new UnsupportedOperationException("addFilter(String filterName, Filter filter)");
	}

	@Override
	public Dynamic addFilter(String filterName, String className) {
		throw new UnsupportedOperationException("addFilter(String filterName, String className)");
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		throw new UnsupportedOperationException("addListener(Class<? extends EventListener> listenerClass)");
	}

	@Override
	public void addListener(String className) {
		throw new UnsupportedOperationException("addListener(String className)");
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		throw new UnsupportedOperationException("addListener(T t)");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		throw new UnsupportedOperationException("addServlet(String servletName, Class<? extends Servlet> servletClass)");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
		throw new UnsupportedOperationException("addServlet(String servletName, Servlet servlet)");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, String className) {
		throw new UnsupportedOperationException("addServlet(String servletName, String className)");
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("createFilter(Class<T> clazz)");
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("createListener(Class<T> clazz)");
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("createServlet(Class<T> clazz)");
	}

	@Override
	public void declareRoles(String... roleNames) {
		throw new UnsupportedOperationException("declareRoles(String... roleNames)");
	}

	@Override
	public ClassLoader getClassLoader() {
		throw new UnsupportedOperationException("getClassLoader()");
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException("getContextPath()");
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		throw new UnsupportedOperationException("getDefaultSessionTrackingModes()");
	}

	@Override
	public int getEffectiveMajorVersion() {
		throw new UnsupportedOperationException("getEffectiveMajorVersion()");
	}

	@Override
	public int getEffectiveMinorVersion() {
		throw new UnsupportedOperationException("getEffectiveMinorVersion()");
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		throw new UnsupportedOperationException("getEffectiveSessionTrackingModes()");
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		throw new UnsupportedOperationException("getFilterRegistration(String filterName)");
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		throw new UnsupportedOperationException("getFilterRegistrations()");
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new UnsupportedOperationException("getJspConfigDescriptor()");
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		throw new UnsupportedOperationException("getServletRegistration(String servletName)");
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		throw new UnsupportedOperationException("getServletRegistrations()");
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		throw new UnsupportedOperationException("getSessionCookieConfig()");
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		throw new UnsupportedOperationException("setInitParameter(String name, String value)");
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) throws IllegalStateException, IllegalArgumentException {
		throw new UnsupportedOperationException("setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes)");
	}

	@Override
	public Object getAttribute(String name) {
		throw new UnsupportedOperationException("getAttribute(String name)");
	}

	@Override
	public Enumeration getAttributeNames() {
		throw new UnsupportedOperationException("getAttributeNames()");
	}

	@Override
	public ServletContext getContext(String uripath) {
		throw new UnsupportedOperationException("getContext(String uripath)");
	}

	@Override
	public String getInitParameter(String name) {
		throw new UnsupportedOperationException("getInitParameter(String name)");
	}

	@Override
	public Enumeration getInitParameterNames() {
		throw new UnsupportedOperationException("getInitParameterNames()");
	}

	@Override
	public int getMajorVersion() {
		throw new UnsupportedOperationException("getMajorVersion()");
	}

	@Override
	public String getMimeType(String file) {
		throw new UnsupportedOperationException("getMimeType(String file)");
	}

	@Override
	public int getMinorVersion() {
		throw new UnsupportedOperationException("getMinorVersion()");
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		throw new UnsupportedOperationException("getNamedDispatcher(String name)");
	}

	@Override
	public String getRealPath(String resource) {
		throw new UnsupportedOperationException("getRealPath(String resource)");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException("getRequestDispatcher(String path)");
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		throw new UnsupportedOperationException("getResource(String path)");
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		throw new UnsupportedOperationException("getResourceAsStream(String path)");
	}

	@Override
	public Set getResourcePaths(String path) {
		throw new UnsupportedOperationException("getResourcePaths(String path)");
	}

	@Override
	public String getServerInfo() {
		throw new UnsupportedOperationException("getServerInfo()");
	}

	@Override
	public Servlet getServlet(String name) throws ServletException {
		throw new UnsupportedOperationException("getServlet(String name)");
	}

	@Override
	public String getServletContextName() {
		throw new UnsupportedOperationException("getServletContextName()");
	}

	@Override
	public Enumeration getServletNames() {
		throw new UnsupportedOperationException("getServletNames()");
	}

	@Override
	public Enumeration getServlets() {
		throw new UnsupportedOperationException("getServlets()");
	}

	@Override
	public void log(String msg) {
		throw new UnsupportedOperationException("log(String msg)");
	}

	@Override
	public void log(Exception exception, String msg) {
		throw new UnsupportedOperationException("log(Exception exception, String msg)");
	}

	@Override
	public void log(String msg, Throwable throwable) {
		throw new UnsupportedOperationException("log(String msg, Throwable throwable)");
	}

	@Override
	public void removeAttribute(String name) {
		throw new UnsupportedOperationException("removeAttribute(String name)");
	}

	@Override
	public void setAttribute(String name, Object value) {
		throw new UnsupportedOperationException("setAttribute(String name, Object value)");
	}
}
