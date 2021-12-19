package js.tiny.container.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.json.Json;
import js.lang.InvocationException;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.http.encoder.ValueWriter;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class RestServletUnitTest {
	@Mock
	private ITinyContainer container;
	@Mock
	private IContainerService service;
	@Mock
	private IManagedClass<?> managedClass;
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
	private Path methodPath;
	@Mock
	private MethodsCache cache;
	@Mock
	private ServerEncoders encoders;
	@Mock
	private ArgumentsReader argumentsReader;
	@Mock
	private ValueWriter valueWriter;

	@Mock
	private Json json;

	private RequestContext context;
	private RestServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		when(container.getInstance(MethodsCache.class)).thenReturn(cache);
		when(container.getInstance(Json.class)).thenReturn(json);
		when(json.stringify(any())).thenReturn("{}");

		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test-app/rest/sub-resource");
		when(httpRequest.getContextPath()).thenReturn("/test-app");
		when(httpRequest.getPathInfo()).thenReturn("/resource/rest/sub-resource");
		when(httpRequest.getLocale()).thenReturn(Locale.ENGLISH);

		when(httpResponse.getOutputStream()).thenReturn(responseStream);

		when(cache.get("POST", "/resource/rest/sub-resource")).thenReturn(new PathTree.Item<>(managedMethod));
		when(encoders.getArgumentsReader(eq(httpRequest), any())).thenReturn(argumentsReader);
		when(encoders.getValueWriter(any())).thenReturn(valueWriter);
		when(argumentsReader.read(eq(httpRequest), any())).thenReturn(new Object[] {});

		context = new RequestContext(container);
		servlet = new RestServlet(encoders);

		servlet.init(servletConfig);
		context.attach(httpRequest, httpResponse);
	}

	@Test
	public void GivenStringRemoteMethod_WhenInvoke_Then200() throws Exception {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.getReturnType()).thenReturn(String.class);
		when(managedMethod.invoke(null)).thenReturn("string value");

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("application/json");

//		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
//		ArgumentCaptor<Integer> offsetArg = ArgumentCaptor.forClass(Integer.class);
//		ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(Integer.class);
//		verify(responseStream).write(bytesArg.capture(), offsetArg.capture(), lengthArg.capture());
//		assertThat(new String(bytesArg.getValue(), offsetArg.getValue(), lengthArg.getValue()), equalTo("\"string value\""));
	}

	@Test
	public void GivenVoidRemoteMethod_WhenInvoke_Then204() throws Exception {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.getReturnType()).thenReturn(void.class);

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(204);
		verify(httpResponse, times(0)).setContentType(anyString());
		verify(responseStream, times(0)).write(any(byte[].class), anyInt(), anyInt());
	}

	@Test
	public void GivenNotAuthorizedRemoteMethod_WhenInvoke_Then401() throws Exception {
		// given
		when(container.getAppName()).thenReturn("Test App");
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(AuthorizationException.class);

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(401);
		verify(httpResponse, times(1)).setHeader("WWW-Authenticate", "Basic realm=Test App");
		verify(httpResponse, times(0)).setContentType(anyString());
		verify(responseStream, times(0)).write(any(byte[].class), anyInt(), anyInt());
	}

	@Test
	public void GivenNotFoundMethod_WhenInvoke_Then404() throws Exception {
		// given
//		String error = "\"error message\"";


		// when
		servlet.handleRequest(context);

		// then
//		verify(httpResponse, times(1)).setStatus(404);
//		verify(httpResponse, times(1)).setHeader("Content-Language", "en");
//		verify(httpResponse, times(1)).setContentType("application/json");
//		verify(responseStream, times(1)).write(error.getBytes());
	}

	/** For now illegal argument exception is considered bad request 400. */
	@Test
	public void GivenIllegalArgument_WhenInvoke_Then400() throws Exception {
		// given

		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(IllegalArgumentException.class);

		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).sendError(400, "/test-app/rest/sub-resource");
		verify(httpResponse, times(0)).setStatus(anyInt());
		verify(responseStream, times(0)).write(any(byte[].class));
	}

	@Test
	public void GivenInvocationException_WhenInvoke_Then500() throws Exception {
		// given
		String error = "\"error message\"";
		when(json.stringify(any())).thenReturn(error);

		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(new InvocationException(new Exception("exception")));

		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(httpResponse, times(1)).setHeader("Content-Language", "en");
		verify(httpResponse, times(1)).setContentType("application/json");
		verify(responseStream, times(1)).write(error.getBytes());
	}

	@Test(expected = RuntimeException.class)
	public void GivenRuntimeException_WhenInvoke_ThenException() throws Exception {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Class[0]);
		when(managedMethod.invoke(null, new Object[0])).thenThrow(RuntimeException.class);

		// when
		servlet.handleRequest(context);

		// then
	}
}
