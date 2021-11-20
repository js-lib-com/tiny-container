package js.tiny.container.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

/**
 * Proxy invocation handler.
 * 
 * @author Iulian Rotaru
 */
public class ManagedProxy<T> implements InvocationHandler {
	private static final Log log = LogFactory.getLog(ManagedProxy.class);

	private final IManagedClass<T> managedClass;
	private final T managedInstance;

	public ManagedProxy(IManagedClass<T> managedClass, T managedInstance) {
		this.managedClass = managedClass;
		this.managedInstance = managedInstance;
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
