package js.tiny.container.net.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.junit.Before;
import org.junit.Test;

import js.lang.InvocationException;
import js.tiny.container.net.HttpRmiServlet;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.tiny.container.stub.TinyContainerStub;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpServletResponseStub;
import js.util.Classes;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class HttpRmiServletHandlerUnitTest {
	private MockContainer container;
	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;

	private RequestContext context;
	private HttpRmiServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		container = new MockContainer();
		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();

		context = new RequestContext(container);

		servlet = new HttpRmiServlet();
		Classes.setFieldValue(servlet, AppServlet.class, "container", container);
	}

	@Test
	public void handleRequest() throws Exception {
		container.managedClass.managedMethod.returnType = String.class;
		executeRequestHandler();

		assertEquals(200, httpResponse.statusCode);
		assertEquals(1, httpResponse.headers.size());
		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
		assertEquals("\"string value\"", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void handleRequest_Void() throws Exception {
		executeRequestHandler();

		assertEquals(204, httpResponse.statusCode);
		assertTrue(httpResponse.headers.isEmpty());
		assertEquals("", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void badRequest() throws Exception {
		httpRequest.requestURI = "/test-app/fake.rmi";
		executeRequestHandler();

		assertEquals(400, httpResponse.statusCode);
		assertTrue(httpResponse.headers.isEmpty());
		assertEquals("/test-app/fake.rmi", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void authorizationException() throws Exception {
		container.managedClass.managedMethod.exception = new AuthorizationException();
		executeRequestHandler();

		assertEquals(401, httpResponse.statusCode);
		assertEquals(1, httpResponse.headers.size());
		assertEquals("Basic realm=app-test", httpResponse.headers.get("WWW-Authenticate"));
		assertEquals("", httpResponse.outputStream.buffer.toString());
	}

	public void executionInvocationException() throws Exception {
		container.managedClass.managedMethod.exception = new InvocationException(new Exception("exception"));
		executeRequestHandler();

		assertEquals(500, httpResponse.statusCode);
		assertEquals(3, httpResponse.headers.size());
		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
		assertEquals("53", httpResponse.headers.get("Content-Length"));
		assertEquals("en-US", httpResponse.headers.get("Content-Language"));
		assertEquals("{\"cause\":\"java.lang.Exception\",\"message\":\"exception\"}", httpResponse.outputStream.buffer.toString());
	}

	public void executionRuntimeException() throws Exception {
		container.managedClass.managedMethod.exception = new RuntimeException();
		executeRequestHandler();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void executeRequestHandler() throws Exception {
		context.attach(httpRequest, httpResponse);
		Classes.invoke(servlet, "handleRequest", context);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends TinyContainerStub {
		private MockManagedClass managedClass = new MockManagedClass();

		@Override
		public IManagedClass getManagedClass(Class<?> interfaceClass) {
			return managedClass;
		}

		@Override
		public <T> T getInstance(IManagedClass managedClass) {
			return (T) new Object();
		}

		@Override
		public String getLoginRealm() {
			return "app-test";
		}

		@Override
		public String getLoginPage() {
			return "login-form.htm";
		}
	}

	private static class MockManagedClass extends ManagedClassSpiStub {
		private MockManagedMethod managedMethod = new MockManagedMethod();

		@Override
		public IManagedMethod getManagedMethod(String methodName) {
			return managedMethod;
		}
	}

	private static class MockManagedMethod extends ManagedMethodSpiStub {
		private Type returnType = void.class;
		private Exception exception;

		@Override
		public Type getReturnType() {
			return returnType;
		}

		@Override
		public Type[] getParameterTypes() {
			return new Type[0];
		}

		@Override
		public <T> T invoke(Object object, Object... arguments) throws IllegalArgumentException, InvocationException, AuthorizationException {
			if (exception instanceof InvocationException) {
				throw (InvocationException) exception;
			}
			if (exception instanceof AuthorizationException) {
				throw (AuthorizationException) exception;
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

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private String requestURI = "/test-app/java/lang/Object/toString";
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
