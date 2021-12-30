package js.tiny.container.servlet;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IScopeFactory;
import js.injector.Key;
import js.injector.ScopedProvider;
import js.lang.BugError;

public class SessionScopeProvider<T> extends ScopedProvider<T> {
	public static final String ATTR_CACHE = "injector-cache";

	private final IInjector injector;
	private final Key<T> key;

	/**
	 * Construct this HTTP session scoped provider. Because is not allowed to nest the scoped providers, this factory method
	 * throws illegal argument if given provisioning binding define a provider that is already a scoped provider.
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
		// SESSION instances are stored on current HTTP session as named attribute value, using instance key
		// HTTP session is created on the fly if necessary
		// if HTTP session exists and possesses an attribute with instance key, simply returns stored instance
		// when HTTP session expires attribute values are removed and SESSION instances are garbage collected

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

	/**
	 * Get HTTP session from current request, creating it if necessary. This method should be call inside a request context
	 * otherwise bug error is thrown.
	 * 
	 * @return current request HTTP session.
	 * @throws BugError if attempt to use this method outside a HTTP request.
	 */
	synchronized Map<String, Object> cache() {
		RequestContext requestContext = injector.getInstance(RequestContext.class);
		HttpServletRequest httpRequest = requestContext.getRequest();
		if (httpRequest == null) {
			throw new BugError("Invalid web context due to null HTTP request. Cannot create managed instance for |%s| with scope SESSION.", getProvisioningProvider().getClass().getCanonicalName());
		}

		// create HTTP session if missing
		// accordingly API, retrieved httpSession is never null if 'create' flag is true
		HttpSession httpSession = httpRequest.getSession(true);

		@SuppressWarnings("unchecked")
		Map<String, Object> cache = (Map<String, Object>) httpSession.getAttribute(ATTR_CACHE);
		if (cache == null) {
			cache = new HashMap<>();
			httpSession.setAttribute(ATTR_CACHE, cache);
		}
		return cache;
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScopeFactory<T> {
		@Override
		public Provider<T> getScopedProvider(IInjector injector, IBinding<T> provisioningBinding) {
			return new SessionScopeProvider<>(injector, provisioningBinding);
		}
	}
}
