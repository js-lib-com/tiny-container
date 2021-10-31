package js.tiny.container.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;

@RunWith(MockitoJUnitRunner.class)
public class TinySecurityContextTest {
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private HttpSession httpSession;

	@Mock
	private RequestContext requestContext;

	private SecurityContextProvider security;

	@Before
	public void beforeTest() {
		when(requestContext.getRequest()).thenReturn(httpRequest);
		when(requestContext.getResponse()).thenReturn(httpResponse);
		when(httpRequest.getSession(true)).thenReturn(httpSession);

		security = new TinySecurityContext();
	}

	/** Login using servlet container provided authentication should write credentials to HTTP response cookies. */
	@Test
	public void GivenHttpRequestLogin_WhenLoginWithCredentials_ThenInvokeAuthenticate() throws ServletException, IOException {
		// given

		// when
		security.login(requestContext, "user", "passwd");

		// then
		verify(httpRequest, times(1)).login(anyString(), anyString());
		verify(httpRequest, times(1)).authenticate(httpResponse);
	}

	/** Fail to login using servlet container provided authentication should not write credentials to HTTP request cookies. */
	@Test
	public void GivenHttpRequestLoginFail_WhenLoginWithCredentials_ThenDoNotInvokeAuthenticate() throws ServletException, IOException {
		// given
		doThrow(ServletException.class).when(httpRequest).login(anyString(), anyString());

		// when
		security.login(requestContext, "user", "passwd");

		// then
		verify(httpRequest, times(1)).login(anyString(), anyString());
		verify(httpRequest, times(0)).authenticate(httpResponse);
	}

	/** Login using servlet container provided authentication should not store anything on session attributes. */
	@Test
	public void GivenHttpRequestLogin_WhenLoginWithCredentials_ThenDoNotSetSessionAttribute() throws ServletException, IOException {
		// given

		// when
		security.login(requestContext, "user", "passwd");

		// then
		verify(httpSession, times(0)).setAttribute(eq(TinyContainer.ATTR_PRINCIPAL), any());
	}

	/** If login with principal, store it on HTTP session attributes but do not set session maximum activity interval. */
	@Test
	public void GivenPrincipal_WhenLoginPrincipal_ThenSetSessionAttribute() {
		// given
		Principal principal = mock(Principal.class);

		// when
		security.login(requestContext, principal);

		// then
		verify(httpSession, times(1)).setAttribute(TinyContainer.ATTR_PRINCIPAL, principal);
		verify(httpSession, times(0)).setMaxInactiveInterval(anyInt());
	}

	@Test(expected = BugError.class)
	public void GivenNoRequest_WhenLoginPrincipal_ThenException() {
		// given
		when(requestContext.getRequest()).thenReturn(null);
		Principal principal = mock(Principal.class);

		// when
		security.login(requestContext, principal);

		// then
	}

	/** If login with nonce user store it on HTTP session attribute and set session maximum activity interval. */
	@Test
	public void GivenNoncePrincipal_WhenLoginPrincipal_ThenSetSessionMaxActivityInterval() {
		// given
		NonceUser nonce = mock(NonceUser.class);

		// when
		security.login(requestContext, nonce);

		// then
		verify(httpSession, times(1)).setAttribute(TinyContainer.ATTR_PRINCIPAL, nonce);
		verify(httpSession, times(1)).setMaxInactiveInterval(anyInt());
	}

	@Test
	public void GivenAuthenticatedRequest_WhenLogout_ThenRequestLogout() throws ServletException {
		// given
		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));

		// when
		security.logout(requestContext);

		// then
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpRequest, times(1)).logout();
	}

	@Test
	public void GivenAuthenticatedRequestAndSession_WhenLogout_ThenSessionChange() throws ServletException {
		// given
		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));
		when(httpRequest.getSession(false)).thenReturn(httpSession);
		
		// when
		security.logout(requestContext);

		// then
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpRequest, times(1)).logout();
		verify(httpSession, times(1)).removeAttribute(TinyContainer.ATTR_PRINCIPAL);
		verify(httpSession, times(1)).invalidate();
	}

	@Test
	public void GivenAuthenticatedRequestAndNoSession_WhenLogout_ThenNoSessionChange() throws ServletException {
		// given
		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));

		// when
		security.logout(requestContext);

		// then
		verify(httpSession, times(0)).removeAttribute(anyString());
		verify(httpSession, times(0)).invalidate();
	}


	@Test
	public void GivenServletException_WhenLogout_ThenSessionStillChange() throws ServletException {
		// given
		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));
		doThrow(ServletException.class).when(httpRequest).logout();
		when(httpRequest.getSession(false)).thenReturn(httpSession);
		
		// when
		security.logout(requestContext);

		// then
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpRequest, times(1)).logout();
		verify(httpSession, times(1)).removeAttribute(TinyContainer.ATTR_PRINCIPAL);
		verify(httpSession, times(1)).invalidate();
	}

	@Test(expected = BugError.class)
	public void GivenNoRequest_WhenLogout_ThenException() {
		// given
		when(requestContext.getRequest()).thenReturn(null);

		// when
		security.logout(requestContext);

		// then
	}

	@Test
	public void GivenAuthenticatedRequest_WhenGetUserPrincipal_ThenGetItFromRequest() {
		// given
		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));

		// when
		Principal principal = security.getUserPrincipal(requestContext);

		// then
		assertThat(principal, notNullValue());
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpSession, times(0)).getAttribute(anyString());
	}

	@Test
	public void GivenNotAuthenticatedRequest_WhenGetUserPrincipal_ThenGetItFromSession() {
		// given
		when(httpRequest.getSession()).thenReturn(httpSession);
		when(httpSession.getAttribute(TinyContainer.ATTR_PRINCIPAL)).thenReturn(mock(Principal.class));

		// when
		Principal principal = security.getUserPrincipal(requestContext);

		// then
		assertThat(principal, notNullValue());
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpSession, times(1)).getAttribute(anyString());
	}

	@Test
	public void GivenNotAuthenticatedRequestAndNoSession_WhenGetUserPrincipal_ThenNull() {
		// given

		// when
		Principal principal = security.getUserPrincipal(requestContext);

		// then
		assertThat(principal, nullValue());
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpSession, times(0)).getAttribute(anyString());
	}

	@Test
	public void GivenAuthenticatedRequest_WhenIsAuthorized_ThenIsUserInRole() {
		// given
		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));
		when(httpRequest.isUserInRole("admin")).thenReturn(true);

		// when
		boolean authorized = security.isAuthorized(requestContext, "admin");

		// then
		assertThat(authorized, equalTo(true));
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpRequest, times(1)).isUserInRole("admin");
	}

	@Test
	public void GivenNotAuthenticatedRequest_WhenIsAuthorized_ThenNotIsUserInRole() {
		// given

		// when
		boolean authorized = security.isAuthorized(requestContext, "admin");

		// then
		assertThat(authorized, equalTo(false));
		verify(httpRequest, times(1)).getUserPrincipal();
		verify(httpRequest, times(0)).isUserInRole(anyString());
	}
}
