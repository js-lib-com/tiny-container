package js.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import js.net.EventStream;
import js.net.EventStreamManagerSPI;
import js.net.EventStreamServlet;
import js.servlet.AppServlet;
import js.servlet.RequestContext;
import js.servlet.TinyContainer;
import js.test.stub.ContainerSpiStub;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.unit.ServletConfigStub;
import js.unit.ServletContextStub;
import js.util.Classes;
import js.util.Strings;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EventStreamServletUnitTest {
	private MockContainer container;

	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;

	private MockServletConfig config;
	private RequestContext context;
	private EventStreamServlet servlet;

	@Before
	public void beforeTest() throws UnavailableException {
		container = new MockContainer();

		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();

		config = new MockServletConfig();
		config.context.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		context = new RequestContext(container);
	}

	@Test
	public void constructor() {
		servlet = new EventStreamServlet();
		assertNull(Classes.getFieldValue(servlet, "eventStreamManager"));
	}

	@Test
	public void init() throws UnavailableException {
		servlet = new EventStreamServlet();
		servlet.init(config);
		assertNotNull(Classes.getFieldValue(servlet, AppServlet.class, "container"));
		assertNotNull(Classes.getFieldValue(servlet, "eventStreamManager"));
	}

	@Test
	public void getEventStreamSessionID() throws Exception {
		assertEquals("a4a7c091232348a1940ae08745416cff", getEventStreamSessionID("/a4a7c091232348a1940ae08745416cff.event"));
		assertEquals("a4a7c091232348a1940ae08745416cff", getEventStreamSessionID("/admin/a4a7c091232348a1940ae08745416cff.event"));
		assertEquals("a4a7c091232348a1940ae08745416cff", getEventStreamSessionID("/event/a4a7c091232348a1940ae08745416cff"));
		assertEquals("a4a7c091232348a1940ae08745416cff", getEventStreamSessionID("/admin/event/a4a7c091232348a1940ae08745416cff"));
		assertEquals("a4a7c091232348a1940ae08745416cff", getEventStreamSessionID("/a4a7c091232348a1940ae08745416cff"));
		assertEquals("a4a7c091232348a1940ae08745416cff", getEventStreamSessionID("/a4a7c091232348a1940ae08745416cff."));
	}

	@Test
	public void handleRequest() throws Exception {
		class MockEventStream extends EventStream {
			private int setRemoteHostProbe;
			private int setWriterProbe;
			private int loopProbe;
			private int onOpenProbe;
			private int onCloseProbe;

			@Override
			protected void setRemoteHost(String remoteHost) {
				super.setRemoteHost(remoteHost);
				++setRemoteHostProbe;
			}

			@Override
			protected void setWriter(PrintWriter writer) {
				super.setWriter(writer);
				++setWriterProbe;
			}

			@Override
			protected boolean loop() {
				return ++loopProbe != 2;
			}

			@Override
			protected void onOpen() {
				super.onOpen();
				++onOpenProbe;
			}

			@Override
			protected void onClose() {
				super.onClose();
				++onCloseProbe;
			}
		}
		MockEventStream eventStream = new MockEventStream();
		container.eventStreamManager.eventStream = eventStream;

		servlet = new EventStreamServlet();
		executeRequestHandler();

		assertEquals(5, httpResponse.headers.size());
		assertEquals("text/event-stream;charset=UTF-8", httpResponse.headers.get("Content-Type"));
		assertEquals("no-cache;no-store", httpResponse.headers.get("Cache-Control"));
		assertEquals("no-cache", httpResponse.headers.get("Pragma"));
		assertEquals("Thu, 01 Jan 1970 02:00:00 GMT", httpResponse.headers.get("Expires"));
		assertEquals("keep-alive", httpResponse.headers.get("Connection"));

		assertEquals(1, eventStream.setRemoteHostProbe);
		assertEquals(1, eventStream.setWriterProbe);
		assertEquals(2, eventStream.loopProbe);
		assertEquals(1, eventStream.onOpenProbe);
		assertEquals(1, eventStream.onCloseProbe);

		assertEquals(1, container.eventStreamManager.createEventStreamProbe);
		assertEquals(1, container.eventStreamManager.closeEventStreamProbe);
	}

	@Test
	public void handleRequest_EventStreamClose() throws Exception {
		class MockEventStream extends EventStream {
			private int onCloseProbe;

			@Override
			protected void onClose() {
				super.onClose();
				++onCloseProbe;
			}
		}
		MockEventStream eventStream = new MockEventStream();
		container.eventStreamManager.eventStream = eventStream;

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				container.eventStreamManager.eventStream.close();
			}
		});
		thread.start();

		servlet = new EventStreamServlet();
		executeRequestHandler();

		assertEquals(1, eventStream.onCloseProbe);
		assertEquals(1, container.eventStreamManager.createEventStreamProbe);
		assertEquals(1, container.eventStreamManager.closeEventStreamProbe);
	}

	/** Bad session ID is considered bad request 400. */
	@Test
	public void handleRequest_BadSessionID() throws Exception {
		servlet = new EventStreamServlet();
		executeRequestHandler();

		assertEquals(400, httpResponse.statusCode);
		assertEquals("/test-app/1234.event", httpResponse.writer.toString());
	}

	/** SocketException from loop should be recorder to logger and finally block should close event stream. */
	@Test
	public void handleRequest_SocketException() throws Exception {
		class MockEventStream extends EventStream {
			private int onCloseProbe;

			@Override
			protected void onClose() {
				super.onClose();
				++onCloseProbe;
			}

			@Override
			protected boolean loop() {
				return false;
			}
		}
		MockEventStream eventStream = new MockEventStream();
		container.eventStreamManager.eventStream = eventStream;

		servlet = new EventStreamServlet();
		executeRequestHandler();

		assertEquals(1, eventStream.onCloseProbe);
		assertEquals(1, container.eventStreamManager.createEventStreamProbe);
		assertEquals(1, container.eventStreamManager.closeEventStreamProbe);
	}

	/** Runtime exceptions from loop should bubbled up and finally block should close event stream. */
	@Test(expected = RuntimeException.class)
	public void handleRequest_IOException() throws Exception {
		class MockEventStream extends EventStream {
			private int onCloseProbe;

			@Override
			protected void onClose() {
				super.onClose();
				++onCloseProbe;
			}

			@Override
			protected boolean loop() {
				throw new RuntimeException();
			}
		}
		MockEventStream eventStream = new MockEventStream();
		container.eventStreamManager.eventStream = eventStream;

		servlet = new EventStreamServlet();
		executeRequestHandler();

		assertEquals(1, eventStream.onCloseProbe);
		assertEquals(1, container.eventStreamManager.createEventStreamProbe);
		assertEquals(1, container.eventStreamManager.closeEventStreamProbe);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String getEventStreamSessionID(String requestURI) throws Exception {
		return Classes.invoke(EventStreamServlet.class, "getEventStreamSessionID", requestURI);
	}

	private void executeRequestHandler() throws Exception {
		servlet.init(config);
		context.attach(httpRequest, httpResponse);
		Classes.invoke(servlet, "handleRequest", context);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends ContainerSpiStub {
		private MockEventStreamManager eventStreamManager = new MockEventStreamManager();

		@Override
		public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
			return (T) eventStreamManager;
		}
	}

	private static class MockEventStreamManager implements EventStreamManagerSPI {
		private EventStream eventStream;
		private int createEventStreamProbe;
		private int closeEventStreamProbe;

		@Override
		public EventStream createEventStream(String sessionID) {
			++createEventStreamProbe;
			return eventStream;
		}

		@Override
		public void destroyEventStream(EventStream eventStream) {
			++closeEventStreamProbe;
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		@Override
		public String getRequestURI() {
			return "/test-app/1234.event";
		}

		@Override
		public String getContextPath() {
			return "/test-app";
		}

		@Override
		public String getMethod() {
			return "POST";
		}

		@Override
		public String getQueryString() {
			return null;
		}

		@Override
		public Enumeration getHeaderNames() {
			return Collections.emptyEnumeration();
		}

		@Override
		public Locale getLocale() {
			return Locale.US;
		}

		@Override
		public String getRemoteHost() {
			return "localhost";
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private Map<String, String> headers = new HashMap<>();
		private int statusCode;
		private StringWriter writer = new StringWriter();

		@Override
		public void setContentType(String type) {
			headers.put("Content-Type", type);
		}

		@Override
		public void setHeader(String name, String value) {
			headers.put(name, value);
		}

		@Override
		public void addHeader(String name, String value) {
			if (headers.containsKey(name)) {
				value = Strings.concat(headers.get(name), ';', value);
			}
			headers.put(name, value);
		}

		@Override
		public void setDateHeader(String name, long value) {
			DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
			headers.put(name, dateFormat.format(new Date(value)));
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return new PrintWriter(writer);
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			statusCode = sc;
			writer.write(msg);
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
	}
}
