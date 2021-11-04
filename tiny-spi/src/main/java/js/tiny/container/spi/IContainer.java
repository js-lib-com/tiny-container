package js.tiny.container.spi;

import java.lang.reflect.InvocationTargetException;

import js.lang.InvocationException;
import js.lang.NoProviderException;

/**
 * Container services for framework internals and plugins. This interface is a service provider interface and is not intended
 * for applications consumption. It deals with container extensions, and managed classes and methods.
 * 
 * @author Iulian Rotaru
 */
public interface IContainer {

	/**
	 * Retrieve a not null managed instance, be it newly created or reused from a scope cache. Throws an implementation specific
	 * runtime exception if there is no binding for requested interface.
	 * 
	 * Instance to retrieve is identified by the interface class. Here the term interface class is used in a broad sense. It is
	 * not necessary to be a Java interface; it can be an abstract or even a concrete one. The point is that this interface
	 * class identifies the instance at injector bindings level.
	 * 
	 * Depending on injector binding and current context a new instance can be created or one can be reused from a provider
	 * scope cache.
	 * 
	 * @param interfaceClass interface class used to identify the instance.
	 * @return instance, newly created or reused from scope.
	 * @throws RuntimeException implementation specific runtime exception if fail to obtain the instance.
	 * @param <T> instance generic type.
	 */
	<T> T getInstance(Class<T> interfaceClass);

	/**
	 * Convenient alternative of {@link #getInstance(Class)} that returns null if implementation not found. Use this factory
	 * method version when implementation is not defined at application build but loaded at run-time. If implementation is not
	 * available this method returns null instead of throwing runtime exception.
	 * 
	 * @param interfaceClass interface class used to identify the instance.
	 * @return instance, newly created or reused from scope.
	 * @throws RuntimeException implementation specific runtime exception if fail to obtain the instance.
	 * @param <T> instance generic type.
	 */
	<T> T getOptionalInstance(Class<T> interfaceClass);

	/**
	 * Retrieve instance of requested managed class. Depending on managed class scope a new managed instance can be created or
	 * it can be reused from caches. If instance is newly created this factory method takes care to resolve all instance
	 * dependencies and execute post-construct method.
	 * 
	 * Under normal circumstances this factory method always succeed and does return a not null instance. Implementation should
	 * fail only for severe errors like out of memory or similar extreme conditions. Anyway, since instance creation may involve
	 * user defined constructors is possible also to throw {@link InvocationTargetException}. Also if requested interface is a
	 * service and no provider found at run-time this factory method throws {@link NoProviderException}.
	 * 
	 * @param managedClass managed class to return instance for.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * 
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws NoProviderException if interface is a service and no provider found on run-time.
	 * @param <T> instance generic type.
	 */
	<T> T getInstance(IManagedClass<T> managedClass);

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
