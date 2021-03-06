package js.tiny.container.mvc.unit;

import static org.junit.Assert.assertEquals;

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
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.lang.BugError;
import js.lang.InvocationException;
import js.tiny.container.AuthorizationException;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.http.NoSuchResourceException;
import js.tiny.container.http.Resource;
import js.tiny.container.http.encoder.ArgumentPartReader;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.mvc.ResourceServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpServletResponseStub;
import js.tiny.container.unit.ServletConfigStub;
import js.tiny.container.unit.ServletContextStub;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ResourceServletHandlerUnitTest {
	private MockContainer container;
	private MockManagedMethod method;

	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;
	private MockServletConfig config;
	private RequestContext context;
	private ResourceServlet servlet;

	@Before
	public void beforeTest() throws UnavailableException {
		method = new MockManagedMethod("index", Resource.class);
		container = new MockContainer();
		container.methods.add(method);

		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();

		config = new MockServletConfig();
		config.context.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);

		servlet = new ResourceServlet(new MockArgumentsReaderFactory());
	}

	/**
	 * Create resource servlet instance and add a method to cache, mapped to <code>/controller/index</code>. Create request
	 * context for <code>/test-app/controller/index</code> request URI and request path <code>/controller/index</code>. Invoke
	 * method and test invocation probe.
	 */
	@Test
	public void handleRequest() throws Exception {
		class MockResource implements Resource {
			private int serializeProbe;

			@Override
			public void serialize(HttpServletResponse httpResponse) throws IOException {
				++serializeProbe;
			}
		}
		method.resource = new MockResource();
		executeRequestHandler();

		assertEquals(1, method.invokeProbe);
		assertEquals(1, ((MockResource) method.resource).serializeProbe);
		assertEquals(200, httpResponse.statusCode);
	}

	@Test
	public void arguments() throws Exception {
		method.parameterTypes = new Type[] { String.class };
		executeRequestHandler();

		assertEquals(1, method.invokeProbe);
		assertEquals(1, method.arguments.length);
		assertEquals("value", method.arguments[0]);
		assertEquals(200, httpResponse.statusCode);
	}

	/** If method throws authorization exception and there is no login page uses servlet container authentication. */
	@Test
	public void authenticate() throws Exception {
		container.loginPage = null;
		method.exception = new AuthorizationException();
		executeRequestHandler();

		assertEquals(1, httpRequest.authenticateProbe);
	}

	/** If method throws authorization exception and there is login page redirect to it. */
	@Test
	public void redirectLoginPage() throws Exception {
		container.loginPage = "login-form.htm";
		method.exception = new AuthorizationException();
		executeRequestHandler();

		assertEquals(1, httpResponse.redirectProbe);
		assertEquals("login-form.htm", httpResponse.location);
	}

	@Test
	public void invocationException() throws Exception {
		method.exception = new InvocationException(new Exception("exception"));
		executeRequestHandler();

		assertEquals(500, httpResponse.statusCode);
		assertEquals("exception", httpResponse.errorMessage);
	}

	/** Illegal argument generated by method invocation is processed as method not found, that is, with status code 404. */
	@Test
	public void illegalArgument() throws Exception {
		method.exception = new IllegalArgumentException("exception");
		executeRequestHandler();

		assertEquals(404, httpResponse.statusCode);
		assertEquals("/test-app/controller/index", httpResponse.errorMessage);
	}

	@Test
	public void missingResource() throws Exception {
		// remove method to emulate missing resource
		container.methods.remove(method);
		executeRequestHandler();

		assertEquals(404, httpResponse.statusCode);
		assertEquals("/test-app/controller/index", httpResponse.errorMessage);
	}

	@Test
	public void noSuchResourceException() throws Exception {
		method.exception = new InvocationException(new NoSuchResourceException());
		executeRequestHandler();

		assertEquals(404, httpResponse.statusCode);
		assertEquals("/test-app/controller/index", httpResponse.errorMessage);
	}

	/** It is a bug if invoked resource method returns null. */
	@Test(expected = BugError.class)
	public void nullResource() throws Exception {
		// method returns null resource
		method.resource = null;
		executeRequestHandler();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void executeRequestHandler() throws Exception {
		servlet.init(config);
		Classes.invoke(servlet, "handleRequest", context);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends ContainerStub {
		private List<ManagedMethodSPI> methods = new ArrayList<>();
		private String loginPage;

		@Override
		public Iterable<ManagedMethodSPI> getNetMethods() {
			return methods;
		}

		@Override
		public <T> T getInstance(ManagedClassSPI managedClass, Object... args) {
			return (T) new Object();
		}

		@Override
		public String getLoginPage() {
			return loginPage;
		}
	}

	private static class MockManagedClass extends ManagedClassSpiStub {
		private String requestPath = "controller";

		@Override
		public String getRequestPath() {
			return requestPath;
		}
	}

	private static class MockManagedMethod extends ManagedMethodSpiStub {
		private MockManagedClass declaringClass = new MockManagedClass();
		private String requestPath;
		private Type[] parameterTypes = new Type[0];
		private Class<?> returnType;

		private Throwable exception;
		private int invokeProbe;
		private Object[] arguments;
		private Resource resource = new Resource() {
			@Override
			public void serialize(HttpServletResponse httpResponse) throws IOException {
			}
		};

		public MockManagedMethod(String requestPath, Class<?> returnType) {
			this.requestPath = requestPath;
			this.returnType = returnType;
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
		public Type[] getParameterTypes() {
			return parameterTypes;
		}

		@Override
		public Type getReturnType() {
			return returnType;
		}

		@Override
		public <T> T invoke(Object object, Object... arguments) throws IllegalArgumentException, InvocationException, AuthorizationException {
			if (exception instanceof IllegalArgumentException) {
				throw (IllegalArgumentException) exception;
			}
			if (exception instanceof InvocationException) {
				throw (InvocationException) exception;
			}
			if (exception instanceof AuthorizationException) {
				throw (AuthorizationException) exception;
			}
			++invokeProbe;
			this.arguments = arguments;
			return (T) resource;
		}
	}

	private static class MockArgumentsReaderFactory implements ArgumentsReaderFactory {
		@Override
		public ArgumentsReader getArgumentsReader(HttpServletRequest httpRequest, Type[] formalParameters) {
			return new ArgumentsReader() {
				public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) {
					return new Object[] { "value" };
				}

				public void clean() {
				}
			};
		}

		@Override
		public ArgumentPartReader getArgumentPartReader(String contentType, Type parameterType) {
			throw new UnsupportedOperationException();
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private int authenticateProbe;

		@Override
		public String getRequestURI() {
			return "/test-app/controller/index";
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
		public String getMethod() {
			return "POST";
		}

		@Override
		public Enumeration getHeaderNames() {
			return Collections.emptyEnumeration();
		}

		@Override
		public String getRemoteHost() {
			return "localhost";
		}

		@Override
		public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
			++authenticateProbe;
			return true;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private int statusCode;
		private String errorMessage;

		private int redirectProbe;
		private String location;

		@Override
		public void setStatus(int sc) {
			statusCode = sc;
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			statusCode = sc;
			errorMessage = msg;
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			++redirectProbe;
			this.location = location;
		}
	}

	private static class MockServletConfig extends ServletConfigStub {
		private MockServletContext context = new MockServletContext();

		@Override
		public String getServletName() {
			return "resource-servlet";
		}

		@Override
		public ServletContext getServletContext() {
			return context;
		}

		@Override
		public String getInitParameter(String name) {
			return null;
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

		@Override
		public String getInitParameter(String name) {
			return null;
		}
	}
}
