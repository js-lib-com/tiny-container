package js.tiny.container.servlet;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.tiny.container.core.Factory;
import js.tiny.container.spi.IContainer;

@RunWith(MockitoJUnitRunner.class)
public class SessionScopeProviderTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpSession httpSession;
	@Mock
	private Provider<?> provider;

	private SessionScopeProvider<?> scopeProvider;

	@Before
	public void beforeTest() {
		Factory.bind(container);

		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getRequest()).thenReturn(httpRequest);
		when(httpRequest.getSession(true)).thenReturn(httpSession);

		scopeProvider = new SessionScopeProvider<>(provider);
	}

	@Test
	public void GivenCacheMissing_WhenGetInstance_ThenSetSessionAttribute() {
		// given

		// when
		scopeProvider.get();

		// then
		verify(httpSession, times(1)).getAttribute(anyString());
		verify(httpSession, times(1)).setAttribute(anyString(), any());
	}

	@Test
	public void GivenCachePresent_WhenGetInstance_ThenNotSetSessionAttribute() {
		// given
		Object instance = new Object();
		when(httpSession.getAttribute(anyString())).thenReturn(instance).thenReturn(instance);

		// when
		scopeProvider.get();

		// then
		verify(httpSession, times(1)).getAttribute(anyString());
		verify(httpSession, times(0)).setAttribute(anyString(), any());
	}

	@Test
	public void GivenDefaults_WhenGetSession_ThenNotNull() throws Exception {
		// given

		// when
		HttpSession session = scopeProvider.getSession();

		// then
		assertThat(session, notNullValue());
		verify(httpRequest, times(1)).getSession(true);
	}

	@Test(expected = BugError.class)
	public void GivenNullHttpRequest_WhenGetSession_ThenException() throws Exception {
		// given
		when(requestContext.getRequest()).thenReturn(null);

		// when
		scopeProvider.getSession();

		// then
	}
}
