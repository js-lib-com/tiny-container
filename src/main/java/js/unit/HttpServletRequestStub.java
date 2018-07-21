package js.unit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class HttpServletRequestStub implements HttpServletRequest {
	@Override
	public long getContentLengthLong() {
		throw new UnsupportedOperationException("getContentLengthLong()");
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("changeSessionId()");
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> type) throws IOException, ServletException {
		throw new UnsupportedOperationException("upgrade(Class<T> type)");
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new UnsupportedOperationException("getAsyncContext()");
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new UnsupportedOperationException("getDispatcherType()");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("getServletContext()");
	}

	@Override
	public boolean isAsyncStarted() {
		throw new UnsupportedOperationException("isAsyncStarted()");
	}

	@Override
	public boolean isAsyncSupported() {
		throw new UnsupportedOperationException("isAsyncSupported()");
	}

	@Override
	public AsyncContext startAsync() {
		throw new UnsupportedOperationException("startAsync()");
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
		throw new UnsupportedOperationException("startAsync(ServletRequest servletRequest, ServletResponse servletResponse)");
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException("authenticate(HttpServletResponse response)");
	}

	@Override
	public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
		throw new UnsupportedOperationException("getPart(String name)");
	}

	@Override
	public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
		throw new UnsupportedOperationException("getParts()");
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException("login(String username, String password)");
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException("logout()");
	}

	@Override
	public String getAuthType() {
		throw new UnsupportedOperationException("getAuthType()");
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException("getContextPath()");
	}

	@Override
	public Cookie[] getCookies() {
		throw new UnsupportedOperationException("getCookies()");
	}

	@Override
	public long getDateHeader(String name) {
		throw new UnsupportedOperationException("getDateHeader(String name)");
	}

	@Override
	public String getHeader(String name) {
		throw new UnsupportedOperationException("getHeader(String name)");
	}

	@Override
	public Enumeration getHeaderNames() {
		throw new UnsupportedOperationException("getHeaderNames()");
	}

	@Override
	public Enumeration getHeaders(String name) {
		throw new UnsupportedOperationException("getHeaders(String name)");
	}

	@Override
	public int getIntHeader(String name) {
		throw new UnsupportedOperationException("getIntHeader(String name)");
	}

	@Override
	public String getMethod() {
		throw new UnsupportedOperationException("getMethod()");
	}

	@Override
	public String getPathInfo() {
		throw new UnsupportedOperationException("getPathInfo()");
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException("getPathTranslated()");
	}

	@Override
	public String getQueryString() {
		throw new UnsupportedOperationException("getQueryString()");
	}

	@Override
	public String getRemoteUser() {
		throw new UnsupportedOperationException("getRemoteUser()");
	}

	@Override
	public String getRequestURI() {
		throw new UnsupportedOperationException("getRequestURI()");
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new UnsupportedOperationException("getRequestURL()");
	}

	@Override
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException("getRequestedSessionId()");
	}

	@Override
	public String getServletPath() {
		throw new UnsupportedOperationException("getServletPath()");
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("getSession()");
	}

	@Override
	public HttpSession getSession(boolean create) {
		throw new UnsupportedOperationException("getSession(boolean create)");
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UnsupportedOperationException("getUserPrincipal()");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException("isRequestedSessionIdFromCookie()");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException("isRequestedSessionIdFromURL()");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new UnsupportedOperationException("isRequestedSessionIdFromUrl()");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException("isRequestedSessionIdValid()");
	}

	@Override
	public boolean isUserInRole(String role) {
		throw new UnsupportedOperationException("isUserInRole(String role)");
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
	public String getCharacterEncoding() {
		throw new UnsupportedOperationException("getCharacterEncoding()");
	}

	@Override
	public int getContentLength() {
		throw new UnsupportedOperationException("getContentLength()");
	}

	@Override
	public String getContentType() {
		throw new UnsupportedOperationException("getContentType()");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException("getInputStream()");
	}

	@Override
	public String getLocalAddr() {
		throw new UnsupportedOperationException("getLocalAddr()");
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException("getLocalName()");
	}

	@Override
	public int getLocalPort() {
		throw new UnsupportedOperationException("getLocalPort()");
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException("getLocale()");
	}

	@Override
	public Enumeration getLocales() {
		throw new UnsupportedOperationException("getLocales()");
	}

	@Override
	public String getParameter(String name) {
		throw new UnsupportedOperationException("getParameter(String name)");
	}

	@Override
	public Map getParameterMap() {
		throw new UnsupportedOperationException("getParameterMap()");
	}

	@Override
	public Enumeration getParameterNames() {
		throw new UnsupportedOperationException("getParameterNames()");
	}

	@Override
	public String[] getParameterValues(String name) {
		throw new UnsupportedOperationException("getParameterValues(String name)");
	}

	@Override
	public String getProtocol() {
		throw new UnsupportedOperationException("getProtocol()");
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException("getReader()");
	}

	@Override
	public String getRealPath(String resource) {
		throw new UnsupportedOperationException("getRealPath(String resource)");
	}

	@Override
	public String getRemoteAddr() {
		throw new UnsupportedOperationException("getRemoteAddr()");
	}

	@Override
	public String getRemoteHost() {
		throw new UnsupportedOperationException("getRemoteHost()");
	}

	@Override
	public int getRemotePort() {
		throw new UnsupportedOperationException("getRemotePort()");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException("getRequestDispatcher(String path)");
	}

	@Override
	public String getScheme() {
		throw new UnsupportedOperationException("getScheme()");
	}

	@Override
	public String getServerName() {
		throw new UnsupportedOperationException("getServerName()");
	}

	@Override
	public int getServerPort() {
		throw new UnsupportedOperationException("getServerPort()");
	}

	@Override
	public boolean isSecure() {
		throw new UnsupportedOperationException("isSecure()");
	}

	@Override
	public void removeAttribute(String name) {
		throw new UnsupportedOperationException("removeAttribute(String name)");
	}

	@Override
	public void setAttribute(String name, Object value) {
		throw new UnsupportedOperationException("setAttribute(String name, Object value)");
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		throw new UnsupportedOperationException("setCharacterEncoding(String env)");
	}
}
