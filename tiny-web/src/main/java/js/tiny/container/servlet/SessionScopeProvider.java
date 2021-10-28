package js.tiny.container.servlet;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.jslib.injector.ScopedProvider;

import js.lang.BugError;
import js.tiny.container.core.Factory;

public class SessionScopeProvider<T> extends ScopedProvider<T> {
	protected SessionScopeProvider(Provider<T> provider) {
		super(provider);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getScopeInstance() {
		HttpSession httpSession = getSession();
		return (T) httpSession.getAttribute(provider.getClass().getCanonicalName());
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
					getSession().setAttribute(provider.getClass().getCanonicalName(), instance);
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
		RequestContext requestContext = Factory.getInstance(RequestContext.class);
		HttpServletRequest httpRequest = requestContext.getRequest();
		if (httpRequest == null) {
			throw new BugError("Invalid web context due to null HTTP request. Cannot create managed instance for |%s| with scope SESSION.", provider.getClass().getCanonicalName());
		}

		// create HTTP session if missing; accordingly API httpSession is never null if 'create' flag is true
		return httpRequest.getSession(true);
	}
}
