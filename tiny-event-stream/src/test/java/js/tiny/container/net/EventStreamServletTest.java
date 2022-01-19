package js.tiny.container.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.Principal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamServletTest {
	@Mock
	private ITinyContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private ServletContext servletContext;
	@Mock
	private ServletConfig servletConfig;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;
	@Mock
	private EventStreamManagerImpl streamManager;
	@Mock
	private EventStream eventStream;
	@Mock
	private PrintWriter writer;
	@Mock
	private Principal principal;

	private EventStreamServlet servlet;

	@Before
	public void beforeTest() throws ServletException {
		when(servletConfig.getServletName()).thenReturn("resource-servlet");
		when(servletConfig.getServletContext()).thenReturn(servletContext);

		when(servletContext.getServletContextName()).thenReturn("test-app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(requestContext.getRequest()).thenReturn(request);
		when(requestContext.getResponse()).thenReturn(response);

		when(container.getInstance(EventStreamManager.class)).thenReturn(streamManager);

		servlet = new EventStreamServlet();
		servlet.init(servletConfig);
	}

	@Test
	public void init() throws UnavailableException {
		assertNotNull(Classes.getFieldValue(servlet, AppServlet.class, "container"));
		assertNotNull(Classes.getFieldValue(servlet, "eventStreamManager"));
	}

	/**
	 * Conformity test with null principal and no configuration object. Event stream is created, opened, its loop invoked once,
	 * closed and destroyed.
	 */
	@Test
	public void handleRequest() throws Exception {
		when(requestContext.getRemoteHost()).thenReturn("192.168.1.10");
		when(streamManager.createEventStream(null, null)).thenReturn(eventStream);
		when(response.getWriter()).thenReturn(writer);

		handleRequest(requestContext);

		verify(response, times(1)).setContentType("text/event-stream;charset=UTF-8");
		verify(response, times(1)).setHeader("Cache-Control", "no-cache");
		verify(response, times(1)).addHeader("Cache-Control", "no-store");
		verify(response, times(1)).setHeader("Pragma", "no-cache");
		verify(response, times(1)).setDateHeader("Expires", 0);
		verify(response, times(1)).setHeader("Connection", "keep-alive");

		verify(eventStream, times(1)).setRemoteHost("192.168.1.10");
		verify(eventStream, times(1)).setWriter(writer);
		verify(eventStream, times(1)).onOpen();
		verify(eventStream, times(1)).loop();
		verify(eventStream, times(1)).onClose();

		ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
		ArgumentCaptor<EventStreamConfig> configCaptor = ArgumentCaptor.forClass(EventStreamConfig.class);
		ArgumentCaptor<EventStream> eventStreamCaptor = ArgumentCaptor.forClass(EventStream.class);

		verify(streamManager, times(1)).createEventStream(principalCaptor.capture(), configCaptor.capture());
		verify(streamManager, times(1)).destroyEventStream(eventStreamCaptor.capture());

		assertThat(principalCaptor.getValue(), nullValue());
		assertThat(configCaptor.getValue(), nullValue());
		assertThat(eventStreamCaptor.getValue(), equalTo(eventStream));
	}

	/** Test that if container has user principal it is passed to event stream factory method. */
	@Test
	public void handleRequest_Principal() throws Exception {
		when(streamManager.createEventStream(principal, null)).thenReturn(eventStream);
		when(container.getUserPrincipal()).thenReturn(principal);

		handleRequest(requestContext);

		ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
		ArgumentCaptor<EventStreamConfig> configCaptor = ArgumentCaptor.forClass(EventStreamConfig.class);

		verify(streamManager, times(1)).createEventStream(principalCaptor.capture(), configCaptor.capture());

		assertThat(principalCaptor.getValue(), equalTo(principal));
		assertThat(configCaptor.getValue(), nullValue());
	}

	@Test
	public void handleRequest_Config() throws Exception {
		String body = "{\"keepAlivePeriod\":20000}";

		when(streamManager.createEventStream(eq(principal), any(EventStreamConfig.class))).thenReturn(eventStream);

		when(container.getUserPrincipal()).thenReturn(principal);
		when(request.getContentLength()).thenReturn(body.length());
		when(request.getMethod()).thenReturn("POST");
		when(request.getContentType()).thenReturn("application/json;charset=UTF-8");
		when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));

		handleRequest(requestContext);

		ArgumentCaptor<EventStreamConfig> configCaptor = ArgumentCaptor.forClass(EventStreamConfig.class);
		verify(streamManager, times(1)).createEventStream(eq(principal), configCaptor.capture());
		assertThat(configCaptor.getValue(), notNullValue());
		assertThat(configCaptor.getValue().getKeepAlivePeriod(), equalTo(20000));
	}

	/** Runtime time exception thrown by event stream loop is bubbled up by event stream servlet. */
	@Test(expected = RuntimeException.class)
	public void handleRequest_RuntimeException() throws Exception {
		when(streamManager.createEventStream(null, null)).thenReturn(eventStream);
		when(eventStream.loop()).thenThrow(new RuntimeException());
		handleRequest(requestContext);
	}

	// --------------------------------------------------------------------------------------------

	private void handleRequest(RequestContext context) throws Exception {
		Classes.invoke(servlet, "handleRequest", context);
	}
}
