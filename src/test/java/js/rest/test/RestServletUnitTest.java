package js.rest.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.UnavailableException;
import javax.servlet.WriteListener;

import js.container.AuthorizationException;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.http.Resource;
import js.lang.InvocationException;
import js.rest.RestServlet;
import js.servlet.AppServlet;
import js.servlet.RequestContext;
import js.servlet.TinyContainer;
import js.test.stub.ContainerStub;
import js.test.stub.ManagedClassSpiStub;
import js.test.stub.ManagedMethodSpiStub;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.unit.ServletConfigStub;
import js.unit.ServletContextStub;
import js.util.Classes;
import js.util.Types;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class RestServletUnitTest {
	private MockContainer container;
	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;

	private RequestContext context;
	private RestServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		container = new MockContainer();
		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();

		context = new RequestContext(container);

		servlet = new RestServlet();
		Classes.setFieldValue(servlet, AppServlet.class, "container", container);
	}

	@Test
	public void constructor() {
		RestServlet servlet = new RestServlet();
		assertNotNull(Classes.getFieldValue(servlet, "argumentsReaderFactory"));
		assertNotNull(Classes.getFieldValue(servlet, "valueWriterFactory"));
	}

	@Test
	public void init() throws UnavailableException {
		container.methods.add(new MockManagedMethod("user", void.class, true));
		container.methods.add(new MockManagedMethod("customer", void.class, false));
		container.methods.add(new MockManagedMethod("index", Resource.class, true));

		MockServletConfig config = new MockServletConfig();
		config.context.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		RestServlet servlet = new RestServlet();
		servlet.init(config);

		Map<String, ManagedMethodSPI> methods = Classes.getFieldValue(servlet, "restMethods");
		assertNotNull(methods);
		assertEquals(1, methods.size());
		assertNotNull(methods.get("/resource/user"));
		assertEquals("user", methods.get("/resource/user").getRequestPath());
	}

	@Test(expected = UnavailableException.class)
	public void init_NoContainer() throws UnavailableException {
		MockServletConfig config = new MockServletConfig();
		RestServlet servlet = new RestServlet();
		servlet.init(config);
	}

	@Test
	public void storageKey() throws Exception {
		MockManagedMethod resourceMethod = new MockManagedMethod("sub-resource", Resource.class);
		assertEquals("/resource/sub-resource", key(resourceMethod));

		resourceMethod.declaringClass.requestPath = null;
		assertEquals("/sub-resource", key(resourceMethod));
	}

	@Test
	public void retrievalKey() throws Exception {
		assertEquals("/resource/sub-resource", key("/resource/sub-resource?query"));
		assertEquals("/resource/sub-resource", key("/resource/sub-resource?"));
		assertEquals("/resource/sub-resource", key("/resource/sub-resource"));

		assertEquals("/resource/sub-resource", key("/resource/sub-resource.ext?query"));

		assertEquals("/sub-resource", key("/sub-resource?query"));
		assertEquals("/sub-resource", key("/sub-resource?"));
		assertEquals("/sub-resource", key("/sub-resource"));
	}

	@Test
	public void handleRequest() throws Exception {
		MockManagedMethod method = new MockManagedMethod("sub-resource", String.class, true);
		container.methods.add(method);
		executeRequestHandler();

		assertEquals(200, httpResponse.statusCode);
		assertEquals(1, httpResponse.headers.size());
		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
		assertEquals("\"string value\"", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void handleRequest_Void() throws Exception {
		MockManagedMethod method = new MockManagedMethod("sub-resource", void.class, true);
		container.methods.add(method);
		executeRequestHandler();

		assertEquals(204, httpResponse.statusCode);
		assertTrue(httpResponse.headers.isEmpty());
		assertEquals("", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void handleRequest_Authorization() throws Exception {
		MockManagedMethod method = new MockManagedMethod("sub-resource", void.class, true);
		method.exception = new AuthorizationException();
		container.methods.add(method);
		executeRequestHandler();

		assertEquals(401, httpResponse.statusCode);
		assertEquals(1, httpResponse.headers.size());
		assertEquals("Basic realm=app-test", httpResponse.headers.get("WWW-Authenticate"));
		assertEquals("", httpResponse.outputStream.buffer.toString());
	}

	public void handleRequest_MissingMethod() throws Exception {
		httpRequest.pathInfo = "/resource/missing-resource";
		executeRequestHandler();

		assertEquals(404, httpResponse.statusCode);
		assertEquals(3, httpResponse.headers.size());
		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
		assertEquals("en-US", httpResponse.headers.get("Content-Language"));
		assertEquals("58", httpResponse.headers.get("Content-Length"));
		assertEquals("{\"cause\":\"java.lang.NoSuchMethodException\",\"message\":null}", httpResponse.outputStream.buffer.toString());
	}

	/** For now illegal argument exception is considered bad request 400. */
	@Test
	public void handleRequest_IllegalArgumentException() throws Exception {
		MockManagedMethod method = new MockManagedMethod("sub-resource", void.class, true);
		method.exception = new IllegalArgumentException();
		container.methods.add(method);
		executeRequestHandler();

		assertEquals(400, httpResponse.statusCode);
		assertEquals(0, httpResponse.headers.size());
		assertEquals("/test-app/rest/sub-resource", httpResponse.outputStream.buffer.toString());
	}

	public void handleRequest_InvocationException() throws Exception {
		MockManagedMethod method = new MockManagedMethod("sub-resource", void.class, true);
		method.exception = new InvocationException(new Exception("exception"));
		container.methods.add(method);
		executeRequestHandler();

		assertEquals(500, httpResponse.statusCode);
		assertEquals(3, httpResponse.headers.size());
		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
		assertEquals("53", httpResponse.headers.get("Content-Length"));
		assertEquals("en-US", httpResponse.headers.get("Content-Language"));
		assertEquals("{\"cause\":\"java.lang.Exception\",\"message\":\"exception\"}", httpResponse.outputStream.buffer.toString());
	}

	@Test(expected = RuntimeException.class)
	public void handleRequest_RuntimeException() throws Exception {
		MockManagedMethod method = new MockManagedMethod("sub-resource", void.class, true);
		method.exception = new RuntimeException();
		container.methods.add(method);
		executeRequestHandler();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void executeRequestHandler() throws Exception {
		MockServletConfig config = new MockServletConfig();
		config.context.attributes.put(TinyContainer.ATTR_INSTANCE, container);
		servlet.init(config);

		context.attach(httpRequest, httpResponse);
		Classes.invoke(servlet, "handleRequest", context);
	}

	private static String key(ManagedMethodSPI resourceMethod) throws Exception {
		return Classes.invoke(RestServlet.class, "key", resourceMethod);
	}

	private static String key(String requestPath) throws Exception {
		return Classes.invoke(RestServlet.class, "key", requestPath);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends ContainerStub {
		private List<ManagedMethodSPI> methods = new ArrayList<>();

		@Override
		public Iterable<ManagedMethodSPI> getManagedMethods() {
			return methods;
		}

		@Override
		public <T> T getInstance(ManagedClassSPI managedClass, Object... args) {
			return (T) new Object();
		}

		@Override
		public String getLoginRealm() {
			return "app-test";
		}

		@Override
		public String getLoginPage() {
			return "login-page.htm";
		}
	}

	private static class MockManagedClass extends ManagedClassSpiStub {
		private String requestPath = "resource";

		@Override
		public String getKey() {
			return "1";
		}

		@Override
		public String getRequestPath() {
			return requestPath;
		}
	}

	private static class MockManagedMethod extends ManagedMethodSpiStub {
		private MockManagedClass declaringClass = new MockManagedClass();
		private String requestPath;
		private Class<?> returnType;
		private boolean remotelyAccessible;
		private Exception exception;

		public MockManagedMethod(String requestPath, Class<?> returnType) {
			this(requestPath, returnType, true);
		}

		public MockManagedMethod(String requestPath, Class<?> returnType, boolean remotelyAccessible) {
			this.requestPath = requestPath;
			this.returnType = returnType;
			this.remotelyAccessible = remotelyAccessible;
		}

		@Override
		public ManagedClassSPI getDeclaringClass() {
			return declaringClass;
		}

		@Override
		public String getRequestPath() {
			return requestPath;
		}

		@Override
		public Type getReturnType() {
			return returnType;
		}

		@Override
		public boolean isVoid() {
			return Types.isVoid(returnType);
		}

		@Override
		public Type[] getParameterTypes() {
			return new Type[0];
		}

		@Override
		public boolean isRemotelyAccessible() {
			return remotelyAccessible;
		}

		@Override
		public <T> T invoke(Object object, Object... arguments) throws IllegalArgumentException, InvocationException, AuthorizationException {
			if (exception instanceof AuthorizationException) {
				throw (AuthorizationException) exception;
			}
			if (exception instanceof IllegalArgumentException) {
				throw (IllegalArgumentException) exception;
			}
			if (exception instanceof InvocationException) {
				throw (InvocationException) exception;
			}
			if (exception instanceof RuntimeException) {
				throw (RuntimeException) exception;
			}
			if (returnType == String.class) {
				return (T) "string value";
			}
			return null;
		}
	}

	private static class MockServletConfig extends ServletConfigStub {
		private MockServletContext context = new MockServletContext();

		@Override
		public String getServletName() {
			return "rest-servlet";
		}

		@Override
		public ServletContext getServletContext() {
			return context;
		}
	}

	private static class MockServletContext extends ServletContextStub {
		private Map<String, Object> attributes = new HashMap<>();

		@Override
		public String getServletContextName() {
			return "test-app";
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private String requestURI = "/test-app/rest/sub-resource";
		private String pathInfo = "/resource/sub-resource";
		private Map<String, String> attributes = new HashMap<>();

		@Override
		public String getRequestURI() {
			return requestURI;
		}

		@Override
		public String getContextPath() {
			return "/test-app";
		}

		@Override
		public String getPathInfo() {
			return pathInfo;
		}

		@Override
		public String getQueryString() {
			return null;
		}

		@Override
		public Locale getLocale() {
			return Locale.US;
		}

		@Override
		public String getRemoteHost() {
			return "localhost";
		}

		@Override
		public String getMethod() {
			return "POST";
		}

		@Override
		public Enumeration getHeaderNames() {
			return Collections.emptyEnumeration();
		}

		@Override
		public String getHeader(String name) {
			return attributes.get(name);
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private int statusCode;
		private Map<String, String> headers = new HashMap<>();
		private MockServletOutputStream outputStream = new MockServletOutputStream();

		@Override
		public void setCharacterEncoding(String charset) {
		}

		@Override
		public void setContentType(String type) {
			headers.put("Content-Type", type);
		}

		@Override
		public void setContentLength(int len) {
			headers.put("Content-Length", Integer.toString(len));
		}

		@Override
		public void setStatus(int sc) {
			statusCode = sc;
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			statusCode = sc;
			outputStream.write(msg.getBytes());
		}

		@Override
		public boolean isCommitted() {
			return false;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return outputStream;
		}

		@Override
		public void setHeader(String name, String value) {
			headers.put(name, value);
		}
	}

	private static class MockServletOutputStream extends ServletOutputStream {
		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		@Override
		public void write(int b) throws IOException {
			buffer.write(b);
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
