package com.jslib.container.rmi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.servlet.AppServlet;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.ITinyContainer;
import com.jslib.lang.InvocationException;
import com.jslib.util.Classes;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class HttpRmiServletHandlerUnitTest {
	@Mock
	private ITinyContainer container;
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private ServletOutputStream outputStream;

	private RequestContext context;
	private HttpRmiServlet servlet;

	@Before
	public void beforeTest() throws Throwable {
		doReturn(managedClass).when(container).getManagedClass(any());
		when(managedClass.getInstance()).thenReturn(new Object());
		when(managedClass.getManagedMethod(any())).thenReturn(managedMethod);
		when(managedMethod.getParameterTypes()).thenReturn(new Type[0]);

		when(httpRequest.getRequestURI()).thenReturn("/test-app/java/lang/Object/toString");
		when(httpRequest.getContextPath()).thenReturn("/test-app");

		when(httpResponse.getOutputStream()).thenReturn(outputStream);

		context = new RequestContext(container);

		servlet = new HttpRmiServlet();
		Classes.setFieldValue(servlet, AppServlet.class, "container", container);
	}

	@Test
	public void GivenNonVoidMethod_WhenHandleRequest_ThenOK() throws Throwable {
		// given
		when(managedMethod.getReturnType()).thenReturn(String.class);

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("application/json");
	}

	@Test
	public void GivenStringMethod_WhenHandleRequest_ThenJsonString() throws Throwable {
		// given
		String value = "string value";
		String json = "\"string value\"";

		when(managedMethod.getReturnType()).thenReturn(String.class);
		when(managedMethod.invoke(any(), any())).thenReturn(value);

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
		ArgumentCaptor<byte[]> bufferArg = ArgumentCaptor.forClass(byte[].class);
		verify(outputStream).write(bufferArg.capture(), eq(0), eq(json.length()));
		assertThat(Arrays.copyOfRange(bufferArg.getValue(), 0, json.length()), equalTo(json.getBytes()));
	}

	@Test
	public void GivenVoidMethod_WhenHandleRequest_ThenNoContent() throws Throwable {
		// given
		when(managedMethod.getReturnType()).thenReturn(Void.class);

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
		verify(httpResponse, times(1)).setStatus(204);
		verify(httpResponse, times(0)).setContentType(any());
	}

	@Test(expected = ClassNotFoundException.class)
	public void GivenMissingClassPath_WhenHandleRequest_ThenException() throws Throwable {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test-app/fake.rmi");

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
	}

	@Test(expected = GeneralSecurityException.class)
	public void GivenMethodAccessNotGranted_WhenHandleRequest_ThenException() throws Throwable {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(GeneralSecurityException.class);

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
	}

	@Test(expected = InvocationException.class)
	public void GivenMethodInvocationException_WhenHandleRequest_ThenException() throws Throwable {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(new InvocationException(new Exception("exception")));

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenMethodRuntimeException_WhenHandleRequest_ThenException() throws Throwable {
		// given
		when(managedMethod.invoke(any(), any())).thenThrow(RuntimeException.class);

		// when
		context.attach(httpRequest, httpResponse);
		servlet.handleRequest(context);

		// then
	}
}
