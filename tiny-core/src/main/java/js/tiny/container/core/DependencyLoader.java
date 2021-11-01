package js.tiny.container.core;

import java.lang.reflect.Proxy;
import java.util.Stack;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.util.Classes;
import js.util.Types;

/**
 * Utility class for dependencies loading.
 *
 * @author Iulian Rotaru
 */
public class DependencyLoader {
	private static final Log log = LogFactory.getLog(DependencyLoader.class);

	/** Thread local storage for dependencies trace stack. Used to prevent circular dependencies. */
	private static ThreadLocal<Stack<Class<?>>> dependenciesStack = new ThreadLocal<>();

	/**
	 * Get dependency value of requested type. See class description for a discussion about supported types and circular
	 * dependencies.
	 * 
	 * @param hostManagedClass managed class defining the context on which dependency is resolved,
	 * @param type dependency type.
	 * @return dependency instance, reused from container caches or fresh created.
	 * @throws BugError if dependency value cannot be created or circular dependency is detected.
	 */
	@SuppressWarnings("unchecked")
	public static Object getDependencyValue(IManagedClass<?> hostManagedClass, Class<?> type) {
		Stack<Class<?>> stackTrace = dependenciesStack.get();
		if (stackTrace == null) {
			stackTrace = new Stack<>();
			dependenciesStack.set(stackTrace);
		}
		IContainer container = hostManagedClass.getContainer();

		if (stackTrace.contains(type)) {
			try {
				// add current dependency class to reveal what dependency from stack is circular
				stackTrace.add(type);

				StringBuilder builder = new StringBuilder();
				builder.append("Circular dependency. Dependencies trace follows:\r\n");
				for (Class<?> stackTraceClass : stackTrace) {
					builder.append("\t- ");
					builder.append(stackTraceClass.getName());
					builder.append("\r\n");
				}

				log.error(builder.toString());
				throw new BugError("Circular dependency for |%s|.", type.getName());
			} finally {
				// takes care to current thread stack trace is removed
				dependenciesStack.remove();
			}
		}

		stackTrace.push(type);
		try {

			IManagedClass<?> dependencyManagedClass = container.getManagedClass(type);
			if (isProxyRequired(hostManagedClass, dependencyManagedClass)) {
				// if scope proxy is required returns a Java Proxy handled by ScopeProxyHandler
				ScopeProxyHandler<?> handler = new ScopeProxyHandler<>(container, type);
				return Proxy.newProxyInstance(dependencyManagedClass.getImplementationClass().getClassLoader(), new Class[] { dependencyManagedClass.getInterfaceClass() }, handler);
			}

			Object value = container.getOptionalInstance((Class<? super Object>) type);
			if (value != null) {
				// if dependency type is a managed class returns it value from factory
				return value;
			}

			if (Types.isKindOf(type, IFactory.class)) {
				// handle ApFactory and its hierarchy since it is a special case
				return container;
			}

			if (Classes.isInstantiable(type)) {
				// if requested type is instantiable POJO create a new empty instance of requested type
				return Classes.newInstance(type);
			}

			// TODO: test value instance of
			// if FactoryBean consider it as factory and substitute value
			// e.g. value = ((FactoryBean)value).getInstance(value.getClass())

			// all attempts to create dependency value has fallen
			throw new BugError("Dependency |%s| not resolved for |%s|.", type.getName(), hostManagedClass);

		} finally {
			stackTrace.pop();
			// do not remove stack trace after outermost call finished, i.e. when stack trace is empty
			// leave it on thread local for reuse, in order to avoid unnecessary object creation
		}
	}

	/**
	 * Compare host and dependency managed classes scope and decide if scope proxy is required. Current implementation enable
	 * scope proxy only when dependency has {@link InstanceScope#SESSION} scope.
	 * 
	 * @param hostManagedClass managed class defining the context on which dependency is resolved,
	 * @param dependencyManagedClass dependency managed class, possible null.
	 * @return true if scope proxy is required.
	 */
	private static boolean isProxyRequired(IManagedClass<?> hostManagedClass, IManagedClass<?> dependencyManagedClass) {
		if (dependencyManagedClass != null) {
			InstanceScope dependencyScope = dependencyManagedClass.getInstanceScope();
			if (InstanceScope.THREAD.equals(dependencyScope)) {
				return InstanceScope.APPLICATION.equals(hostManagedClass.getInstanceScope());
			}
			return InstanceScope.SESSION.equals(dependencyScope);
		}
		return false;
	}
}
