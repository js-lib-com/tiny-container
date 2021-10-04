package js.tiny.container.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.json.Json;
import js.lang.InvocationException;
import js.tiny.container.http.Resource;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class RestServletUnitTest {
	@Mock
	private ITinyContainer container;
	@Mock
	private IManagedClass managedClass;
	@Mock
	private IManagedMethod managedMethod;

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

	private RequestContext context;
	private RestServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		when(container.getInstance(Json.class)).thenReturn(json);

		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(managedClass.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("resource"));
		when(managedMethod.getDeclaringClass()).thenReturn(managedClass);

		when(httpRequest.getRequestURI()).thenReturn("/test-app/rest/sub-resource");
		when(httpRequest.getContextPath()).thenReturn("/test-app");
		when(httpRequest.getPathInfo()).thenReturn("/resource/rest/sub-resource");
		when(httpRequest.getLocale()).thenReturn(Locale.ENGLISH);

		when(httpResponse.getOutputStream()).thenReturn(responseStream);

		context = new RequestContext(container);
		servlet = new RestServlet();
		Classes.setFieldValue(servlet, AppServlet.class, "container", container);
	}

	@Test
	public void GivenDefault_WhenConstructor_ThenNotNullState() {
		RestServlet servlet = new RestServlet();
		assertNotNull(Classes.getFieldValue(servlet, "argumentsReaderFactory"));
		assertNotNull(Classes.getFieldValue(servlet, "valueWriterFactory"));
	}

	@Test
	public void GivenRemoteMethod_WhenServletInit_ThenMethodRegistered() throws ServletException {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("user"));
		when(managedMethod.getReturnType()).thenReturn(void.class);
		when(managedMethod.isRemotelyAccessible()).thenReturn(true);

		// when
		RestServlet servlet = new RestServlet();
		servlet.init(servletConfig);

		// then
		Map<String, IManagedMethod> methods = Classes.getFieldValue(servlet, "restMethods");
		assertNotNull(methods);
		assertEquals(1, methods.size());
		assertNotNull(methods.get("/resource/user"));
		assertEquals("user", methods.get("/resource/user").getServiceMeta(PathMeta.class).value());
	}

	@Test
	public void GivenNotRemoteMethod_WhenServletInit_ThenMethodNotRegistered() throws ServletException {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.isRemotelyAccessible()).thenReturn(false);

		// when
		RestServlet servlet = new RestServlet();
		servlet.init(servletConfig);

		// then
		Map<String, IManagedMethod> methods = Classes.getFieldValue(servlet, "restMethods");
		assertNotNull(methods);
		assertEquals(0, methods.size());
	}

	@Test
	public void GivenResourceMethod_WhenServletInit_ThenMethodNotRegistered() throws ServletException {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.getReturnType()).thenReturn(Resource.class);
		when(managedMethod.isRemotelyAccessible()).thenReturn(true);

		// when
		RestServlet servlet = new RestServlet();
		servlet.init(servletConfig);

		// then
		Map<String, IManagedMethod> methods = Classes.getFieldValue(servlet, "restMethods");
		assertNotNull(methods);
		assertEquals(0, methods.size());
	}

	@Test(expected = UnavailableException.class)
	public void GivenNoContainerAttr_WhenServletInit_ThenException() throws ServletException {
		// given
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(null);

		// when
		RestServlet servlet = new RestServlet();
		servlet.init(servletConfig);

		// then
	}

	@Test
	public void GivenAppContext_WhenCreateStorageKey_ThenIncludeAppContext() throws Exception {
		// given
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("sub-resource"));

		// when
		String key = key(managedMethod);

		// then
		assertEquals("/resource/sub-resource", key);
	}

	@Test
	public void GivenRootContext_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given
		when(managedClass.getServiceMeta(PathMeta.class)).thenReturn(null);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("sub-resource"));

		// when
		String key = key(managedMethod);

		// then
		assertEquals("/sub-resource", key);
	}

	@Test
	public void GivenValidRequestPath_WhenCreateRetrieveKey_ThenValidKey() throws Exception {
		assertEquals("/resource/sub-resource", key("/resource/sub-resource?query"));
		assertEquals("/resource/sub-resource", key("/resource/sub-resource?"));
		assertEquals("/resource/sub-resource", key("/resource/sub-resource"));

		assertEquals("/resource/sub-resource", key("/resource/sub-resource.ext?query"));

		assertEquals("/sub-resource", key("/sub-resource?query"));
		assertEquals("/sub-resource", key("/sub-resource?"));
		assertEquals("/sub-resource", key("/sub-resource"));
	}

	@Test
	public void GivenStringRemoteMethod_WhenInvoke_Then200() throws Exception {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/sub-resource"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.getReturnType()).thenReturn(String.class);
		when(managedMethod.invoke(null)).thenReturn("string value");

		// when
		executeRequestHandler();

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("application/json");

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		ArgumentCaptor<Integer> offsetArg = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(Integer.class);
		verify(responseStream).write(bytesArg.capture(), offsetArg.capture(), lengthArg.capture());
		assertThat(new String(bytesArg.getValue(), offsetArg.getValue(), lengthArg.getValue()), equalTo("\"string value\""));
	}

	@Test
	public void GivenVoidRemoteMethod_WhenInvoke_Then204() throws Exception {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/sub-resource"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.isVoid()).thenReturn(true);

		// when
		executeRequestHandler();

		// then
		verify(httpResponse, times(1)).setStatus(204);
		verify(httpResponse, times(0)).setContentType(anyString());
		verify(responseStream, times(0)).write(any(byte[].class), anyInt(), anyInt());
	}

	@Test
	public void GivenNotAuthorizedRemoteMethod_WhenInvoke_Then401() throws Exception {
		// given
		when(container.getLoginRealm()).thenReturn("Test App");
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/sub-resource"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(AuthorizationException.class);

		// when
		executeRequestHandler();

		// then
		verify(httpResponse, times(1)).setStatus(401);
		verify(httpResponse, times(1)).setHeader("WWW-Authenticate", "Basic realm=Test App");
		verify(httpResponse, times(0)).setContentType(anyString());
		verify(responseStream, times(0)).write(any(byte[].class), anyInt(), anyInt());
	}

	@Test
	public void GivenNotFoundMethod_WhenInvoke_Then404() throws Exception {
		// given
		String error = "\"error message\"";
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(json.stringify(any())).thenReturn(error);

		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/missing-method"));

		// when
		executeRequestHandler();

		// then
		verify(httpResponse, times(1)).setStatus(404);
		verify(httpResponse, times(1)).setHeader("Content-Language", "en");
		verify(httpResponse, times(1)).setContentType("application/json");
		verify(responseStream, times(1)).write(error.getBytes());
	}

	/** For now illegal argument exception is considered bad request 400. */
	@Test
	public void GivenIllegalArgument_WhenInvoke_Then400() throws Exception {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));

		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/sub-resource"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(IllegalArgumentException.class);

		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

		// when
		executeRequestHandler();

		// then
		verify(httpResponse, times(1)).sendError(400, "/test-app/rest/sub-resource");
		verify(httpResponse, times(0)).setStatus(anyInt());
		verify(responseStream, times(0)).write(any(byte[].class));
	}

	@Test
	public void GivenInvocationException_WhenInvoke_Then500() throws Exception {
		// given
		String error = "\"error message\"";
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(json.stringify(any())).thenReturn(error);

		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/sub-resource"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(new InvocationException(new Exception("exception")));

		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

		// when
		executeRequestHandler();

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(httpResponse, times(1)).setHeader("Content-Language", "en");
		verify(httpResponse, times(1)).setContentType("application/json");
		verify(responseStream, times(1)).write(error.getBytes());
	}

	@Test(expected = RuntimeException.class)
	public void GivenRuntimeException_WhenInvoke_ThenException() throws Exception {
		// given
		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));

		when(managedMethod.isRemotelyAccessible()).thenReturn(true);
		when(managedMethod.getServiceMeta(PathMeta.class)).thenReturn(new PathMeta("rest/sub-resource"));
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(RuntimeException.class);

		// when
		executeRequestHandler();

		// then
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void executeRequestHandler() throws Exception {
		servlet.init(servletConfig);
		context.attach(httpRequest, httpResponse);
		Classes.invoke(servlet, "handleRequest", context);
	}

	private static String key(IManagedMethod resourceMethod) throws Exception {
		return Classes.invoke(RestServlet.class, "key", resourceMethod);
	}

	private static String key(String requestPath) throws Exception {
		return Classes.invoke(RestServlet.class, "key", requestPath);
	}
}
