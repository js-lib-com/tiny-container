package js.tiny.container.mvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Collections;

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
import js.lang.BugError;
import js.lang.InvocationException;
import js.tiny.container.http.NoSuchResourceException;
import js.tiny.container.http.Resource;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServletHandlerTest {
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
	private IContainerService containerService;
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
	@Mock
	private MethodsCache cache;

	private RequestContext requestContext;
	private ResourceServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		when(container.getInstance(MethodsCache.class)).thenReturn(cache);
		
		when(servletConfig.getServletName()).thenReturn("resource-servlet");
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getServletContextName()).thenReturn("test-app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(httpRequest.getRequestURI()).thenReturn("/test-app/controller/index");
		when(httpRequest.getContextPath()).thenReturn("/test-app");
		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		when(managedMethod.getParameterTypes()).thenReturn(new Class[] { String.class });
		when(managedMethod.invoke(any(), any())).thenReturn(resource);

		when(argumentsFactory.getArgumentsReader(httpRequest, managedMethod.getParameterTypes())).thenReturn(argumentsReader);
		when(argumentsReader.read(httpRequest, managedMethod.getParameterTypes())).thenReturn(new Object[] { "value" });

		requestContext = new RequestContext(container);
		requestContext.attach(httpRequest, httpResponse);

		when(cache.get("/controller/index")).thenReturn(managedMethod);

		servlet = new ResourceServlet(argumentsFactory);
		servlet.init(servletConfig);
	}
	
	@Test
	public void GivenDefaults_WhenInvoke_Then200() throws Exception {
		// given

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(managedMethod, times(1)).invoke(any(), any());
		verify(httpResponse, times(1)).setStatus(200);
		verify(resource, times(1)).serialize(httpResponse);
	}

	@Test
	public void GivenParameterTypes_WhenInvoke_ThenArgumentValue() throws Exception {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Type[] { String.class });

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(managedMethod, times(1)).invoke(any(), eq("value"));
		verify(httpResponse, times(1)).setStatus(200);
		verify(resource, times(1)).serialize(httpResponse);
	}

	/** If method throws authorization exception and there is no login page uses servlet container authentication. */
	@Test
	public void GivenSecureMethodAndNoLoginPage_WhenInvoke_ThenHttpRequestAuthenticate() throws Exception {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(AuthorizationException.class);

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(httpRequest, times(1)).authenticate(httpResponse);
	}

	/** If method throws authorization exception and there is login page then redirect to it. */
	@Test
	public void GivenSecureMethodAndLoginPage_WhenInvoke_ThenHttpResponseRedirect() throws Exception {
		// given
		when(container.getLoginPage()).thenReturn("login-form.htm");
		when(managedMethod.invoke(any(), any())).thenThrow(AuthorizationException.class);

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(httpResponse, times(1)).sendRedirect("login-form.htm");
	}

	@Test
	public void GivenInvocationException_WhenInvoke_Then500() throws Exception {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(new InvocationException(new Exception("exception")));

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(httpResponse, times(1)).sendError(500, "exception");
	}

	/** Illegal argument generated by method invocation is processed as method not found, that is, with status code 404. */
	@Test
	public void GivenIllegalArgument_WhenInvoke_Then404() throws Exception {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(new IllegalArgumentException("exception"));

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(httpResponse, times(1)).sendError(404, "/test-app/controller/index");
	}

	@Test
	public void GivenMissingMethod_WhenInvoke_Then404() throws Exception {
		// given
		when(cache.get("/controller/index")).thenReturn(null);

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(httpResponse, times(1)).sendError(404, "/test-app/controller/index");
	}

	@Test
	public void GivenNoSuchResource_WhenInvoke_Then404() throws Exception {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(new InvocationException(new NoSuchResourceException()));

		// when
		servlet.handleRequest(requestContext);

		// then
		verify(httpResponse, times(1)).sendError(404, "/test-app/controller/index");
	}

	/** It is a bug if invoked managed method returns null. */
	@Test(expected = BugError.class)
	public void GivenNullReturnValue_WhenInvoke_ThenBugError() throws Exception {
		// given
		when(managedMethod.invoke(any(), any())).thenReturn(null);

		// when
		servlet.handleRequest(requestContext);

		// then
	}
}
