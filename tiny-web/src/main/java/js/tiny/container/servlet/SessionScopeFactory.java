package js.tiny.container.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import js.lang.BugError;
import js.tiny.container.cdi.ScopeFactory;
import js.tiny.container.core.InstanceKey;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.InstanceScope;

/**
 * Scope factory for managed classes with HTTP session scope. A managed instance with {@link InstanceScope#SESSION} scope is
 * stateful across a single HTTP session. Managed instance is stored on HTTP session attribute; when HTTP session expires,
 * attribute is removed and session managed instance becomes candidate for GC.
 * <p>
 * Session scope factory uses HTTP session to retrieve previously stored managed instance via {@link #getInstance(InstanceKey)};
 * HTTP session is updated by {@link #persistInstance(InstanceKey, Object)}. Both uses provided key argument for instance
 * storage and retrieval.
 * 
 * @author Iulian Rotaru
 */
final class SessionScopeFactory implements ScopeFactory {
	/** Reference to parent container. */
	private IContainer container;

	/**
	 * Construct session scope factory instance.
	 * 
	 * @param container parent container reference.
	 */
	public SessionScopeFactory(IContainer container) {
		this.container = container;
	}

	@Override
	public InstanceScope getInstanceScope() {
		return InstanceScope.SESSION;
	}

	/**
	 * Retrieve instance from current HTTP session, bound to given instance key or null if none found. Uses provided instance
	 * key argument as HTTP session attribute name. Managed instance key argument should be not null.
	 * <p>
	 * Access to HTTP session is obtained from HTTP request; therefore, it is considered a bug if attempt to use this method
	 * outside a HTTP request thread.
	 * <p>
	 * Implementation note: this method could have side effect. It <b>creates the HTTP session</b> if there is none on current
	 * HTTP request.
	 * 
	 * @param instanceKey managed instance key,
	 * @return managed instance or null.
	 * @throws BugError if attempt to use this method outside a HTTP request.
	 */
	@Override
	public Object getInstance(InstanceKey instanceKey) {
		// at this point managed instance key is guaranteed to be non null

		// SESSION instances are stored on current HTTP session as named attribute value, using provided instance key
		// HTTP session is created on the fly if necessary
		// if HTTP session exists and possesses an attribute with instance key, simply returns stored instance
		// when HTTP session expires attribute values are removed and SESSION instances are garbage collected

		HttpSession httpSession = getSession(instanceKey);
		return httpSession.getAttribute(instanceKey.getValue());
	}

	/**
	 * Persist instance on current HTTP session, bound to given managed instance key. This method simply uses
	 * <code>instanceKey</code> to add instance as HTTP session attribute. Both arguments should to be not null.
	 * <p>
	 * Access to HTTP session is obtained from HTTP request; therefore, it is considered a bug if attempt to use this method
	 * outside a HTTP request thread.
	 * <p>
	 * Implementation note: this method could have side effect. It <b>creates the HTTP session</b> if there is none on current
	 * HTTP request.
	 * 
	 * @param instanceKey managed instance key,
	 * @param instance managed instance.
	 * @throws BugError if attempt to use this method outside a HTTP request.
	 */
	@Override
	public void persistInstance(InstanceKey instanceKey, Object instance) {
		// at this point key and instance arguments are guaranteed to be non null

		HttpSession httpSession = getSession(instanceKey);
		httpSession.setAttribute(instanceKey.getValue(), instance);
	}

	/**
	 * Get HTTP session from current request, creating it if necessary. This method should be call inside a request context
	 * otherwise bug error is thrown.
	 * 
	 * @param instanceKey managed instance key.
	 * @return current request HTTP session.
	 * @throws BugError if attempt to use this method outside a HTTP request.
	 */
	HttpSession getSession(InstanceKey instanceKey) {
		RequestContext requestContext = container.getInstance(RequestContext.class);
		HttpServletRequest httpRequest = requestContext.getRequest();
		if (httpRequest == null) {
			throw new BugError("Invalid web context due to null HTTP request. Cannot create managed instance for |%s| with scope SESSION.", instanceKey);
		}

		// create HTTP session if missing; accordingly API httpSession is never null if 'create' flag is true
		return httpRequest.getSession(true);
	}

	/** This method does nothing and leaves session instances release on servlet container. */
	@Override
	public void clear() {
	}
}