package js.tiny.container.servlet;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

// TODO: dependencies on injector implementation
import com.jslib.injector.IScope;
import com.jslib.injector.Key;
import com.jslib.injector.ScopedProvider;

import js.lang.BugError;

public class SessionScopeProvider<T> extends ScopedProvider<T> {

	private final Key<T> key;

	protected SessionScopeProvider(Key<T> key, Provider<T> provider) {
		super(provider);
		this.key = key;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getScopeInstance() {
		HttpSession httpSession = getSession();
		return (T) httpSession.getAttribute(key.toScope());
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
					instance = provider.get();
					getSession().setAttribute(key.toScope(), instance);
				}
			}
		}
		return instance;
	}

	@Override
	public String toString() {
		return provider.toString() + ":SESSION";
	}

	/**
	 * Get HTTP session from current request, creating it if necessary. This method should be call inside a request context
	 * otherwise bug error is thrown.
	 * 
	 * @return current request HTTP session.
	 * @throws BugError if attempt to use this method outside a HTTP request.
	 */
	HttpSession getSession() {
		// TODO: replace global factory
		RequestContext requestContext = js.tiny.container.spi.Factory.getInstance(RequestContext.class);
		HttpServletRequest httpRequest = requestContext.getRequest();
		if (httpRequest == null) {
			throw new BugError("Invalid web context due to null HTTP request. Cannot create managed instance for |%s| with scope SESSION.", provider.getClass().getCanonicalName());
		}

		// create HTTP session if missing; accordingly API httpSession is never null if 'create' flag is true
		return httpRequest.getSession(true);
	}

	// --------------------------------------------------------------------------------------------

	public static class Factory<T> implements IScope<T> {
		@Override
		public Provider<T> scope(Key<T> key, Provider<T> provider) {
			return new SessionScopeProvider<>(key, provider);
		}
	}

}
