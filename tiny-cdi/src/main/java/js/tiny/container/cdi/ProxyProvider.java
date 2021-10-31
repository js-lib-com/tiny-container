package js.tiny.container.cdi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.inject.Provider;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

/**
 * Create a Java {@link Proxy} that delegates all method invocations to given managed instance. This proxy allows invoking
 * managed methods via <code>member operator</code>, i.e. dot notation.
 * 
 * @author Iulian Rotaru
 */
class ProxyProvider<T> implements Provider<T> {
	private static final Log log = LogFactory.getLog(ProxyProvider.class);

	private final IManagedClass<T> managedClass;
	private final Provider<T> provider;

	/**
	 * Proxy provider construction.
	 * 
	 * @param managedClass managed instance class,
	 * @param provider provider to create instance to delegate requests to.
	 */
	public ProxyProvider(IManagedClass<T> managedClass, Provider<T> provider) {
		log.trace("ProxyProvider(IManagedClass<T>, T)");
		this.managedClass = managedClass;
		this.provider = provider;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		final ClassLoader classLoader = managedClass.getImplementationClass().getClassLoader();
		final Class<T>[] interfaces = new Class[] { managedClass.getInterfaceClass() };
		final InvocationHandler handler = new ProxyHandler<>(managedClass, provider.get());
		return (T) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}

	@Override
	public String toString() {
		return provider.toString() + ":PROXY";
	}

	/**
	 * Proxy invocation handler.
	 * 
	 * @author Iulian Rotaru
	 */
	private static class ProxyHandler<T> implements InvocationHandler {
		private final IManagedClass<T> managedClass;
		private final T managedInstance;

		public ProxyHandler(IManagedClass<T> managedClass, T managedInstance) {
			this.managedClass = managedClass;
			this.managedInstance = managedInstance;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if(method.getDeclaringClass().equals(Object.class)) {
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
