package js.servlet.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import js.lang.BugError;
import js.servlet.Cookies;
import js.servlet.RequestContext;
import js.test.stub.ContainerStub;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.unit.HttpSessionStub;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({ "rawtypes" })
public class RequestContextUnitTest {
	private MockContainer container;
	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;

	@Before
	public void beforeTest() {
		container = new MockContainer();
		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();
	}

	@Test
	public void constructor() {
		RequestContext context = new RequestContext(container);
		assertEquals(container, context.getContainer());
		assertFalse(context.isAttached());
	}

	@Test
	public void locale_LoadFromRequestPreprocessor() {
		RequestContext context = new RequestContext(container);
		context.setLocale(Locale.US);
		context.attach(httpRequest, httpResponse);
		assertEquals(Locale.US, context.getLocale());
	}

	@Test
	public void locale_LoadFromHttpRequest() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals(Locale.UK, context.getLocale());
	}

	@Test
	public void securityDomain() {
		RequestContext context = new RequestContext(container);
		context.setSecurityDomain("admin");
		context.attach(httpRequest, httpResponse);
		assertEquals("admin", context.getSecurityDomain());
	}

	@Test
	public void requestPath_LoadFromRequestPreprocessor() {
		RequestContext context = new RequestContext(container);
		context.setRequestPath("/controller/index");
		context.attach(httpRequest, httpResponse);
		assertEquals("/controller/index", context.getRequestPath());
	}

	@Test
	public void requestPath_LoadFromHttpRequest() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals("/index.htm", context.getRequestPath());
	}

	@Test
	public void requestURI() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals("/test-app/index.htm", context.getRequestURI());
	}

	@Test
	public void cookies() {
		RequestContext context = new RequestContext(container);
		assertNull(Classes.getFieldValue(context, "cookies"));
		context.attach(httpRequest, httpResponse);
		assertNotNull(context.getCookies());
		assertNotNull(context.getCookies());
	}

	@Test
	public void localName() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals("localhost", context.getLocalName());
	}

	@Test
	public void localPort() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals(80, context.getLocalPort());
	}

	@Test
	public void remoteAddr() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals("server.com", context.getRemoteHost());
	}

	@Test
	public void remotePort() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals(1964, context.getRemotePort());
	}

	@Test
	public void request() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals(httpRequest, context.getRequest());
	}

	@Test
	public void response() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals(httpResponse, context.getResponse());
	}

	@Test
	public void session() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		assertEquals(MockHttpSession.class, context.getSession().getClass());
		assertNull(context.getSession(false));
		assertEquals(MockHttpSession.class, context.getSession(true).getClass());
	}

	@Test
	public void detach() {
		RequestContext context = new RequestContext(container);
		Classes.setFieldValue(context, "attached", true);
		Classes.setFieldValue(context, "locale", Locale.ENGLISH);
		Classes.setFieldValue(context, "securityDomain", "admin");
		Classes.setFieldValue(context, "cookies", new Cookies(httpRequest, httpResponse));
		Classes.setFieldValue(context, "requestPath", "/index.htm");

		context.detach();
		assertFalse((boolean) Classes.getFieldValue(context, "attached"));
		assertNull(Classes.getFieldValue(context, "locale"));
		assertNull(Classes.getFieldValue(context, "securityDomain"));
		assertNull(Classes.getFieldValue(context, "cookies"));
		assertNull(Classes.getFieldValue(context, "requestPath"));
	}

	@Test
	public void dump() {
		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);
		context.dump();
	}

	// --------------------------------------------------------------------------------------------
	// NOT ATTACHED

	@Test(expected = BugError.class)
	public void locale_Detached() {
		RequestContext context = new RequestContext(container);
		context.getLocale();
	}

	@Test(expected = BugError.class)
	public void securityDomain_Detached() {
		RequestContext context = new RequestContext(container);
		context.getSecurityDomain();
	}

	@Test(expected = BugError.class)
	public void requestPath_Detached() {
		RequestContext context = new RequestContext(container);
		context.getRequestPath();
	}

	@Test(expected = BugError.class)
	public void requestURI_Detached() {
		RequestContext context = new RequestContext(container);
		context.getRequestURI();
	}

	@Test(expected = BugError.class)
	public void cookies_Detached() {
		RequestContext context = new RequestContext(container);
		assertNotNull(context.getCookies());
	}

	@Test(expected = BugError.class)
	public void localAddr_Detached() {
		RequestContext context = new RequestContext(container);
		context.getLocalName();
	}

	@Test(expected = BugError.class)
	public void localPort_Detached() {
		RequestContext context = new RequestContext(container);
		context.getLocalPort();
	}

	@Test(expected = BugError.class)
	public void remoteAddr_Detached() {
		RequestContext context = new RequestContext(container);
		context.getRemoteHost();
	}

	@Test(expected = BugError.class)
	public void remotePort_Detached() {
		RequestContext context = new RequestContext(container);
		context.getRemotePort();
	}

	@Test(expected = BugError.class)
	public void request_Detached() {
		RequestContext context = new RequestContext(container);
		context.getRequest();
	}

	@Test(expected = BugError.class)
	public void response_Detached() {
		RequestContext context = new RequestContext(container);
		context.getResponse();
	}

	@Test(expected = BugError.class)
	public void session_Detached() {
		RequestContext context = new RequestContext(container);
		context.getSession();
	}

	@Test
	public void dump_Detached() {
		RequestContext context = new RequestContext(container);
		context.dump();
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	class MockContainer extends ContainerStub {

	}

	class MockHttpServletRequest extends HttpServletRequestStub {
		private String requestURI = "/test-app/index.htm";
		private String contextPath = "/test-app";

		@Override
		public String getMethod() {
			return "GET";
		}

		@Override
		public String getRequestURI() {
			return requestURI;
		}

		@Override
		public String getContextPath() {
			return contextPath;
		}

		@Override
		public String getQueryString() {
			return null;
		}

		@Override
		public Enumeration getHeaderNames() {
			return Collections.enumeration(Arrays.asList("Host"));
		}

		@Override
		public String getHeader(String name) {
			return "server.com";
		}

		@Override
		public Locale getLocale() {
			return Locale.UK;
		}

		@Override
		public Cookie[] getCookies() {
			return new Cookie[0];
		}

		@Override
		public String getLocalName() {
			return "localhost";
		}

		@Override
		public int getLocalPort() {
			return 80;
		}

		@Override
		public int getRemotePort() {
			return 1964;
		}

		@Override
		public String getRemoteHost() {
			return "server.com";
		}

		@Override
		public HttpSession getSession() {
			return new MockHttpSession();
		}

		@Override
		public HttpSession getSession(boolean create) {
			return create ? new MockHttpSession() : null;
		}
	}

	class MockHttpServletResponse extends HttpServletResponseStub {

	}

	class MockHttpSession extends HttpSessionStub {

	}
}
