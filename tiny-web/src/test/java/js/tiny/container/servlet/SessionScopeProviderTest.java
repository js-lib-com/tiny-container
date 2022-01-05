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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Provider;
import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.Key;
import js.tiny.container.spi.Factory;
import js.tiny.container.spi.IContainer;

@RunWith(MockitoJUnitRunner.class)
public class SessionScopeProviderTest {
	@Mock
	private IInjector injector;
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpSession httpSession;

	@Mock
	private IBinding<Object> provisioningBinding;
	@Mock
	private Key<Object> instanceKey;
	@Mock
	private Provider<Object> provisioningProvider;

	private SessionScopeProvider<Object> scopeProvider;

	@Before
	public void beforeTest() {
		Factory.bind(container);

		when(injector.getInstance(RequestContext.class)).thenReturn(requestContext);
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

		when(provisioningBinding.key()).thenReturn(instanceKey);
		when(provisioningBinding.provider()).thenReturn(provisioningProvider);

		scopeProvider = new SessionScopeProvider<>(injector, provisioningBinding);
	}

	@Test
	public void GivenCacheMissing_WhenGetInstance_ThenSetSessionAttribute() {
		// given

		// when
		scopeProvider.get();

		// then
		verify(httpSession, times(2)).getAttribute(anyString());
		verify(httpSession, times(1)).setAttribute(anyString(), any());
	}

	@Test
	public void GivenCache_WhenGetInstance_ThenNotSetSessionAttribute() {
		// given
		Object instance = new Object();
		Map<String, Object> cache = new HashMap<>();
		cache.put("key", instance);
		when(httpSession.getAttribute(anyString())).thenReturn(cache).thenReturn(cache);

		// when
		scopeProvider.get();

		// then
		verify(httpSession, times(2)).getAttribute(anyString());
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

		when(provisioningBinding.key()).thenReturn(key1);
		SessionScopeProvider<Object> scopeProvider1 = new SessionScopeProvider<>(injector, provisioningBinding);

		when(provisioningBinding.key()).thenReturn(key2);
		SessionScopeProvider<Object> scopeProvider2 = new SessionScopeProvider<>(injector, provisioningBinding);

		// when
		Object instance1 = scopeProvider1.get();
		Object instance2 = scopeProvider2.get();

		// then
		assertThat(instance1, not(equalTo(instance2)));
		verify(httpSession, times(4)).getAttribute(anyString());
		verify(httpSession, times(1)).setAttribute(anyString(), any());
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

		when(provisioningBinding.key()).thenReturn(key1);
		SessionScopeProvider<Object> scopeProvider1 = new SessionScopeProvider<>(injector, provisioningBinding);

		when(provisioningBinding.key()).thenReturn(key2);
		SessionScopeProvider<Object> scopeProvider2 = new SessionScopeProvider<>(injector, provisioningBinding);

		// when
		Object instance1 = scopeProvider1.get();
		instance1 = scopeProvider1.get();

		Object instance2 = scopeProvider2.get();
		instance2 = scopeProvider2.get();

		// then
		assertThat(instance1, not(equalTo(instance2)));
	}

	@Test
	public void GivenHttpRequest_WhenCache_ThenNotNull() throws Exception {
		// given

		// when
		Map<String, Object> cache = scopeProvider.cache();

		// then
		assertThat(cache, notNullValue());
		verify(httpRequest, times(1)).getSession(true);
	}

	@Test(expected = ContextNotActiveException.class)
	public void GivenNullHttpRequest_WhenCache_ThenException() throws Exception {
		// given
		when(requestContext.getRequest()).thenReturn(null);

		// when
		scopeProvider.cache();

		// then
	}
}
