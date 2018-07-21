package js.container;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import js.core.AppFactory;

/**
 * Adapter for dependency with shorter scope hosted into managed instance with larger scope. If dependency scope, i.e. life span
 * is shorter than hosting instance is possible for dependency instance to become invalid, that is out of scope or scope
 * changed, while hosting instance is still valid. For example, a dependency with {@link InstanceScope#SESSION} scope hosted in
 * a controller with {@link InstanceScope#APPLICATION} scope: controller should use the managed instance attached to current
 * HTTP request only.
 * <p>
 * This proxy uses parent application factory to retrieve managed instance before every method invocation.
 * {@link AppFactory#getInstance(Class, Object...)} takes care about managed instance scope.
 * <p>
 * This class is designed specifically for {@link InstanceFieldsInjectionProcessor}.
 * 
 * @author Iulian Rotaru
 * @param <T> managed class type.
 * @version final
 */
final class ScopeProxyHandler<T> implements InvocationHandler {
	/** Parent application factory. */
	private final AppFactory appFactory;
	/** Managed class interface. */
	private final Class<T> interfaceClass;

	/**
	 * Construct scope proxy handler with parent application factory and managed class interface.
	 * 
	 * @param appFactory parent application factory,
	 * @param interfaceClasss managed class interface.
	 */
	public ScopeProxyHandler(AppFactory appFactory, Class<T> interfaceClasss) {
		this.appFactory = appFactory;
		this.interfaceClass = interfaceClasss;
	}

	/**
	 * Retrieve managed instance from application factory and invoke given method on that instance.
	 * 
	 * @param proxy Java Proxy instance using this handler, unused by current implementation,
	 * @param method reflexive method invoked by proxy instance caller,
	 * @param args method invocation arguments.
	 * @return value returned by method execution, possible null.
	 * @throws Throwable any exception generated by method execution is bubbled up.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		T instance = appFactory.getInstance(interfaceClass);
		return method.invoke(instance, args);
	}
}
