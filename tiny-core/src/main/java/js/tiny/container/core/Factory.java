package js.tiny.container.core;

import js.lang.BugError;
import js.tiny.container.spi.IContainer;

/**
 * Global master factory for managed instances. This utility class has JVM level visibility and just delegates requests to
 * {@link IContainer}, factory implementation. All operations described by factory interface are supported by this master factory.
 * See {@link IContainer} class description for general presentation.
 * 
 * In order for master factory to be able to delegate factory implementation, it should be bound to current thread using
 * {@link #bind(IContainer)}. One can test if factory implementation is properly bound using {@link #isValid()}. Also there is
 * convenient helper that retrieve current thread factory - see {@link #get()}.
 * 
 * @author Iulian Rotaru
 */
public final class Factory {
	/**
	 * Inheritable thread local storage for factory implementation. This field keeps factory implementation reference on current
	 * thread; is updated by {@link #bind(IContainer)} and value retrieved by {@link #get()}. Uses inheritable thread local
	 * storage so that child threads can have access to parent factory.
	 */
	private static ThreadLocal<IContainer> tls = new InheritableThreadLocal<>();

	/** Forbid default constructor synthesis. */
	private Factory() {
	}

	/**
	 * Bind a factory implementation to current thread. Depending on application architecture it can be done on application
	 * start or, in web contexts, on every new HTTP request. Given factory implementation is saved on current thread local
	 * storage.
	 * 
	 * @param factory factory implementation.
	 */
	public static void bind(IContainer factory) {
		tls.set(factory);
	}

	/**
	 * Delegates {@link IContainer#getInstance(Class)}.
	 * 
	 * @param interfaceClass requested interface class.
	 * @param <T> managed class implementation.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * @throws BugError if application factory is not bound to current thread.
	 * @see IContainer#getInstance(Class)
	 */
	public static <T> T getInstance(Class<T> interfaceClass) {
		return get().getInstance(interfaceClass);
	}

	/**
	 * Delegates {@link IContainer#getOptionalInstance(Class)}.
	 * 
	 * @param interfaceClass requested interface class.
	 * @return managed instance or null if no implementation found.
	 * @param <T> managed class implementation.
	 * @throws BugError if application factory is not bound to current thread.
	 * @see IContainer#getOptionalInstance(Class)
	 */
	public static <T> T getOptionalInstance(Class<T> interfaceClass) {
		return get().getOptionalInstance(interfaceClass);
	}

	/**
	 * Test if current thread has a factory implementation bound. If return true current thread can safely be used for managed
	 * instances creation. Trying to create managed instances from a thread for which this predicate returns false will throw
	 * bug error.
	 * 
	 * @return true if current thread has factory implementation bound.
	 */
	public static boolean isValid() {
		return tls.get() != null;
	}

	/**
	 * Helper method to retrieve factory implementation bound to current thread. Retrieve factory reference from current thread
	 * local storage. In order to be successfully this method must be preceded by {@link #bind(IContainer)} called from current
	 * thread; otherwise bug error is thrown.
	 * 
	 * @return factory bound to current thread.
	 * @throws BugError if current thread has no application factory bound.
	 * @implNote package visibility to grant unit tests access.
	 */
	static IContainer get() {
		IContainer factory = tls.get();
		if (factory == null) {
			throw new BugError("No factory implementation bound to current thread |%s|. See #bind(IContainer).", Thread.currentThread());
		}
		return factory;
	}

	/** Unit tests access to factory local thread storage. */
	static ThreadLocal<IContainer> tls() {
		return tls;
	}
}
