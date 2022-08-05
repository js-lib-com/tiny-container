package com.jslib.container.cdi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;

import jakarta.inject.Provider;
import com.jslib.api.injector.ITypedProvider;
import com.jslib.lang.BugError;
import com.jslib.lang.InstanceInvocationHandler;
import com.jslib.lang.InvocationException;

/**
 * Create a Java {@link Proxy} that delegates all method invocations to given managed instance. This proxy allows invoking
 * managed methods via <code>member operator</code>, i.e. dot notation, while applying container method invocation services.
 * 
 * This provider is used by {@link ProxyBinding}. It is used only if container proxy processing is enabled, when create embedded
 * container.
 * 
 * @author Iulian Rotaru
 */
class ProxyProvider<T> implements ITypedProvider<T> {
	private static final Log log = LogFactory.getLog(ProxyProvider.class);

	private final Class<T> interfaceClass;
	private final IManagedLoader managedLoader;
	private final Provider<T> provider;

	public ProxyProvider(Class<T> interfaceClass, IManagedLoader managedLoader, Provider<T> provider) {
		log.trace("ProxyProvider(Class<T>, IManagedLoader, IManagedClass<T>>, Provider)");
		this.interfaceClass = interfaceClass;
		this.managedLoader = managedLoader;
		this.provider = provider;
	}

	@Override
	public Class<? extends T> type() {
		return ((ITypedProvider<T>) provider).type();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		final ClassLoader classLoader = interfaceClass.getClassLoader();
		final Class<T>[] interfaces = new Class[] { interfaceClass };
		final InvocationHandler handler = new ProxyHandler<>(managedLoader.getManagedClass(interfaceClass), provider.get());
		return (T) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}

	@Override
	public String toString() {
		return provider.toString() + ":PROXY";
	}

	/**
	 * Invocation handler for proxy provider. It just delegates method invocations to
	 * {@link IManagedMethod#invoke(Object, Object...)}.
	 * 
	 * @author Iulian Rotaru
	 */
	private static class ProxyHandler<T> implements InstanceInvocationHandler<T> {
		private final IManagedClass<T> managedClass;
		private final T managedInstance;

		public ProxyHandler(IManagedClass<T> managedClass, T managedInstance) {
			this.managedClass = managedClass;
			this.managedInstance = managedInstance;
		}

		@Override
		public T getWrappedInstance() {
			return managedInstance;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getDeclaringClass().equals(Object.class)) {
				return method.invoke(managedInstance, args);
			}

			final IManagedMethod managedMethod = managedClass.getManagedMethod(method.getName());
			if (managedMethod == null) {
				throw new BugError("Attempt to use not managed method |%s|.", method);
			}
			log.trace("Invoke |%s|.", managedMethod);

			try {
				return managedMethod.invoke(managedInstance, args);
			} catch (Throwable t) {
				throw throwable(t, "Method |%s| invocation fails.", managedMethod);
			}
		}

		/**
		 * Prepare given throwable and dump it to logger with formatted message. Return prepared throwable. If throwable is
		 * {@link InvocationTargetException} or its unchecked related version, {@link InvocationException} replace it with root
		 * cause.
		 * 
		 * @param throwable throwable instance,
		 * @param message formatted error message,
		 * @param args optional formatted message arguments.
		 * @return prepared throwable.
		 */
		private static Throwable throwable(Throwable throwable, String message, Object... args) {
			Throwable t = throwable;
			if (t instanceof InvocationException && t.getCause() != null) {
				t = t.getCause();
			}
			if (t instanceof InvocationTargetException && ((InvocationTargetException) t).getTargetException() != null) {
				t = ((InvocationTargetException) t).getTargetException();
			}
			message = String.format(message, args);
			log.dump(message, t);
			return t;
		}
	}
}
