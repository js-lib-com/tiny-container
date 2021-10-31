package js.tiny.container.spi;

import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;

/**
 * Container services for framework internals and plugins. This interface is a service provider interface and is not intended
 * for applications consumption. It deals with container extensions, and managed classes and methods. 
 * 
 * @author Iulian Rotaru
 */
public interface IContainer extends IFactory {

	/**
	 * Retrieve instance of requested managed class. Depending on managed class scope a new managed instance can be created or
	 * it can be reused from caches. If instance is newly created this factory method takes care to resolve all instance
	 * dependencies - at field, constructor and method level, and execute post-construct method, of course if defined.
	 * 
	 * Under normal circumstances this factory method always succeed and does return a not null instance. Implementation should
	 * fail only for severe errors like out of memory or similar extreme conditions. Anyway, since instance creation may involve
	 * user defined constructors is possible also to throw {@link InvocationException}. Also if requested interface is a service
	 * and no provider found at run-time this factory method throws {@link NoProviderException}. Finally, this factory method
	 * may also fail for other conditions that are related to development stage and considered bugs.
	 * 
	 * @param managedClass managed class to return instance for.
	 * @param <T> instance type.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * 
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws NoProviderException if interface is a service and no provider found on run-time.
	 * @throws ConverterException if attempt to initialize a field with a type for which there is no converter,
	 * @throws BugError if dependency value cannot be created or circular dependency is detected.
	 * @throws BugError if instance configuration fails either due to bad configuration object or fail on configuration user
	 *             defined logic.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 * @throws BugError if attempt to assign field to not POJO type.
	 */
	<T> T getInstance(IManagedClass<T> managedClass);

	/**
	 * Test if interface class has a managed class bound.
	 * 
	 * @param interfaceClass interface class to test.
	 * @return true if given interface class is managed.
	 */
	boolean isManagedClass(Class<?> interfaceClass);

	/**
	 * Get managed class bound to requested interface class or null if none found.
	 * 
	 * @param interfaceClass interface class to retrieve managed class for.
	 * @return managed class bound to requested interface or null.
	 */
	<T> IManagedClass<T> getManagedClass(Class<T> interfaceClass);

	/**
	 * Get all managed classes registered to this container.
	 * 
	 * @return container managed classes, in no particular order.
	 */
	Iterable<IManagedClass<?>> getManagedClasses();

	/**
	 * Get all managed methods, from all managed classes, registered to this container.
	 * 
	 * @return container managed methods, in no particular order.
	 */
	Iterable<IManagedMethod> getManagedMethods();

}
