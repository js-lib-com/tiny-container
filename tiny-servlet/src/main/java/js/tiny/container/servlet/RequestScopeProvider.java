package js.tiny.container.servlet;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IScopeFactory;
import js.injector.Key;
import js.injector.ScopedProvider;
import js.tiny.container.spi.IContainer;

/**
 * Scope provider specialized in creation of {@link RequestScoped} instances.
 * 
 * @author Iulian Rotaru
 */
public class RequestScopeProvider<T> extends ScopedProvider<T> {
	private final IInjector injector;
	private final Key<T> key;

	/**
	 * Construct this HTTP request scoped provider. Because is not allowed to nest the scoped providers, throws illegal argument
	 * if given provisioning binding define a provider that is already a scoped provider.
	 * 
	 * @param injector parent injector,
	 * @param provisioningProvider provisioning binding, used for actual instances creation.
	 * @throws IllegalArgumentException if provider defined by provisioning binding argument is already a scoped provider.
	 */
	public RequestScopeProvider(IInjector injector, IBinding<T> provisioningBinding) {
		super(provisioningBinding.provider());
		this.injector = injector;
		this.key = provisioningBinding.key();
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return RequestScoped.class;
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
		return getProvisioningProvider().toString() + ":REQUEST";
	}

	/** Request scope provider cache is kept on HTTP request attributes, identified by this name. */
	public static final String ATTR_CACHE = "injector-cache";

	/**
	 * Get request scope provider cache kept on current HTTP request attributes. Create cache on the fly if missing.
	 * 
	 * @return request provider cache from current HTTP request.
	 * @throws ContextNotActiveException if attempt to use this method outside HTTP request thread.
	 */
	synchronized Map<String, Object> cache() {
		HttpServletRequest httpRequest = injector.getInstance(HttpServletRequest.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpRequest.getAttribute(ATTR_CACHE);
		if (cache == null) {
			cache = new HashMap<>();
			httpRequest.setAttribute(ATTR_CACHE, cache);
		}
		return cache;
	}

	/**
	 * Destroy context for request scope provider. Invoked by container on HTTP request destroying, see
	 * {@link TinyContainer#requestDestroyed(javax.servlet.ServletRequestEvent)}.
	 * 
	 * Signal instance of out scope for all instances found on the request scope provider cache.
	 * 
	 * @param container parent container,
	 * @param httpRequest HTTP request about to be destroyed.
	 */
	public static void destroyContext(IContainer container, HttpServletRequest httpRequest) {
		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpRequest.getAttribute(RequestScopeProvider.ATTR_CACHE);
		if (cache != null) {
			cache.values().forEach(instance -> container.onInstanceOutOfScope(instance));
		}
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScopeFactory<T> {
		@Override
		public Provider<T> getScopedProvider(IInjector injector, IBinding<T> provisioningBinding) {
			return new RequestScopeProvider<>(injector, provisioningBinding);
		}
	}
}
