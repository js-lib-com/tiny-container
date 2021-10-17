package js.tiny.container.core;

import js.lang.BugError;
import js.rmi.UnsupportedProtocolException;
import js.tiny.container.spi.IFactory;

/**
 * Server global master factory for managed instances. This utility class has JVM level visibility and just delegates requests
 * to {@link IFactory}, application specific factory. All operations described by application factory interface are supported by
 * this master factory. See {@link IFactory} class description for general presentation.
 * <p>
 * In order for master factory to be able to delegate application factory, it should be bound to current thread using
 * {@link #bind(IFactory)}; otherwise {@link BugError} is thrown. One can test if application factory is properly bound using
 * {@link #isValid()}. Also there is convenient helper that retrieve current thread application factory.
 * 
 * @author Iulian Rotaru
 */
public final class Factory {
	/**
	 * Inheritable thread local storage for application specific factories. This field keeps application factory reference on
	 * current thread; is updated by {@link #bind(IFactory)} and value retrieved by {@link #get()}. Uses inheritable thread
	 * local storage so that child threads can have access to parent application factory.
	 */
	private static ThreadLocal<IFactory> tls = new InheritableThreadLocal<>();

	/** Forbid default constructor synthesis. */
	private Factory() {
	}

	/**
	 * Bind application specific factory to current thread. Depending on application architecture it can be done on application
	 * start or, in web contexts, on every new HTTP request. Given application specific factory is saved on current thread local
	 * storage.
	 * 
	 * @param appFactory application specific factory.
	 */
	public static void bind(IFactory appFactory) {
		tls.set(appFactory);
	}

	/**
	 * Delegates {@link IFactory#getInstance(Class)}.
	 * 
	 * @param interfaceClass requested interface class.
	 * @param <T> managed class implementation.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * @throws BugError if application factory is not bound to current thread.
	 * @see IFactory#getInstance(Class)
	 */
	public static <T> T getInstance(Class<T> interfaceClass) {
		return get().getInstance(interfaceClass);
	}

	/**
	 * Delegates {@link IFactory#getInstance(String, Class)}.
	 * 
	 * @param instanceName instance name,
	 * @param interfaceClass requested interface class.
	 * @param <T> managed class implementation.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * @throws BugError if application factory is not bound to current thread.
	 * @see IFactory#getInstance(String, Class)
	 */
	public static <T> T getInstance(String instanceName, Class<T> interfaceClass) {
		return get().getInstance(instanceName, interfaceClass);
	}

	/**
	 * Delegates {@link IFactory#getOptionalInstance(Class)}.
	 * 
	 * @param interfaceClass requested interface class.
	 * @return managed instance or null if no implementation found.
	 * @param <T> managed class implementation.
	 * @throws BugError if application factory is not bound to current thread.
	 * @see IFactory#getOptionalInstance(Class)
	 */
	public static <T> T getOptionalInstance(Class<T> interfaceClass) {
		return get().getOptionalInstance(interfaceClass);
	}

	/**
	 * Delegates {@link IFactory#getRemoteInstance(String, Class)}.
	 * 
	 * @param implementationURL the URL of remote implementation,
	 * @param interfaceClass interface implemented by remote class.
	 * @param <T> managed class implementation.
	 * @return remote class proxy instance.
	 * @throws UnsupportedProtocolException if URL protocol is not supported.
	 * @throws BugError if application factory is not bound to current thread.
	 * @see AppFactory#getRemoteInstance(String, Class)
	 */
	public static <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
		return get().getRemoteInstance(implementationURL, interfaceClass);
	}

	/**
	 * Test if current thread has application specific factory bound. If return true current thread can safely be used for
	 * managed instances creation. Trying to create managed instances from a thread for which this predicate returns false will
	 * throw bug error.
	 * 
	 * @return true if current thread has application specific factory bound.
	 */
	public static boolean isValid() {
		return tls.get() != null;
	}

	/**
	 * Helper method to retrieve application factory bound to current thread. Retrieve application factory from current thread
	 * local storage. In order to be successfully this method must be preceded by {@link #bind(IFactory)} called from current
	 * thread; otherwise bug error is thrown.
	 * 
	 * @return application factory bound to current thread.
	 * @throws BugError if current thread has no application factory bound.
	 */
	static IFactory get() {
		IFactory factory = tls.get();
		if (factory == null) {
			throw new BugError("No factory implementation bound to current thread |%s|. See #bind(IFactory).", Thread.currentThread());
		}
		return factory;
	}

	static ThreadLocal<IFactory> tls() {
		return tls;
	}
}
