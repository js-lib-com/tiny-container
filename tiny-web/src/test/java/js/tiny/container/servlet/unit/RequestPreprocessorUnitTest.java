package js.tiny.container.servlet.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.RequestPreprocessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.unit.FilterConfigStub;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpServletResponseStub;
import js.tiny.container.unit.RequestDispatcherStub;
import js.tiny.container.unit.ServletContextStub;
import js.tiny.container.unit.TestContext;
import js.util.Classes;

public class RequestPreprocessorUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private IContainer container;

	@Before
	public void beforeTest() throws Exception {
		container = (IContainer) TestContext.start();
	}

	/** Loads internal lists from initial parameters. */
	@Test
	public void init() throws UnavailableException {
		MockFilterConfig config = new MockFilterConfig();
		config.servletContext.container = container;
		config.locale = "en,iw,ro";
		config.securityDomain = "admin,editor,viewer";

		RequestPreprocessor filter = new RequestPreprocessor();
		filter.init(config);

		List<String> locales = Classes.getFieldValue(filter, "locales");
		assertEquals(3, locales.size());
		assertEquals("en", locales.get(0));
		assertEquals("iw", locales.get(1));
		assertEquals("ro", locales.get(2));

		List<String> securityDomains = Classes.getFieldValue(filter, "securityDomains");
		assertEquals(3, securityDomains.size());
		assertEquals("admin", securityDomains.get(0));
		assertEquals("editor", securityDomains.get(1));
		assertEquals("viewer", securityDomains.get(2));
	}

	/** Initialize instance with empty locale and security domain lists. */
	@Test
	public void init_NullInitParam() throws UnavailableException {
		MockFilterConfig config = new MockFilterConfig();
		config.servletContext.container = container;

		RequestPreprocessor filter = new RequestPreprocessor();
		filter.init(config);

		List<String> locales = Classes.getFieldValue(filter, "locales");
		assertEquals(0, locales.size());

		List<String> securityDomains = Classes.getFieldValue(filter, "securityDomains");
		assertEquals(0, securityDomains.size());
	}

	@Test(expected = UnavailableException.class)
	public void init_UnavailableException() throws UnavailableException {
		RequestPreprocessor filter = new RequestPreprocessor();
		filter.init(new MockFilterConfig());
	}

	/** Filter conformity test. */
	@Test
	public void doFilter() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/iw/editor/controller/resource");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(Arrays.asList("en", "iw", "ro"), Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/controller/resource", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertEquals(new Locale("iw"), context.getLocale());
		assertEquals("editor", context.getSecurityDomain());
		assertEquals("/controller/resource", context.getRequestPath());
	}

	/** If request URI is for a static file redirect to filter chain. */
	@Test
	public void doFilter_ExistingFile() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/src/main/java/js/tiny/container/servlet/RequestPreprocessor.java");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(Arrays.asList("en", "iw", "ro"), Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/app/src/main/java/js/tiny/container/servlet/RequestPreprocessor.java", chain.requestURI);
	}

	/** If application is configured with locale and request URI locale is not found redirect to 404. */
	@Test
	public void doFilter_BadLocale() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/jp/admin/controller/resource");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(Arrays.asList("en", "iw", "ro"), Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/jp/admin/controller/resource", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertNull(context.getLocale());
		assertNull(context.getSecurityDomain());
		assertEquals("/jp/admin/controller/resource", context.getRequestPath());
	}

	/** Null request preprocessor should leave request URI unchanged. */
	@Test
	public void doFilter_NoLocale() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/jp/admin/controller/resource");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(null, null);
		filter.doFilter(request, response, chain);

		assertEquals("/jp/admin/controller/resource", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertNull(context.getLocale());
		assertNull(context.getSecurityDomain());
		assertEquals("/jp/admin/controller/resource", context.getRequestPath());
	}

	/** Request URI without security domain on a filter configured with security domains left unchanged. */
	@Test
	public void doFilter_MissingSecurityDomain() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/controller/resource");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(null, Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/controller/resource", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertNull(context.getLocale());
		assertNull(context.getSecurityDomain());
		assertEquals("/controller/resource", context.getRequestPath());
	}

	/** Request URI with not configured security domain should remain unchanged. */
	@Test
	public void doFilter_BadSecurityDomain() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/user/controller/resource");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(null, Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/user/controller/resource", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		Classes.setFieldValue(context, "attached", true);
		assertNull(context.getLocale());
		assertNull(context.getSecurityDomain());
		assertEquals("/user/controller/resource", context.getRequestPath());
	}

	@Test
	public void startsWith() throws Exception {
		assertTrue(startsWith("/admin/controller/resource", "admin"));
		assertTrue(startsWith("/Admin/controller/resource", "admin"));
		assertTrue(startsWith("/admin/controller/resource", "Admin"));

		assertFalse(startsWith("/controller/resource", "admin"));
		assertFalse(startsWith("/administrator/controller/resource", "admin"));
		assertFalse(startsWith("/admin/controller/resource", "administrator"));
		assertFalse(startsWith("/admin", "administrator"));
		assertFalse(startsWith("admin/controller/resource", "admin"));
	}

	private static boolean startsWith(String requestPath, String pathComponent) throws Exception {
		return Classes.invoke(RequestPreprocessor.class, "startsWith", requestPath, pathComponent);
	}

	/**
	 * REST servlet is always mapped by servlet path, that is, REST never uses extensions. For this reason {@link RestServlet}
	 * uses {@link HttpServletRequest#getPathInfo()} to locate resources. As a consequence
	 * {@link RequestContext#getRequestPath()} is not used and is initialized to the same value as request URI.
	 */
	@Test
	public void doFilter_REST() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/iw/editor/rest/controller/resource");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(Arrays.asList("en", "iw", "ro"), Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/rest/controller/resource", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertEquals(new Locale("iw"), context.getLocale());
		assertEquals("editor", context.getSecurityDomain());
		assertEquals("/rest/controller/resource", context.getRequestPath());
	}

	/** Conformity test for HTTP-RMI request with locale and security domain. */
	@Test
	public void doFilter_HttpRmi() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/iw/editor/js/captcha/Captcha/getSession.rmi");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(Arrays.asList("en", "iw", "ro"), Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/js/captcha/Captcha/getSession.rmi", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertEquals(new Locale("iw"), context.getLocale());
		assertEquals("editor", context.getSecurityDomain());
		assertEquals("/js/captcha/Captcha/getSession.rmi", context.getRequestPath());
	}

	/** Query parameters should be passed to both forwarded and request paths. */
	@Test
	public void doFilter_QueryParameters() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("/app/iw/editor/controller/resource?id=1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		Filter filter = getFilter(Arrays.asList("en", "iw", "ro"), Arrays.asList("admin", "editor", "viewer"));
		filter.doFilter(request, response, chain);

		assertEquals("/controller/resource?id=1", request.forwardPath);
		assertNull(chain.requestURI);

		RequestContext context = getRequestContext();
		assertEquals(new Locale("iw"), context.getLocale());
		assertEquals("editor", context.getSecurityDomain());
		assertEquals("/controller/resource?id=1", context.getRequestPath());
	}

	@Test
	public void destroy() {
		Filter filter = getFilter(null, null);
		filter.destroy();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private Filter getFilter(List<String> locales, List<String> securityDomains) {
		Filter filter = new RequestPreprocessor();
		Classes.setFieldValue(filter, "container", container);
		Classes.setFieldValue(filter, "locales", locales != null ? locales : Collections.emptyList());
		Classes.setFieldValue(filter, "securityDomains", securityDomains != null ? securityDomains : Collections.emptyList());
		return filter;
	}

	private RequestContext getRequestContext() {
		RequestContext context = container.getInstance(RequestContext.class);
		Classes.setFieldValue(context, "attached", true);
		return context;
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private class MockFilterConfig extends FilterConfigStub {
		private MockServletContext servletContext = new MockServletContext();
		private String locale;
		private String securityDomain;

		@Override
		public ServletContext getServletContext() {
			return servletContext;
		}

		@Override
		public String getInitParameter(String name) {
			switch (name) {
			case "locale":
				return locale;

			case "security-domain":
				return securityDomain;
			}
			return null;
		}
	}

	private class MockServletContext extends ServletContextStub {
		private IContainer container;

		@Override
		public Object getAttribute(String name) {
			return container;
		}

		@Override
		public String getRealPath(String resource) {
			return resource.substring(1);
		}
	}

	private class MockHttpServletRequest extends HttpServletRequestStub {
		private ServletContext context = new MockServletContext();
		private RequestDispatcher dispatcher = new MockRequestDispatcher();
		private String requestURI;
		private String forwardPath;

		public MockHttpServletRequest(String requestURI) {
			super();
			this.requestURI = requestURI;
		}

		@Override
		public ServletContext getServletContext() {
			return context;
		}

		@Override
		public String getRequestURI() {
			return requestURI;
		}

		@Override
		public String getQueryString() {
			return null;
		}

		@Override
		public String getContextPath() {
			return "/app";
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String path) {
			forwardPath = path;
			return dispatcher;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
	}

	private static class MockFilterChain implements FilterChain {
		private String requestURI;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
			requestURI = ((HttpServletRequest) request).getRequestURI();
		}
	}

	private static class MockRequestDispatcher extends RequestDispatcherStub {
		@Override
		public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		}
	}
}
