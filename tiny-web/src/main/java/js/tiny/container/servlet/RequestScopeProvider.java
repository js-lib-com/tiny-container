package js.tiny.container.servlet;

import java.lang.annotation.Annotation;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IScopeFactory;
import js.injector.Key;
import js.injector.RequestScoped;
import js.injector.ScopedProvider;

public class RequestScopeProvider<T> extends ScopedProvider<T> {
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
		return (T) getHttpRequest().getAttribute(key.toScope());
	}

	@Override
	public T get() {
		T instance = getScopeInstance();
		if (instance == null) {
			synchronized (this) {
				if (instance == null) {
					instance = getProvisioningProvider().get();
					getHttpRequest().setAttribute(key.toScope(), instance);
				}
			}
		}
		return instance;
	}

	@Override
	public String toString() {
		return getProvisioningProvider().toString() + ":REQUEST";
	}

	private HttpServletRequest getHttpRequest() {
		return injector.getInstance(RequestContext.class).getRequest();
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScopeFactory<T> {
		@Override
		public Provider<T> getScopedProvider(IInjector injector, IBinding<T> provisioningBinding) {
			return new RequestScopeProvider<>(injector, provisioningBinding);
		}
	}
}
