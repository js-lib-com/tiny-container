package js.tiny.container.net.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Collections;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.InvocationException;
import js.tiny.container.net.HttpRmiServlet;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class HttpRmiServletHandlerUnitTest {
	@Mock
	private ITinyContainer container;
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IManagedMethod managedMethod;
	
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private ServletOutputStream outputStream;

	private RequestContext context;
	private HttpRmiServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		doReturn(managedClass).when(container).getManagedClass(any());
		when(managedClass.getInstance()).thenReturn(new Object());
		when(managedClass.getManagedMethod(any())).thenReturn(managedMethod);
		when(managedMethod.getParameterTypes()).thenReturn(new Type[0]);
		when(managedMethod.invoke(any(), any())).thenReturn("string value");
		
		when(httpRequest.getRequestURI()).thenReturn("/test-app/java/lang/Object/toString");
		when(httpRequest.getContextPath()).thenReturn("/test-app");
		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
		
		when(httpResponse.getOutputStream()).thenReturn(outputStream);
		
		context = new RequestContext(container);

		servlet = new HttpRmiServlet();
		Classes.setFieldValue(servlet, AppServlet.class, "container", container);
	}

	@After
	public void afterTest() {
		context.detach();
	}
	
	@Test
	public void handleRequest() throws Exception {
		when(managedMethod.getReturnType()).thenReturn(String.class);
		executeRequestHandler();

		verify(httpResponse, times(1)).setStatus(200);
		
//		assertEquals(1, httpResponse.headers.size());
//		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
//		assertEquals("\"string value\"", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void handleRequest_Void() throws Exception {
		when(managedMethod.getReturnType()).thenReturn(Void.class);
		
		executeRequestHandler();

		verify(httpResponse, times(1)).setStatus(204);

//		assertTrue(httpResponse.headers.isEmpty());
//		assertEquals("", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void badRequest() throws Exception {
		when(httpRequest.getRequestURI()).thenReturn("/test-app/fake.rmi");
		
		executeRequestHandler();

		verify(httpResponse, times(1)).sendError(400, "/test-app/fake.rmi");
//		assertTrue(httpResponse.headers.isEmpty());
//		assertEquals("/test-app/fake.rmi", httpResponse.outputStream.buffer.toString());
	}

	@Test
	public void authorizationException() throws Exception {
		when(managedMethod.invoke(any(), any())).thenThrow(AuthorizationException.class);
		executeRequestHandler();

		verify(httpResponse, times(1)).setStatus(401);
//		assertEquals(1, httpResponse.headers.size());
//		assertEquals("Basic realm=app-test", httpResponse.headers.get("WWW-Authenticate"));
//		assertEquals("", httpResponse.outputStream.buffer.toString());
	}

	public void executionInvocationException() throws Exception {
		when(managedMethod.invoke(any(), any())).thenThrow(new InvocationException(new Exception("exception")));
		executeRequestHandler();

		verify(httpResponse, times(1)).setStatus(500);
//		assertEquals(3, httpResponse.headers.size());
//		assertEquals("application/json", httpResponse.headers.get("Content-Type"));
//		assertEquals("53", httpResponse.headers.get("Content-Length"));
//		assertEquals("en-US", httpResponse.headers.get("Content-Language"));
//		assertEquals("{\"cause\":\"java.lang.Exception\",\"message\":\"exception\"}", httpResponse.outputStream.buffer.toString());
	}

	public void executionRuntimeException() throws Exception {
		when(managedMethod.invoke(any(), any())).thenThrow(RuntimeException.class);
		executeRequestHandler();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void executeRequestHandler() throws Exception {
		context.attach(httpRequest, httpResponse);
		Classes.invoke(servlet, "handleRequest", context);
	}
}
