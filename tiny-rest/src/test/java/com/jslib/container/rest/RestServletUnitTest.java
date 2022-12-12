package com.jslib.container.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.json.Json;
import com.jslib.container.http.encoder.ArgumentsReader;
import com.jslib.container.http.encoder.ServerEncoders;
import com.jslib.container.http.encoder.ValueWriter;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.servlet.TinyContainer;
import com.jslib.container.spi.IContainerService;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IManagedParameter;
import com.jslib.container.spi.ITinyContainer;
import com.jslib.lang.InvocationException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;

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
	private IManagedParameter managedParameter;

	@Mock
	private ServletConfig servletConfig;
	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	@Mock
	private Path methodPath;
	@Mock
	private PathMethodsCache cache;
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
		when(container.getInstance(PathMethodsCache.class)).thenReturn(cache);

		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);
		when(servletContext.getContextPath()).thenReturn("");

		doReturn(managedClass).when(managedMethod).getDeclaringClass();
		when(managedMethod.getManagedParameters()).thenReturn(Arrays.asList(managedParameter));
		when(managedParameter.getAnnotations()).thenReturn(new Annotation[0]);

		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test-app/rest/sub-resource");
		when(httpRequest.getContextPath()).thenReturn("/test-app");
		when(httpRequest.getPathInfo()).thenReturn("/resource/rest/sub-resource");
		when(httpRequest.getLocale()).thenReturn(Locale.ENGLISH);
		when(httpRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());

		when(cache.get("POST", "/resource/rest/sub-resource")).thenReturn(new PathTree.Item<>(managedMethod));
		when(encoders.getArgumentsReader(any(), any())).thenReturn(argumentsReader);
		when(encoders.getValueWriter(any())).thenReturn(valueWriter);
		when(argumentsReader.read(any(), any())).thenReturn(new Object[] {});

		context = new RequestContext(container);
		servlet = new RestServlet(encoders);

		servlet.init(servletConfig);
		context.attach(httpRequest, httpResponse);
	}

	@Test
	public void GivenStringRemoteMethod_WhenInvoke_ThenOK() throws Throwable {
		// given
		when(managedMethod.getManagedParameters()).thenReturn(Collections.emptyList());
		when(managedMethod.getReturnType()).thenReturn(String.class);
		when(managedMethod.invoke(any(), any())).thenReturn("string value");

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("application/json");
	}

	@Test
	public void GivenStringRemoteMethodWithParameter_WhenInvoke_ThenOK() throws Throwable {
		// given
		when(managedMethod.getReturnType()).thenReturn(String.class);
		when(managedMethod.invoke(any(),any())).thenReturn("string value");
		when(argumentsReader.read(any(), any())).thenReturn(new Object[] {"argument"});

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("application/json");
	}

	@Test
	public void GivenVoidRemoteMethod_WhenInvoke_ThenNoContent() throws Throwable {
		// given
		when(managedMethod.getManagedParameters()).thenReturn(Collections.emptyList());
		when(managedMethod.getReturnType()).thenReturn(void.class);

		// when
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(204);
		verify(httpResponse, times(0)).setContentType(anyString());
	}

	@Test(expected = GeneralSecurityException.class)
	public void GivenNotAuthorizedRemoteMethod_WhenInvoke_ThenException() throws Throwable {
		// given
		when(managedMethod.getManagedParameters()).thenReturn(Collections.emptyList());
		when(managedMethod.invoke(null, new Object[0])).thenThrow(GeneralSecurityException.class);

		// when
		servlet.handleRequest(context);

		// then
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenIllegalArgument_WhenInvoke_ThenException() throws Throwable {
		// given

		when(managedMethod.getManagedParameters()).thenReturn(Collections.emptyList());
		when(managedMethod.invoke(null, new Object[0])).thenThrow(IllegalArgumentException.class);

		// when
		servlet.handleRequest(context);

		// then
	}

	@Test(expected = InvocationException.class)
	public void GivenInvocationException_WhenInvoke_ThenException() throws Throwable {
		// given
		when(managedMethod.getManagedParameters()).thenReturn(Collections.emptyList());
		when(managedMethod.invoke(null, new Object[0])).thenThrow(new InvocationException(new Exception("exception")));


		// when
		servlet.handleRequest(context);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenRuntimeException_WhenInvoke_ThenException() throws Throwable {
		// given
		when(managedMethod.getManagedParameters()).thenReturn(Collections.emptyList());
		when(managedMethod.invoke(null, new Object[0])).thenThrow(RuntimeException.class);

		// when
		servlet.handleRequest(context);

		// then
	}
}
