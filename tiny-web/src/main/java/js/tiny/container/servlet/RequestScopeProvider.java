package js.tiny.container.servlet;

import javax.inject.Provider;

import js.injector.IScope;
import js.injector.Key;
import js.injector.ScopedProvider;

public class RequestScopeProvider<T> extends ScopedProvider<T> {
	private final Key<T> key;

	/**
	 * Construct this session provider instance. Because is not allowed to nest the scoped providers, throws illegal argument if
	 * given provisioning provider argument is a scoped provider instance.
	 * 
	 * @param key instance key for which this session provider is created.
	 * @param provisioningProvider wrapped provisioning provider.
	 * @throws IllegalArgumentException if provisioning provider argument is a scoped provider instance.
	 */
	public RequestScopeProvider(Key<T> key, Provider<T> provisioningProvider) {
		super(provisioningProvider);
		this.key = key;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getScopeInstance() {
		RequestContext requestContext = getRequestContext();
		return (T) requestContext.getAttribute(key.toScope());
	}

	@Override
	public T get() {
		T instance = getScopeInstance();
		if (instance == null) {
			synchronized (this) {
				if (instance == null) {
					instance = getProvisioningProvider().get();
					getRequestContext().setAttribute(key.toScope(), instance);
				}
			}
		}
		return instance;
	}

	@Override
	public String toString() {
		return getProvisioningProvider().toString() + ":REQUEST";
	}

	private RequestContext getRequestContext() {
		RequestContext requestContext = js.tiny.container.spi.Factory.getInstance(RequestContext.class);
		return requestContext;
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScope<T> {
		@Override
		public Provider<T> scope(Key<T> key, Provider<T> provider) {
			return new RequestScopeProvider<>(key, provider);
		}
	}
}
