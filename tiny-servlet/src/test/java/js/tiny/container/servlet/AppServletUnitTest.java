package js.tiny.container.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.json.Json;
import js.lang.InvocationException;
import js.rmi.BusinessException;
import js.tiny.container.spi.ITinyContainer;
import js.util.Classes;

@SuppressWarnings({ "unused", "serial" })
@RunWith(MockitoJUnitRunner.class)
public class AppServletUnitTest {
	private static final String DESCRIPTOR = "" + //
			"<?xml version='1.0' encoding='UTF-8'?>" + //
			"<test>" + //
			"	<managed-classes>" + //
			"		<controller interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.AppServletUnitTest$MockRequestContext' />" + //
			"	</managed-classes>" + //
			"	<login>" + //
			"		<property name='realm' value='Test App' />" + //
			"		<property name='page' value='login.xsp' />" + //
			"	</login>" + //
			"</test>";

	@Mock
	private ITinyContainer container;
	@Mock
	private Json json;

	@Mock
	private RequestContext requestContext;

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

	@Before
	public void beforeTest() throws Exception {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getContainer()).thenReturn(container);
		when(requestContext.getRequest()).thenReturn(httpRequest);
		when(requestContext.getResponse()).thenReturn(httpResponse);
		when(requestContext.getLocale()).thenReturn(Locale.getDefault());

		when(container.getInstance(Json.class)).thenReturn(json);

		when(servletConfig.getServletName()).thenReturn("ServletName");
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getServletContextName()).thenReturn("test-app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(httpRequest.getServletContext()).thenReturn(servletContext);
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getContextPath()).thenReturn("test");
		when(httpRequest.getRequestURI()).thenReturn("test/service");

		when(httpResponse.getOutputStream()).thenReturn(responseStream);
	}

	@Test
	public void GivenAppServlet_WhenInit_ThenFields() throws ServletException {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
			}
		};

		// when
		servlet.init(servletConfig);

		// then
		assertThat(servlet.servletName(), equalTo("test-app#ServletName"));
		assertThat(servlet.container(), notNullValue());
		assertThat(servlet.container(), equalTo(container));
	}

	@Test(expected = UnavailableException.class)
	public void GivenAttrInstance_WhenInit_ThenException() throws ServletException {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
			}
		};
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(null);

		// when
		servlet.init(servletConfig);

		// then
	}

	@Test
	public void GivenServlet_WhenDestroy_Then() {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
			}
		};

		// when
		servlet.destroy();

		// then
	}

	@Test
	public void GivenServlet_WhenService_ThenRequestContext() throws Exception {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
			}
		};

		// when
		exerciseService(servlet);

		// then
		verify(requestContext, times(1)).attach(httpRequest, httpResponse);
		verify(requestContext, times(0)).dump();
	}

	@Test
	public void GivenEmptyUriRequest_WhenService_Then() throws Exception {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
			}
		};

		when(httpRequest.getRequestURI()).thenReturn("");

		// when
		exerciseService(servlet);

		// then
		verify(requestContext, times(1)).attach(httpRequest, httpResponse);
		verify(requestContext, times(0)).dump();
	}

	@Test
	public void GivenIOException_WhenService_Then() throws Exception {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
				throw new IOException("test exception");
			}
		};

		// when
		try {
			exerciseService(servlet);
		} catch (IOException e) {
			assertEquals("test exception", e.getMessage());
		}

		// then
		verify(requestContext, times(1)).attach(httpRequest, httpResponse);
		verify(requestContext, times(1)).dump();
	}

	@Test
	public void GivenRuntimeException_WhenService_Then() throws Exception {
		// given
		AppServlet servlet = new AppServlet() {
			@Override
			protected void handleRequest(RequestContext requestContext) throws IOException, ServletException {
				throw new IllegalArgumentException("test exception");
			}
		};

		// when
		try {
			exerciseService(servlet);
		} catch (IllegalArgumentException e) {
			assertEquals("test exception", e.getMessage());
		}

		// then
		verify(requestContext, times(1)).attach(httpRequest, httpResponse);
		verify(requestContext, times(1)).dump();
	}

	@Test
	public void emptyUriRequest() throws Exception {
		when(httpRequest.getMethod()).thenReturn("POST");
		assertFalse(isEmptyUriRequest(httpRequest));

		when(httpRequest.getMethod()).thenReturn("GET");
		when(httpRequest.getHeader("Referer")).thenReturn("/test/service");

		when(httpRequest.getRequestURI()).thenReturn("/test/query");
		assertFalse(isEmptyUriRequest(httpRequest));

		when(httpRequest.getRequestURI()).thenReturn("/test/service");
		assertTrue(isEmptyUriRequest(httpRequest));

		when(httpRequest.getRequestURI()).thenReturn("/test/service");
		when(httpRequest.getHeader("Accept")).thenReturn("text/xml");
		assertTrue(isEmptyUriRequest(httpRequest));

		when(httpRequest.getRequestURI()).thenReturn("/test/service");
		when(httpRequest.getHeader("Accept")).thenReturn("image/jpg");
		assertTrue(isEmptyUriRequest(httpRequest));

		// ------------------------------------------------------------------------------

		when(httpRequest.getRequestURI()).thenReturn("/test/service?qqq");
		when(httpRequest.getHeader("Referer")).thenReturn("/test/service?qqq");
		assertTrue(isEmptyUriRequest(httpRequest));

		when(httpRequest.getRequestURI()).thenReturn("/test/service?qqq");
		when(httpRequest.getQueryString()).thenReturn("qqq");
		when(httpRequest.getHeader("Referer")).thenReturn("/test/service?qqq");
		assertFalse(isEmptyUriRequest(httpRequest));

		when(httpRequest.getHeader("Referer")).thenReturn(null);
		assertFalse(isEmptyUriRequest(httpRequest));
	}

	private static boolean isEmptyUriRequest(HttpServletRequest httpRequest) throws Exception {
		return Classes.invoke(AppServlet.class, "isEmptyUriRequest", httpRequest);
	}

	/** For non XHR request unauthorized access send 401 and WWW-Authenticate set to basic. */
	@Test
	public void GivenLoginRealm_WhenSendUnauthorized_ThenBasicRealm() throws Exception {
		// given
		when(servletContext.getServletContextName()).thenReturn("Test App");
		
		// when
		AppServlet.sendUnauthorized(requestContext);

		// then
		verify(httpResponse, times(1)).setStatus(401);
		verify(httpResponse, times(1)).setHeader("WWW-Authenticate", "Basic realm=Test App");
		verify(requestContext, times(0)).dump();
	}

	@Test
	public void Given_WhenSendUnauthorized_ThenResponseCommited() throws Exception {
		// given
		requestContext.attach(httpRequest, httpResponse);

		// when
		AppServlet.sendUnauthorized(requestContext);

		// then
		verify(httpResponse, times(1)).setStatus(401);
		verify(httpResponse, times(1)).setHeader(eq("WWW-Authenticate"), anyString());
		verify(requestContext, times(0)).dump();
	}

	@Test
	public void Given_WhenSendNotFound_Then() throws Exception {
		// given
		String response = "{\"cause\":\"java.lang.NoSuchMethodException\",\"message\":\"js.test.Class#method\"}";
		when(json.stringify(any())).thenReturn(response);

		// when
		AppServlet.sendNotFound(requestContext, new NoSuchMethodException("js.test.Class#method"));

		// then
		verify(httpResponse, times(1)).setStatus(404);
		verify(requestContext, times(0)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void Given_WhenSendError_Then() throws Exception {
		// given
		String response = "{\"cause\":\"java.io.IOException\",\"message\":\"test exception\"}"; 
		when(json.stringify(any())).thenReturn(response);

		// when
		AppServlet.sendError(requestContext, new IOException("test exception"));

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(requestContext, times(1)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void Given_WhenSendError_ThenInvocationException() throws Exception {
		// given
		String response = "{\"cause\":\"java.io.IOException\",\"message\":\"test exception\"}";
		when(json.stringify(any())).thenReturn(response);

		// when
		AppServlet.sendError(requestContext, new InvocationException(new IOException("test exception")));

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(requestContext, times(1)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void Given_WhenSendError_ThenInvocationException_NullCause() throws Exception {
		// given
		String response = "{\"cause\":\"java.lang.Exception\",\"message\":\"exception\"}";
		when(json.stringify(any())).thenReturn(response);

		// when
		AppServlet.sendError(requestContext, new InvocationException(new Exception("exception")));

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(requestContext, times(1)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void Given_WhenSendError_ThenInvocationTargetException() throws Exception {
		// given
		String response = "{\"cause\":\"java.io.IOException\",\"message\":\"test exception\"}";
		when(json.stringify(any())).thenReturn(response);

		// when
		AppServlet.sendError(requestContext, new InvocationTargetException(new IOException("test exception")));

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(requestContext, times(1)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void Given_WhenSendError_ThenResponseCommited() throws Exception {
		// given
		when(json.stringify(any())).thenReturn("{}");

		// when
		AppServlet.sendError(requestContext, new IOException("test exception"));

		// then
		verify(httpResponse, times(1)).setStatus(500);
		verify(requestContext, times(1)).dump();
	}

	@Test
	public void Given_WhenSendError_ThendBusinessConstrain() throws Exception {
		// given
		String response = "{\"errorCode\":6500}";
		when(json.stringify(any())).thenReturn(response);

		// when
		AppServlet.sendError(requestContext, new BusinessException(0x1964));

		// then
		verify(httpResponse, times(1)).setStatus(400);
		verify(requestContext, times(0)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void Given_WhenSendJsonObject_ThenResponse() throws Exception {
		// given
		String response = "{\"text\":\"message text\"}";
		when(json.stringify(any())).thenReturn(response);

		class Message {
			private String text = "message text";
		}

		// when
		AppServlet.sendJsonObject(requestContext, new Message(), 200);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentLength(23);
		verify(httpResponse, times(1)).setContentType("application/json");
		verify(httpResponse, times(1)).setHeader("Content-Language", "en-US");
		verify(requestContext, times(0)).dump();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		verify(responseStream).write(bytesArg.capture());
		assertThat(new String(bytesArg.getValue()), equalTo(response));
	}

	@Test
	public void GivenResponseCommited_WhenSendJsonObject_ThenNoResponse() throws Exception {
		// given
		when(httpResponse.isCommitted()).thenReturn(true);

		// when
		AppServlet.sendJsonObject(requestContext, new Object(), 200);

		// then
		verify(httpResponse, times(0)).setStatus(anyInt());
		verify(httpResponse, times(0)).setContentLength(anyInt());
		verify(httpResponse, times(0)).setContentType(anyString());
		verify(httpResponse, times(0)).setHeader(eq("Content-Language"), anyString());
		verify(requestContext, times(0)).dump();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void exerciseService(AppServlet servlet) throws ServletException, IOException {
		servlet.init(servletConfig);
		servlet.service(httpRequest, httpResponse);
	}
}
