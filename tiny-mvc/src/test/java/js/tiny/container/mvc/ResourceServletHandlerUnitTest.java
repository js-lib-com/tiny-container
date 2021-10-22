package js.tiny.container.mvc;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.json.Json;
import js.tiny.container.http.Resource;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServletHandlerUnitTest {
	@Mock
	private ServletConfig servletConfig;
	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private ServletOutputStream responseStream;

	@Mock
	private Json json;

	@Mock
	private ITinyContainer container;
	@Mock
	private IContainerService service;
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Mock
	private ArgumentsReaderFactory argumentsFactory;
	@Mock
	private ArgumentsReader argumentsReader;

	@Mock
	private Resource resource;

	private RequestContext context;
	private ResourceServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		when(servletConfig.getServletName()).thenReturn("resource-servlet");
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getServletContextName()).thenReturn("test-app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(httpRequest.getRequestURI()).thenReturn("/test-app/controller/index");
		when(httpRequest.getContextPath()).thenReturn("/test-app");

		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		// when(container.getLoginPage()).thenReturn("login");
		doReturn(new Object()).when(container).getInstance(managedClass);

		when(managedClass.getServiceMeta(ControllerMeta.class)).thenReturn(new ControllerMeta(service, "controller"));
		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		when(managedMethod.getServiceMeta(RequestPathMeta.class)).thenReturn(new RequestPathMeta(service, "index"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[] { String.class });
		when(managedMethod.getReturnType()).thenReturn(Resource.class);
		when(managedMethod.invoke(any(), any())).thenReturn(resource);

		when(argumentsFactory.getArgumentsReader(httpRequest, managedMethod.getParameterTypes())).thenReturn(argumentsReader);
		when(argumentsReader.read(httpRequest, managedMethod.getParameterTypes())).thenReturn(new Object[] { "value" });

		context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);

		servlet = new ResourceServlet(argumentsFactory);
	}

	/**
	 * Create resource servlet instance and add a method to cache, mapped to <code>/controller/index</code>. Create request
	 * context for <code>/test-app/controller/index</code> request URI and request path <code>/controller/index</code>. Invoke
	 * method and test 'invoke' was called.
	 */
	@Test
	public void GivenDefaults_WhenInvoke_Then200() throws Exception {
		// given

		// when
		executeRequestHandler();

		// then
		verify(managedMethod, times(1)).invoke(any(), any());
		verify(httpResponse, times(1)).setStatus(200);
		verify(resource, times(1)).serialize(httpResponse);
	}

	/*
	 * @Test public void arguments() throws Exception { method.parameterTypes = new Type[] { String.class };
	 * executeRequestHandler();
	 * 
	 * assertEquals(1, method.invokeProbe); assertEquals(1, method.arguments.length); assertEquals("value",
	 * method.arguments[0]); assertEquals(200, httpResponse.statusCode); }
	 */

	/** If method throws authorization exception and there is no login page uses servlet container authentication. */
	/*
	 * @Test public void authenticate() throws Exception { container.loginPage = null; method.exception = new
	 * AuthorizationException(); executeRequestHandler();
	 * 
	 * assertEquals(1, httpRequest.authenticateProbe); }
	 */

	/** If method throws authorization exception and there is login page redirect to it. */
	/*
	 * @Test public void redirectLoginPage() throws Exception { container.loginPage = "login-form.htm"; method.exception = new
	 * AuthorizationException(); executeRequestHandler();
	 * 
	 * assertEquals(1, httpResponse.redirectProbe); assertEquals("login-form.htm", httpResponse.location); }
	 * 
	 * @Test public void invocationException() throws Exception { method.exception = new InvocationException(new
	 * Exception("exception")); executeRequestHandler();
	 * 
	 * assertEquals(500, httpResponse.statusCode); assertEquals("exception", httpResponse.errorMessage); }
	 */
	/** Illegal argument generated by method invocation is processed as method not found, that is, with status code 404. */

	/*
	 * @Test public void illegalArgument() throws Exception { method.exception = new IllegalArgumentException("exception");
	 * executeRequestHandler();
	 * 
	 * assertEquals(404, httpResponse.statusCode); assertEquals("/test-app/controller/index", httpResponse.errorMessage); }
	 * 
	 * @Test public void missingResource() throws Exception { // remove method to emulate missing resource
	 * container.methods.remove(method); executeRequestHandler();
	 * 
	 * assertEquals(404, httpResponse.statusCode); assertEquals("/test-app/controller/index", httpResponse.errorMessage); }
	 * 
	 * @Test public void noSuchResourceException() throws Exception { method.exception = new InvocationException(new
	 * NoSuchResourceException()); executeRequestHandler();
	 * 
	 * assertEquals(404, httpResponse.statusCode); assertEquals("/test-app/controller/index", httpResponse.errorMessage); }
	 */
	/** It is a bug if invoked resource method returns null. */
	/*
	 * @Test(expected = BugError.class) public void nullResource() throws Exception { // method returns null resource
	 * method.resource = null; executeRequestHandler(); }
	 */

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void executeRequestHandler() throws Exception {
		servlet.init(servletConfig);
		Classes.invoke(servlet, "handleRequest", context);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	/*
	 * private static class MockManagedMethod extends ManagedMethodSpiStub { private MockManagedClass declaringClass = new
	 * MockManagedClass(); private String requestPath; private Type[] parameterTypes = new Type[0]; private Class<?> returnType;
	 * 
	 * private Throwable exception; private int invokeProbe; private Object[] arguments; private Resource resource = new
	 * Resource() {
	 * 
	 * @Override public void serialize(HttpServletResponse httpResponse) throws IOException { } };
	 * 
	 * public MockManagedMethod(String requestPath, Class<?> returnType) { this.requestPath = requestPath; this.returnType =
	 * returnType; }
	 * 
	 * @Override public <T> T invoke(Object object, Object... arguments) throws IllegalArgumentException, InvocationException,
	 * AuthorizationException { if (exception instanceof IllegalArgumentException) { throw (IllegalArgumentException) exception;
	 * } if (exception instanceof InvocationException) { throw (InvocationException) exception; } if (exception instanceof
	 * AuthorizationException) { throw (AuthorizationException) exception; } ++invokeProbe; this.arguments = arguments; return
	 * (T) resource; } }
	 * 
	 * private static class MockHttpServletRequest extends HttpServletRequestStub { private int authenticateProbe;
	 * 
	 * @Override public String getRequestURI() { return "/test-app/controller/index"; }
	 * 
	 * @Override public String getContextPath() { return "/test-app"; }
	 * 
	 * @Override public String getQueryString() { return null; }
	 * 
	 * @Override public Locale getLocale() { return Locale.US; }
	 * 
	 * @Override public String getMethod() { return "POST"; }
	 * 
	 * @Override public Enumeration getHeaderNames() { return Collections.emptyEnumeration(); }
	 * 
	 * @Override public String getRemoteHost() { return "localhost"; }
	 * 
	 * @Override public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
	 * ++authenticateProbe; return true; } }
	 * 
	 * private static class MockHttpServletResponse extends HttpServletResponseStub { private int statusCode; private String
	 * errorMessage;
	 * 
	 * private int redirectProbe; private String location;
	 * 
	 * @Override public void setStatus(int sc) { statusCode = sc; }
	 * 
	 * @Override public void sendError(int sc, String msg) throws IOException { statusCode = sc; errorMessage = msg; }
	 * 
	 * @Override public void sendRedirect(String location) throws IOException { ++redirectProbe; this.location = location; } }
	 */
}
