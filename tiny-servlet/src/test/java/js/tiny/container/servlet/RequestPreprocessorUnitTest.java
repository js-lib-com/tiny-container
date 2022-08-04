package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import js.tiny.container.spi.IContainer;

@RunWith(MockitoJUnitRunner.class)
public class RequestPreprocessorUnitTest {
	@Mock
	private ServletContext servletContext;
	@Mock
	private RequestDispatcher requestDispatcher;
	@Mock
	private FilterChain filterChain;
	@Mock
	private FilterConfig filterConfig;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	@Mock
	private RequestContext requestContext;
	@Mock
	private IContainer container;

	private RequestPreprocessor preprocessor;

	@Before
	public void beforeTest() throws Exception {
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);
		when(servletContext.getRealPath(any())).thenReturn("");

		when(filterConfig.getServletContext()).thenReturn(servletContext);
		when(filterConfig.getInitParameter("locale")).thenReturn("en,iw,ro");
		when(filterConfig.getInitParameter("security-domain")).thenReturn("admin,editor,viewer");

		when(httpRequest.getServletContext()).thenReturn(servletContext);
		when(httpRequest.getRequestDispatcher(any())).thenReturn(requestDispatcher);
		when(httpRequest.getContextPath()).thenReturn("/app");

		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);

		preprocessor = new RequestPreprocessor();
	}

	/** Loads internal lists from initial parameters. */
	@Test
	public void GivenInitParameters_WhenInit_ThenLoadLocalesAndSecurity() throws UnavailableException {
		// given
		
		// when
		preprocessor.init(filterConfig);

		// then
		List<String> locales = preprocessor.locales();
		assertEquals(3, locales.size());
		assertEquals("en", locales.get(0));
		assertEquals("iw", locales.get(1));
		assertEquals("ro", locales.get(2));

		List<String> securityDomains = preprocessor.securityDomains();
		assertEquals(3, securityDomains.size());
		assertEquals("admin", securityDomains.get(0));
		assertEquals("editor", securityDomains.get(1));
		assertEquals("viewer", securityDomains.get(2));
	}

	@Test
	public void GivenLocaleWithSpaces_WhenInit_ThenLoadLocales() throws UnavailableException {
		// given
		when(filterConfig.getInitParameter("locale")).thenReturn("en, iw, ro");
		
		// when
		preprocessor.init(filterConfig);

		// then
		List<String> locales = preprocessor.locales();
		assertEquals(3, locales.size());
		assertEquals("en", locales.get(0));
		assertEquals("iw", locales.get(1));
		assertEquals("ro", locales.get(2));
	}

	@Test
	public void GivenSecurityWithSpaces_WhenInit_ThenLoadSecurity() throws UnavailableException {
		// given
		when(filterConfig.getInitParameter("security-domain")).thenReturn("admin, editor, viewer");
		
		// when
		preprocessor.init(filterConfig);

		// then
		List<String> securityDomains = preprocessor.securityDomains();
		assertEquals(3, securityDomains.size());
		assertEquals("admin", securityDomains.get(0));
		assertEquals("editor", securityDomains.get(1));
		assertEquals("viewer", securityDomains.get(2));
	}


	/** Initialize instance with empty locale and security domain lists. */
	@Test
	public void GivenNullInitParameters_WhenInit_ThenEmptyLocaleAndSecurity() throws UnavailableException {
		// given
		when(filterConfig.getInitParameter("locale")).thenReturn(null);
		when(filterConfig.getInitParameter("security-domain")).thenReturn(null);
		
		// when
		preprocessor.init(filterConfig);

		// then
		assertEquals(0, preprocessor.locales().size());
		assertEquals(0, preprocessor.securityDomains().size());
	}

	@Test(expected = UnavailableException.class)
	public void GivenMissingContainer_WhenInit_ThenException() throws UnavailableException {
		// given
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(null);

		// when
		preprocessor.init(filterConfig);

		// then
	}

	/** Filter conformity test. */
	@Test
	public void GivenAllGood_WhenDoFilter_ThenUpdateRequestContext() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/iw/editor/controller/resource");
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/controller/resource");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(1)).setLocale(new Locale("iw"));
		verify(requestContext, times(1)).setSecurityDomain("editor");
		verify(requestContext, times(1)).setRequestPath("/controller/resource");
	}

	/** If httpRequest URI is for a static file redirect to filter chain. */
	@Test
	public void GivenStaticFileRequest_WhenDoFilter_ThenFilterChain() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/src/test/resources/log4j.properties");
		when(servletContext.getRealPath(any())).thenReturn("src/test/resources/log4j.properties");
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(filterChain, times(1)).doFilter(httpRequest, httpResponse);
	}

	/** If application is configured with locale and httpRequest URI locale is not found do not set request context locale. */
	@Test
	public void GivenBadLocale_WhenDoFilter_ThenDoNotSetRequestContextLocale() throws IOException, ServletException {
		// given jp locale not configured on init parameter
		when(httpRequest.getRequestURI()).thenReturn("/app/jp/admin/controller/resource");
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/jp/admin/controller/resource");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(0)).setLocale(any());
		// TODO: if locale not found security domain cannot be processed
		// there is security domain but because locale is not recognized security domain cannot be processed
		verify(requestContext, times(0)).setSecurityDomain(any());
		verify(requestContext, times(1)).setRequestPath("/jp/admin/controller/resource");
	}

	/** Null httpRequest preprocessor should leave httpRequest URI unchanged. */
	@Test
	public void GivenNullInitParameters_WhenDoFilter_ThenRequestUriNotChanged() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/jp/admin/controller/resource");
		when(filterConfig.getInitParameter("locale")).thenReturn(null);
		when(filterConfig.getInitParameter("security-domain")).thenReturn(null);
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/jp/admin/controller/resource");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(0)).setLocale(any());
		verify(requestContext, times(0)).setSecurityDomain(any());
		verify(requestContext, times(1)).setRequestPath("/jp/admin/controller/resource");
	}

	/** Request URI without security domain on a filter configured with security domains left unchanged. */
	@Test
	public void GivenNullSecurityInitParameters_WhenDoFilter_ThenDoNotSetRequestContext() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/controller/resource");
		when(filterConfig.getInitParameter("locale")).thenReturn(null);
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/controller/resource");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(0)).setLocale(any());
		verify(requestContext, times(0)).setSecurityDomain(any());
		verify(requestContext, times(1)).setRequestPath("/controller/resource");
	}

	/** Request URI with not configured security domain should remain unchanged. */
	@Test
	public void GivenMissingSecurity_WhenDoFilter_ThenRequestUriNotChanged() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/user/controller/resource");
		when(filterConfig.getInitParameter("locale")).thenReturn(null);
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/user/controller/resource");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(0)).setLocale(any());
		verify(requestContext, times(0)).setSecurityDomain(any());
		verify(requestContext, times(1)).setRequestPath("/user/controller/resource");
	}

	@Test
	public void GivenValidRequestURI_WhenStartsWith_ThenTrue() throws Exception {
		assertTrue(RequestPreprocessor.startsWith("/admin/controller/resource", "admin"));
		assertTrue(RequestPreprocessor.startsWith("/Admin/controller/resource", "admin"));
		assertTrue(RequestPreprocessor.startsWith("/admin/controller/resource", "Admin"));
	}

	@Test
	public void GivenInvalidRequestURI_WhenStartsWith_ThenFalse() throws Exception {
		assertFalse(RequestPreprocessor.startsWith("/controller/resource", "admin"));
		assertFalse(RequestPreprocessor.startsWith("/administrator/controller/resource", "admin"));
		assertFalse(RequestPreprocessor.startsWith("/admin/controller/resource", "administrator"));
		assertFalse(RequestPreprocessor.startsWith("/admin", "administrator"));
		assertFalse(RequestPreprocessor.startsWith("admin/controller/resource", "admin"));
	}

	/**
	 * REST servlet is always mapped by servlet path, that is, REST never uses extensions. For this reason {@link RestServlet}
	 * uses {@link HttpServletRequest#getPathInfo()} to locate resources. As a consequence
	 * {@link RequestContext#getRequestPath()} is not used and is initialized to the same value as httpRequest URI.
	 */
	@Test
	public void GivenRestRequestWithLocaleAndSecurity_WhenDoFilter_ThenRestPath() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/iw/editor/rest/controller/resource");
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/rest/controller/resource");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(1)).setLocale(new Locale("iw"));
		verify(requestContext, times(1)).setSecurityDomain("editor");
		verify(requestContext, times(1)).setRequestPath("/rest/controller/resource");
	}

	/** Conformity test for HTTP-RMI httpRequest with locale and security domain. */
	@Test
	public void GivenHttpRmiRequestWithLocaleAndSecurity_WhenDoFilter_ThenHttpRmiPath() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/iw/editor/js/captcha/Captcha/getSession.rmi");
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/js/captcha/Captcha/getSession.rmi");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(1)).setLocale(new Locale("iw"));
		verify(requestContext, times(1)).setSecurityDomain("editor");
		verify(requestContext, times(1)).setRequestPath("/js/captcha/Captcha/getSession.rmi");
	}

	/** Query parameters should be passed to both forwarded and httpRequest paths. */
	@Test
	public void GivenQueryParametersWithLocaleAndSecurity_WhenDoFilter_ThenPreserveQueryParameters() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/app/iw/editor/controller/resource?id=1");
		preprocessor.init(filterConfig);

		// when
		preprocessor.doFilter(httpRequest, httpResponse, filterChain);

		// then
		verify(httpRequest, times(1)).getRequestDispatcher("/controller/resource?id=1");
		verify(filterChain, times(0)).doFilter(httpRequest, httpResponse);

		verify(requestContext, times(1)).setLocale(new Locale("iw"));
		verify(requestContext, times(1)).setSecurityDomain("editor");
		verify(requestContext, times(1)).setRequestPath("/controller/resource?id=1");
	}

	@Test
	public void Given_WhenDestroy_Then() {
		// given
		
		// when
		preprocessor.destroy();
		
		// then
	}
}
