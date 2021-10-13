package js.tiny.container;

import java.lang.reflect.Proxy;
import java.util.Stack;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.AppFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.util.Classes;
import js.util.Types;

/**
 * Base class for all processors dealing with dependencies. Supplies utility method for dependency instance retrieval with guard
 * against circular dependencies. Usually {@link #getDependencyValue(IManagedClass, Class)} expect as dependency type a managed
 * class and just delegate container for instance retrieval, see {@link AppFactory#getOptionalInstance(Class, Object...)}.
 * Anyway, if dependency type is not a managed class tries to instantiate it with standard {@link Class#newInstance()}; of
 * course type should be concrete class and have default constructor. Otherwise throws bug error.
 * <p>
 * Depending on host and dependency managed classes scope is possible that dependency value to be replaced by a scope proxy, see
 * {@link ScopeProxyHandler}. This is to adapt dependency with shorted life span into host with larger life span; otherwise
 * dependency may become invalid while host instance is still active. Logic to detect if scope proxy is required is encapsulated
 * into separated utility method, see {@link #isProxyRequired(IManagedClass, IManagedClass)}.
 * 
 * <h3>Circular Dependencies</h3>
 * <p>
 * When container creates a new managed instance there will be a call to this class utility method. On its turn this class
 * utility method may use container to retrieve instance for a dependency; this creates recursive calls that allows for
 * unrestricted graph but is prone to circular dependencies.
 * <p>
 * This class keeps a stack trace for classes in dependencies chain and throws bug error if discover a request for a dependency
 * type that is already into stack trace. Since stack trace is kept on thread local storage circular dependencies guard works
 * only in current thread.
 * 
 * <h3>App Factory</h3>
 * <p>
 * Application factory and its hierarchy cannot be managed classes since their job is to manage managed classes. If requested
 * dependency type is a kind of application factory, container cannot be used for instance retrieval. This is <b>special</b>
 * case handled by {@link #getDependencyValue(IManagedClass, Class)} with special logic: if dependency type is a kind of
 * {@link AppFactory} returns container reference provided by host managed class, see {@link IManagedClass#getContainer()}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class DependencyProcessor {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(DependencyProcessor.class);

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
	public static Object getDependencyValue(IManagedClass hostManagedClass, Class<?> type) {
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

			IManagedClass dependencyManagedClass = container.getManagedClass(type);
			if (isProxyRequired(hostManagedClass, dependencyManagedClass)) {
				// if scope proxy is required returns a Java Proxy handled by ScopeProxyHandler
				ScopeProxyHandler<?> handler = new ScopeProxyHandler<>(container, type);
				return Proxy.newProxyInstance(dependencyManagedClass.getImplementationClass().getClassLoader(), dependencyManagedClass.getInterfaceClasses(), handler);
			}

			Object value = container.getOptionalInstance((Class<? super Object>) type);
			if (value != null) {
				// if dependency type is a managed class returns it value from factory
				return value;
			}

			if (Types.isKindOf(type, AppFactory.class)) {
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
	private static boolean isProxyRequired(IManagedClass hostManagedClass, IManagedClass dependencyManagedClass) {
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
