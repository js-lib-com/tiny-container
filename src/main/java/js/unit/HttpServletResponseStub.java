package js.unit;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public abstract class HttpServletResponseStub implements HttpServletResponse {
	@Override
	public void setContentLengthLong(long length) {
		throw new UnsupportedOperationException("setContentLengthLong(long length)");
	}

	@Override
	public String getHeader(String name) {
		throw new UnsupportedOperationException("getHeader(String name)");
	}

	@Override
	public Collection<String> getHeaderNames() {
		throw new UnsupportedOperationException("getHeaderNames()");
	}

	@Override
	public Collection<String> getHeaders(String name) {
		throw new UnsupportedOperationException("getHeaders(String name)");
	}

	@Override
	public int getStatus() {
		throw new UnsupportedOperationException("getStatus()");
	}

	@Override
	public void addCookie(Cookie cookie) {
		throw new UnsupportedOperationException("addCookie(Cookie cookie)");
	}

	@Override
	public void addDateHeader(String name, long timestamp) {
		throw new UnsupportedOperationException("addDateHeader(String name, long timestamp)");
	}

	@Override
	public void addHeader(String name, String value) {
		throw new UnsupportedOperationException("addHeader(String name, String value)");
	}

	@Override
	public void addIntHeader(String name, int value) {
		throw new UnsupportedOperationException("addIntHeader(String name, int value)");
	}

	@Override
	public boolean containsHeader(String name) {
		throw new UnsupportedOperationException("containsHeader(String name)");
	}

	@Override
	public String encodeRedirectURL(String url) {
		throw new UnsupportedOperationException("encodeRedirectURL(String url)");
	}

	@Override
	public String encodeRedirectUrl(String url) {
		throw new UnsupportedOperationException("encodeRedirectUrl(String url)");
	}

	@Override
	public String encodeURL(String url) {
		throw new UnsupportedOperationException("encodeURL(String url)");
	}

	@Override
	public String encodeUrl(String url) {
		throw new UnsupportedOperationException("encodeUrl(String url)");
	}

	@Override
	public void sendError(int sc) throws IOException {
		throw new UnsupportedOperationException("sendError(int sc)");
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		throw new UnsupportedOperationException("sendError(int sc, String msg)");
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		throw new UnsupportedOperationException("sendRedirect(String location)");
	}

	@Override
	public void setDateHeader(String name, long value) {
		throw new UnsupportedOperationException("setDateHeader(String name, long value)");
	}

	@Override
	public void setHeader(String name, String value) {
		throw new UnsupportedOperationException("setHeader(String name, String value)");
	}

	@Override
	public void setIntHeader(String name, int value) {
		throw new UnsupportedOperationException("setIntHeader(String name, int value)");
	}

	@Override
	public void setStatus(int sc) {
		throw new UnsupportedOperationException("setStatus(int sc)");
	}

	@Override
	public void setStatus(int sc, String msg) {
		throw new UnsupportedOperationException("setStatus(int sc, String msg)");
	}

	@Override
	public void flushBuffer() throws IOException {
		throw new UnsupportedOperationException("flushBuffer()");
	}

	@Override
	public int getBufferSize() {
		throw new UnsupportedOperationException("getBufferSize()");
	}

	@Override
	public String getCharacterEncoding() {
		throw new UnsupportedOperationException("getCharacterEncoding()");
	}

	@Override
	public String getContentType() {
		throw new UnsupportedOperationException("getContentType()");
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException("getLocale()");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("getOutputStream()");
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		throw new UnsupportedOperationException("getWriter()");
	}

	@Override
	public boolean isCommitted() {
		throw new UnsupportedOperationException("isCommitted()");
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException("reset()");
	}

	@Override
	public void resetBuffer() {
		throw new UnsupportedOperationException("resetBuffer()");
	}

	@Override
	public void setBufferSize(int size) {
		throw new UnsupportedOperationException("setBufferSize(int size)");
	}

	@Override
	public void setCharacterEncoding(String charset) {
		throw new UnsupportedOperationException("setCharacterEncoding(String charset)");
	}

	@Override
	public void setContentLength(int len) {
		throw new UnsupportedOperationException("setContentLength(int len)");
	}

	@Override
	public void setContentType(String type) {
		throw new UnsupportedOperationException("setContentType(String type)");
	}

	@Override
	public void setLocale(Locale locale) {
		throw new UnsupportedOperationException("setLocale(Locale locale)");
	}
}
