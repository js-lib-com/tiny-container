package js.tiny.container.servlet;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Provider;
import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IScopeFactory;
import js.injector.Key;
import js.injector.ScopedProvider;
import js.tiny.container.spi.IContainer;

/**
 * Scope provider specialized in creation of {@link SessionScoped} instances.
 * 
 * @author Iulian Rotaru
 */
public class SessionScopeProvider<T> extends ScopedProvider<T> {
	private final IInjector injector;
	private final Key<T> key;

	/**
	 * Construct this HTTP session scoped provider. Because is not allowed to nest the scoped providers, throws illegal argument
	 * if given provisioning binding define a provider that is already a scoped provider.
	 * 
	 * @param injector parent injector,
	 * @param provisioningProvider provisioning binding, used for actual instances creation.
	 * @throws IllegalArgumentException if provider defined by provisioning binding argument is already a scoped provider.
	 */
	public SessionScopeProvider(IInjector injector, IBinding<T> provisioningBinding) {
		super(provisioningBinding.provider());
		this.injector = injector;
		this.key = provisioningBinding.key();
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return SessionScoped.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getScopeInstance() {
		return (T) cache().get(key.toScope());
	}

	@Override
	public T get() {
		T instance = getScopeInstance();
		if (instance == null) {
			synchronized (this) {
				if (instance == null) {
					instance = getProvisioningProvider().get();
					cache().put(key.toScope(), instance);
				}
			}
		}
		return instance;
	}

	@Override
	public String toString() {
		return getProvisioningProvider().toString() + ":SESSION";
	}

	/** Session scope provider cache is kept on HTTP session attributes, identified by this name. */
	private static final String ATTR_CACHE = "injector-cache";

	/**
	 * Get session scope provider cache kept on current HTTP session attributes. Create cache on the fly if missing.
	 * 
	 * @return session provider cache from current HTTP request.
	 * @throws ContextNotActiveException if attempt to use this method outside HTTP request thread.
	 */
	synchronized Map<String, Object> cache() {
		HttpServletRequest httpRequest = injector.getInstance(HttpServletRequest.class);
		HttpSession httpSession = httpRequest.getSession(true);

		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpSession.getAttribute(ATTR_CACHE);
		if (cache == null) {
			cache = new HashMap<>();
			httpSession.setAttribute(ATTR_CACHE, cache);
		}
		return cache;
	}

	/**
	 * Destroy context for session scope provider. Invoked by container on HTTP session destroying, see
	 * {@link TinyContainer#sessionDestroyed(javax.servlet.http.HttpSessionEvent)}.
	 * 
	 * Signal instance of out scope for all instances found on the session scope provider cache.
	 * 
	 * @param container parent container,
	 * @param httpSession HTTP session about to be destroyed.
	 */
	public static void destroyContext(IContainer container, HttpSession httpSession) {
		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpSession.getAttribute(SessionScopeProvider.ATTR_CACHE);
		if (cache != null) {
			cache.values().forEach(instance -> container.onInstanceOutOfScope(instance));
		}
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScopeFactory<T> {
		@Override
		public Provider<T> getScopedProvider(IInjector injector, IBinding<T> provisioningBinding) {
			return new SessionScopeProvider<>(injector, provisioningBinding);
		}
	}
}
