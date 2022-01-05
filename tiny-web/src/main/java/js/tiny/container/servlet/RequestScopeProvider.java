package js.tiny.container.servlet;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Provider;
import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IScopeFactory;
import js.injector.Key;
import js.injector.ScopedProvider;
import js.tiny.container.spi.IContainer;

public class RequestScopeProvider<T> extends ScopedProvider<T> {
	public static final String ATTR_CACHE = "injector-cache";

	private final IInjector injector;
	private final Key<T> key;

	/**
	 * Construct this HTTP request scoped provider. Because is not allowed to nest the scoped providers, this factory method
	 * throws illegal argument if given provisioning binding define a provider that is already a scoped provider.
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

	// --------------------------------------------------------------------------------------------

	private static final ThreadLocal<HttpServletRequest> httpRequest = new ThreadLocal<>();

	public static void createHttpRequestContext(HttpServletRequest httpRequest) {
		RequestScopeProvider.httpRequest.set(httpRequest);
	}

	public static void destroyHttpRequestContext(IContainer container) {
		final HttpServletRequest httpRequest = RequestScopeProvider.httpRequest.get();
		assert httpRequest != null;

		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpRequest.getAttribute(RequestScopeProvider.ATTR_CACHE);
		if (cache != null) {
			cache.values().forEach(instance -> container.onInstanceOutOfScope(instance));
		}
		RequestScopeProvider.httpRequest.remove();
	}

	private synchronized Map<String, Object> cache() {
		final HttpServletRequest httpRequest = RequestScopeProvider.httpRequest.get();
		if (httpRequest == null) {
			throw new ContextNotActiveException(format("Attempt to create request scoped instance |%s| outside HTTP request context.", key));
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpRequest.getAttribute(ATTR_CACHE);
		if (cache == null) {
			cache = new HashMap<>();
			httpRequest.setAttribute(ATTR_CACHE, cache);
		}
		return cache;
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScopeFactory<T> {
		@Override
		public Provider<T> getScopedProvider(IInjector injector, IBinding<T> provisioningBinding) {
			return new RequestScopeProvider<>(injector, provisioningBinding);
		}
	}
}
