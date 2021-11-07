package js.tiny.container.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.injector.Key;

import js.lang.BugError;
import js.tiny.container.spi.Factory;
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
	private Key<Object> instanceKey;
	@Mock
	private Provider<Object> provisioningProvider;

	private SessionScopeProvider<Object> scopeProvider;

	@Before
	public void beforeTest() {
		Factory.bind(container);

		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getRequest()).thenReturn(httpRequest);
		when(httpRequest.getSession(true)).thenReturn(httpSession);

		Map<String, Object> attributes = new HashMap<>();
		when(httpSession.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
		doAnswer(invocation -> {
			attributes.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(httpSession).setAttribute(anyString(), any());

		when(instanceKey.toScope()).thenReturn("scoped-key");
		when(provisioningProvider.get()).thenAnswer(invocation -> new Object());
		
		scopeProvider = new SessionScopeProvider<>(instanceKey, provisioningProvider);
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
	public void GivenCache_WhenGetInstance_ThenNotSetSessionAttribute() {
		// given
		Object instance = new Object();
		when(httpSession.getAttribute(anyString())).thenReturn(instance).thenReturn(instance);

		// when
		scopeProvider.get();

		// then
		verify(httpSession, times(1)).getAttribute(anyString());
		verify(httpSession, times(0)).setAttribute(anyString(), any());
	}

	/**
	 * There are two bindings using the same provisioning provider. Both bindings have session scope. When retrieve instance for
	 * the first time, i.e. with cache miss, resulting instances should be different.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void GivenMissingCacheOnTwoScopesWithTheSameProvisioningClass_WhenGetInstance_ThenNotEqual() {
		// given
		Key<Object> key1 = Mockito.mock(Key.class);
		Key<Object> key2 = Mockito.mock(Key.class);

		when(key1.toScope()).thenReturn("scoped-key1");
		when(key2.toScope()).thenReturn("scoped-key2");

		SessionScopeProvider<Object> scopeProvider1 = new SessionScopeProvider<>(key1, provisioningProvider);
		SessionScopeProvider<Object> scopeProvider2 = new SessionScopeProvider<>(key2, provisioningProvider);

		// when
		Object instance1 = scopeProvider1.get();
		Object instance2 = scopeProvider2.get();

		// then
		assertThat(instance1, not(equalTo(instance2)));
		verify(httpSession, times(2)).getAttribute(anyString());
		verify(httpSession, times(2)).setAttribute(anyString(), any());
	}

	/**
	 * There are two bindings using the same provisioning provider. Both bindings have session scope. When retrieve instance for
	 * the second time, i.e. with scope cache, resulting instances should be different.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void GivenCacheOnTwoScopesWithTheSameProvisioningClass_WhenGetInstance_ThenNotEqual() {
		// given
		Key<Object> key1 = Mockito.mock(Key.class);
		Key<Object> key2 = Mockito.mock(Key.class);

		when(key1.toScope()).thenReturn("scoped-key1");
		when(key2.toScope()).thenReturn("scoped-key2");

		SessionScopeProvider<Object> scopeProvider1 = new SessionScopeProvider<>(key1, provisioningProvider);
		SessionScopeProvider<Object> scopeProvider2 = new SessionScopeProvider<>(key2, provisioningProvider);

		// when
		Object instance1 = scopeProvider1.get();
		instance1 = scopeProvider1.get();

		Object instance2 = scopeProvider2.get();
		instance2 = scopeProvider2.get();

		// then
		assertThat(instance1, not(equalTo(instance2)));
	}

	@Test
	public void GivenHttpRequest_WhenGetSession_ThenNotNull() throws Exception {
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
