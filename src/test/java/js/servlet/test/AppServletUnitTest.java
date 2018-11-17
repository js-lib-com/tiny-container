package js.servlet.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.UnavailableException;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import js.container.ContainerSPI;
import js.core.App;
import js.core.Factory;
import js.lang.InvocationException;
import js.rmi.BusinessException;
import js.servlet.AppServlet;
import js.servlet.RequestContext;
import js.servlet.TinyContainer;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.unit.ServletContextStub;
import js.unit.TestContext;
import js.util.Classes;
import js.util.Files;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings({ "unused", "serial" })
public class AppServletUnitTest {
	private static final String DESCRIPTOR = "" + //
			"<?xml version='1.0' encoding='UTF-8'?>" + //
			"<test>" + //
			"	<managed-classes>" + //
			"		<controller interface='js.servlet.RequestContext' class='js.servlet.test.AppServletUnitTest$MockRequestContext' />" + //
			"	</managed-classes>" + //
			"	<login>" + //
			"		<property name='realm' value='Test App' />" + //
			"		<property name='page' value='login.xsp' />" + //
			"	</login>" + //
			"</test>";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private ContainerSPI container;
	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;
	private MockRequestContext context;

	@Before
	public void beforeTest() throws Exception {
		container = (ContainerSPI) TestContext.start(DESCRIPTOR);
		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();
		context = (MockRequestContext) Factory.getInstance(RequestContext.class);
	}

	@Test
	public void init() throws UnavailableException {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
			}
		};

		MockServletContext context = new MockServletContext();
		context.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		MockServletConfig config = new MockServletConfig();
		config.servletName = "ServletName";
		config.servletContext = context;

		servlet.init(config);

		assertEquals("test-app#ServletName", Classes.getFieldValue(servlet, AppServlet.class, "servletName"));
		assertNotNull(Classes.getFieldValue(servlet, AppServlet.class, "container"));
		assertEquals(container, Classes.getFieldValue(servlet, AppServlet.class, "container"));
	}

	@Test(expected = UnavailableException.class)
	public void init_NullApp() throws UnavailableException {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
			}
		};

		MockServletContext context = new MockServletContext();
		MockServletConfig config = new MockServletConfig();
		config.servletName = "ServletName";
		config.servletContext = context;

		servlet.init(config);
	}

	@Test
	public void destroy() {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
			}
		};
		servlet.destroy();
	}

	@Test
	public void service() throws Exception {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
				TestCase.assertEquals(httpRequest, context.getRequest());
				TestCase.assertEquals(httpResponse, context.getResponse());
				TestCase.assertEquals("test/service", context.getRequest().getRequestURI());
				TestCase.assertNotNull(container);
			}
		};
		exerciseService(servlet);

		assertEquals(1, context.attachProbe);
		assertEquals(1, context.detachProbe);
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void service_EmptyUriRequest() throws Exception {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
				TestCase.assertEquals(httpRequest, context.getRequest());
				TestCase.assertEquals(httpResponse, context.getResponse());
				TestCase.assertEquals("test/service", context.getRequest().getRequestURI());
				TestCase.assertNotNull(container);
			}
		};

		httpRequest.requestURI = "/test/service";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "/test/service");

		exerciseService(servlet);

		assertEquals(0, context.attachProbe);
		assertEquals(0, context.detachProbe);
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void serviceIOException() throws Exception {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
				throw new IOException("test exception");
			}
		};
		try {
			exerciseService(servlet);
		} catch (IOException e) {
			assertEquals("test exception", e.getMessage());
		}

		assertEquals(1, context.attachProbe);
		assertEquals(1, context.detachProbe);
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void service_Throwable() throws Exception {
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext context) throws IOException, ServletException {
				throw new IllegalArgumentException("test exception");
			}
		};
		try {
			exerciseService(servlet);
		} catch (IllegalArgumentException e) {
			assertEquals("test exception", e.getMessage());
		}

		assertEquals(1, context.attachProbe);
		assertEquals(1, context.detachProbe);
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void emptyUriRequest() throws Exception {
		httpRequest.method = "POST";
		assertFalse(isEmptyUriRequest(httpRequest));

		httpRequest.requestURI = "/test/query";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "test/service");
		assertFalse(isEmptyUriRequest(httpRequest));

		httpRequest.requestURI = "/test/service";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "/test/service");
		assertTrue(isEmptyUriRequest(httpRequest));

		httpRequest.requestURI = "/test/service?qqq";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "/test/service?qqq");
		assertTrue(isEmptyUriRequest(httpRequest));

		httpRequest.requestURI = "/test/service?qqq";
		httpRequest.queryString = "qqq";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "/test/service?qqq");
		assertTrue(isEmptyUriRequest(httpRequest));
		httpRequest.queryString = null;

		httpRequest.requestURI = "/test/service?qqq";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", null);
		assertFalse(isEmptyUriRequest(httpRequest));

		httpRequest.requestURI = "/test/service";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "/test/service");
		httpRequest.headers.put("Accept", "image/jpg");
		assertTrue(isEmptyUriRequest(httpRequest));

		httpRequest.requestURI = "/test/service";
		httpRequest.method = "GET";
		httpRequest.headers.put("Referer", "/test/service");
		httpRequest.headers.put("Accept", "text/html");
		assertFalse(isEmptyUriRequest(httpRequest));
	}

	private static boolean isEmptyUriRequest(HttpServletRequest httpRequest) throws Exception {
		return Classes.invoke(AppServlet.class, "isEmptyUriRequest", httpRequest);
	}

	/** For non XHR request unauthorized access send 401 and WWW-Authenticate set to basic. */
	@Test
	public void sendUnauthorized() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendUnauthorized", context);

		assertEquals(401, httpResponse.status);
		assertEquals("Basic realm=Test App", httpResponse.headers.get("WWW-Authenticate"));
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void sendUnauthorized_ResponseCommited() throws Exception {
		httpRequest.requestURI = "/test/service";
		Files.copy(new StringReader("response"), httpResponse.stringWriter);
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendUnauthorized", context);

		assertEquals(0, httpResponse.status);
		assertNull(httpResponse.headers.get("WWW-Authenticate"));
		assertEquals(0, context.dumpProbe);
		assertEquals("response", httpResponse.getBody());
	}

	/**
	 * For XHR request on application with login page unauthorized access send 200 and custom header X-JSLIB-Location set to
	 * login page.
	 */
	@Test
	public void sendXhrUnauthorized() throws Exception {
		httpRequest.requestURI = "/test/service";
		httpRequest.headers.put("X-Requested-With", "XMLHttpRequest");
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendUnauthorized", context);

		assertEquals(200, httpResponse.status);
		assertEquals("/test-app/login.xsp", httpResponse.headers.get("X-JSLIB-Location"));
		assertEquals(0, context.dumpProbe);
	}

	/** For XHR request on application without login page unauthorized access send 401 and WWW-Authenticate set to basic. */
	@Test
	public void sendXhrUnauthorized_NoLoginPage() throws Exception {
		Classes.setFieldValue(container, TinyContainer.class, "loginPage", null);
		httpRequest.requestURI = "/test/service";
		httpRequest.headers.put("X-Requested-With", "XMLHttpRequest");
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendUnauthorized", context);

		assertEquals(401, httpResponse.status);
		assertEquals("Basic realm=Test App", httpResponse.headers.get("WWW-Authenticate"));
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void sendNotFound() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendNotFound", context, new NoSuchMethodException("js.test.Class#method"));

		assertEquals(404, httpResponse.status);
		assertEquals("{\"cause\":\"java.lang.NoSuchMethodException\",\"message\":\"js.test.Class#method\"}", httpResponse.getBody());
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void sendError() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendError", context, new IOException("test exception"));

		assertEquals(500, httpResponse.status);
		assertEquals("{\"cause\":\"java.io.IOException\",\"message\":\"test exception\"}", httpResponse.getBody());
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void sendError_InvocationException() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendError", context, new InvocationException(new IOException("test exception")));

		assertEquals(500, httpResponse.status);
		assertEquals("{\"cause\":\"java.io.IOException\",\"message\":\"test exception\"}", httpResponse.getBody());
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void sendError_InvocationException_NullCause() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendError", context, new InvocationException(new Exception("exception")));

		assertEquals(500, httpResponse.status);
		assertEquals("{\"cause\":\"java.lang.Exception\",\"message\":\"exception\"}", httpResponse.getBody());
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void sendError_InvocationTargetException() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendError", context, new InvocationTargetException(new IOException("test exception")));

		assertEquals(500, httpResponse.status);
		assertEquals("{\"cause\":\"java.io.IOException\",\"message\":\"test exception\"}", httpResponse.getBody());
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void sendError_ResponseCommited() throws Exception {
		httpRequest.requestURI = "/test/service";
		Files.copy(new StringReader("response"), httpResponse.stringWriter);
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendError", context, new IOException("test exception"));

		assertEquals(0, httpResponse.status);
		assertEquals("response", httpResponse.getBody());
		assertEquals(1, context.dumpProbe);
	}

	@Test
	public void sendBusinessConstrain() throws Exception {
		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendError", context, new BusinessException(0x1964));

		assertEquals(400, httpResponse.status);
		assertEquals("{\"errorCode\":6500}", httpResponse.getBody());
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void sendJsonObject() throws Exception {
		class Message {
			private String text = "message text";
		}

		httpRequest.requestURI = "/test/service";
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendJsonObject", context, new Message(), 200);

		assertEquals(200, httpResponse.status);
		assertEquals(23, httpResponse.contentLegth);
		assertEquals("application/json", httpResponse.getContentType());
		assertEquals("en-US", httpResponse.getHeader("Content-Language"));
		assertEquals("{\"text\":\"message text\"}", httpResponse.getBody());
		assertEquals(0, context.dumpProbe);
	}

	@Test
	public void sendJsonObject_ResponseCommited() throws Exception {
		httpRequest.requestURI = "/test/service";
		Files.copy(new StringReader("response"), httpResponse.stringWriter);
		context.attach(httpRequest, httpResponse);
		Classes.invoke(AppServlet.class, "sendJsonObject", context, new Object(), 200);

		assertEquals(0, httpResponse.status);
		assertEquals(0, httpResponse.contentLegth);
		assertNull(httpResponse.getContentType());
		assertNull(httpResponse.getHeader("Content-Language"));
		assertEquals("response", httpResponse.getBody());
		assertEquals(0, context.dumpProbe);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void exerciseService(AppServlet servlet) throws ServletException, IOException {
		MockServletContext context = new MockServletContext();
		context.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		MockServletConfig config = new MockServletConfig();
		config.servletName = "ServletName";
		config.servletContext = context;

		servlet.init(config);

		if (httpRequest.method == null) {
			httpRequest.method = "POST";
		}
		if (httpRequest.requestURI == null) {
			httpRequest.requestURI = "test/service";
		}
		servlet.service(httpRequest, httpResponse);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private HttpServletResponse httpResponse;
		private ServletContext servletContext;

		private String method;
		private String requestURI;
		private String queryString;
		private Map<String, String> headers = new HashMap<>();
		private HttpSession session;
		private String content;

		@Override
		public String getRemoteHost() {
			return "localhost";
		}

		@Override
		public String getMethod() {
			return method;
		}

		@Override
		public String getHeader(String name) {
			return headers.get(name);
		}

		@Override
		public String getQueryString() {
			if (queryString != null) {
				return queryString;
			}
			if (requestURI == null) {
				return null;
			}
			int beginIndex = requestURI.lastIndexOf('?');
			if (beginIndex == -1) {
				return null;
			}
			return requestURI.substring(beginIndex);
		}

		@Override
		public String getContextPath() {
			if (requestURI == null) {
				return null;
			}
			int beginIndex = 0;
			if (requestURI.startsWith("/")) {
				beginIndex++;
			}
			int endIndex = requestURI.indexOf("/", beginIndex);
			if (endIndex == -1) {
				return "";
			}
			return requestURI.substring(0, endIndex);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return new Headers(headers.keySet());
		}

		@Override
		public String getRequestURI() {
			if (requestURI == null) {
				return null;
			}
			int index = requestURI.lastIndexOf('?');
			if (index == -1) {
				index = requestURI.length();
			}
			return requestURI.substring(0, index);
		}

		@Override
		public Locale getLocale() {
			return Locale.getDefault();
		}

		@Override
		public Cookie[] getCookies() {
			return null;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private int status;
		private int contentLegth;
		private Map<String, String> headers = new HashMap<>();
		private StringWriter stringWriter = new StringWriter();

		public String getBody() {
			return stringWriter.toString();
		}

		@Override
		public boolean isCommitted() {
			return stringWriter.getBuffer().length() > 0;
		}

		@Override
		public void setStatus(int status) {
			this.status = status;
		}

		@Override
		public void setContentType(String contentType) {
			headers.put("Content-Type", contentType);
		}

		@Override
		public void setContentLength(int contentLegth) {
			this.contentLegth = contentLegth;
		}

		@Override
		public void setHeader(String header, String value) {
			headers.put(header, value);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return new OutputStream(stringWriter);
		}

		@Override
		public String getHeader(String header) {
			return headers.get(header);
		}

		@Override
		public String getContentType() {
			return headers.get("Content-Type");
		}
	}

	private static class MockRequestContext extends RequestContext {
		private int attachProbe;
		private int detachProbe;
		private int dumpProbe;

		public MockRequestContext(ContainerSPI container) {
			super(container);
		}

		@Override
		public void attach(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
			super.attach(httpRequest, httpResponse);
			++attachProbe;
		}

		@Override
		public void detach() {
			super.detach();
			++detachProbe;
		}

		@Override
		public void dump() {
			super.dump();
			++dumpProbe;
		}
	}

	private static class MockServletContext extends ServletContextStub {
		private Map<String, Object> attributes = new HashMap<String, Object>();

		@Override
		public String getServletContextName() {
			return "test-app";
		}

		@Override
		public String getRealPath(String resource) {
			return ".";
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}
	}

	private static class Headers implements Enumeration<String> {
		private Iterator<String> headerNames;

		public Headers(Collection<String> headerNames) {
			this.headerNames = headerNames.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return headerNames.hasNext();
		}

		@Override
		public String nextElement() {
			return headerNames.next();
		}
	}

	private static class OutputStream extends ServletOutputStream {
		private StringWriter stringWriter;

		public OutputStream(StringWriter stringWriter) {
			this.stringWriter = stringWriter;
		}

		@Override
		public void write(int b) throws IOException {
			stringWriter.append((char) b);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}
	}
}
