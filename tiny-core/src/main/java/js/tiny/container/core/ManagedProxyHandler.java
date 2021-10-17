package js.tiny.container.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import js.lang.BugError;
import js.lang.InstanceInvocationHandler;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.InstanceType;
import js.util.Params;

/**
 * Invocation handler implementing container services for managed classes of {@link InstanceType#PROXY} type. Managed classes
 * declared as {@link InstanceType#PROXY} have the actual managed instance wrapped in a Java Proxy, dynamically generated. That
 * proxy routes all managed instance method invocations to this invocation handler {@link #invoke(Object, Method, Object[])}
 * method that, on its turn, delegates wrapped instance.
 * 
 * @author Iulian Rotaru
 */
public final class ManagedProxyHandler implements InstanceInvocationHandler<Object> {
	/** Class logger. */
	private static Log log = LogFactory.getLog(ManagedProxyHandler.class);

	/** Wrapped managed class. */
	private final IManagedClass managedClass;

	/** Managed instance. */
	private final Object managedInstance;

	/**
	 * Construct transactional proxy invocation handler for given managed instance.
	 * 
	 * @param managedClass managed class,
	 * @param managedInstance instance of managed class.
	 * @throws IllegalArgumentException if <code>managedClass</code> is null or not transactional or
	 *             <code>managedInstance</code> is null.
	 */
	public ManagedProxyHandler(IManagedClass managedClass, Object managedInstance) {
		Params.notNull(managedClass, "Managed class");
		Params.notNull(managedInstance, "Managed instance");
		this.managedClass = managedClass;
		this.managedInstance = managedInstance;
	}

	/**
	 * Return wrapped implementation instance.
	 * 
	 * @return wrapped implementation instance.
	 * @see #managedInstance
	 */
	@Override
	public Object getWrappedInstance() {
		return managedInstance;
	}

	/**
	 * Invocation handler implementation. Every method invocation on managed class interface is routed to this point. Here
	 * actual container services are implemented and method is invoked against wrapped instance.
	 * 
	 * @param proxy instance of dynamically generated Java Proxy,
	 * @param method interface Java method about to invoke,
	 * @param args method invocation arguments.
	 * @return value returned by implementation method.
	 * @throws Throwable forward any exception rose by implementation method.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final IManagedMethod managedMethod = managedClass.getManagedMethod(method.getName());
		if (managedMethod == null) {
			throw new BugError("Attempt to use not managed method |%s|.", method);
		}
		log.trace("Invoke |%s|.", managedMethod);
		try {
			return managedMethod.invoke(managedInstance, args);
		} catch (Throwable t) {
			throw throwable(t, "Non transactional method |%s| invocation fails.", managedMethod);
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
