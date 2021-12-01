package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class RequestContextUnitTest {
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private HttpSession httpSession;

	@Mock
	private ITinyContainer container;

	@Before
	public void beforeTest() {
		when(httpRequest.getRequestURI()).thenReturn("/test-app/index.htm");
		when(httpRequest.getContextPath()).thenReturn("/test-app");
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
		when(httpRequest.getLocale()).thenReturn(Locale.UK);

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
		when(httpRequest.getLocalName()).thenReturn("localhost");

		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);

		assertEquals("localhost", context.getLocalName());
	}

	@Test
	public void localPort() {
		when(httpRequest.getLocalPort()).thenReturn(80);

		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);

		assertEquals(80, context.getLocalPort());
	}

	@Test
	public void remoteAddr() {
		when(httpRequest.getRemoteHost()).thenReturn("server.com");

		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);

		assertEquals("server.com", context.getRemoteHost());
	}

	@Test
	public void remotePort() {
		when(httpRequest.getRemotePort()).thenReturn(1964);

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
		when(httpRequest.getSession()).thenReturn(httpSession);
		when(httpRequest.getSession(true)).thenReturn(httpSession);

		RequestContext context = new RequestContext(container);
		context.attach(httpRequest, httpResponse);

		assertEquals(httpSession, context.getSession());
		assertNull(context.getSession(false));
		assertEquals(httpSession, context.getSession(true));
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
		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

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
}
