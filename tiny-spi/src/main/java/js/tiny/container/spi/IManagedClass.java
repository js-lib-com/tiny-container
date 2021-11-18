package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import js.lang.InvocationException;
import js.lang.NoProviderException;

/**
 * Service provider interface for managed class. Although public, this interface is designed for library internal usage.
 * Application should consider this interface as volatile and subject to change without notice.
 * 
 * Basically this interface provides utility methods for miscellaneous library needs. Its existing rationale is to hide managed
 * class implementation as much as possible.
 * 
 * @author Iulian Rotaru
 */
public interface IManagedClass<T> {

	/**
	 * Get parent container that created this managed class.
	 * 
	 * @return parent container.
	 */
	IContainer getContainer();

	/**
	 * Get the key uniquely identifying this managed class. Returned key is created incrementally, but not necessarily in
	 * sequence, and can be used to sort managed classes in creation order; creation order is the order of class descriptor
	 * declarations on application descriptor.
	 * 
	 * @return managed class unique key.
	 */
	Integer getKey();

	/**
	 * Get managed interface class. Note that returned class is not mandatory Java interface. Here term <code>interface</code>
	 * denotes a class that identify managed class and indeed usually is Java interface. Anyway, it can be as well an abstract
	 * or a concrete base class. The point is, managed class implementation must implement or extend this
	 * <code>interface</code>.
	 * 
	 * @return managed class interface.
	 */
	Class<T> getInterfaceClass();

	/**
	 * Get optional implementation class, possible null. Not all managed classes require implementation, e.g. managed classes of
	 * {@link InstanceType#REMOTE} or {@link InstanceType#SERVICE} types. Anyway, if instance type requires so, implementation
	 * class should be not null.
	 * 
	 * @return managed class implementation, possible null.
	 * @see InstanceType#requiresImplementation()
	 */
	Class<? extends T> getImplementationClass();

	/**
	 * Get managed methods owned by this managed class, sequence with no order guaranteed and possible empty. Note that managed
	 * class is not required to create managed methods for every method declared on interface classes. For example a managed
	 * class may have only few methods declared remotely accessible and will create managed methods only for those.
	 * 
	 * @return managed methods sequence, in no particular order and possible empty.
	 */
	Iterable<IManagedMethod> getManagedMethods();

	/**
	 * Get managed method by name. This getter tries to locate named managed method declared by this managed class and return
	 * it; if not found returns null.
	 * 
	 * @param methodName the name of managed method.
	 * @return requested managed method, possible null.
	 */
	IManagedMethod getManagedMethod(String methodName);

	/**
	 * Gets an instance of this managed class implementation, newly created or reused for scope caches. If a new instance is
	 * indeed created this factory method takes care to resolve and inject all instance dependencies and execute post-construct
	 * processors.
	 * 
	 * Under normal circumstances this factory method always succeed and does return a not null instance. Implementation should
	 * fail only for severe errors like out of memory or similar extreme conditions. Anyway, since instance creation may involve
	 * user defined constructors is possible also to throw {@link InvocationTargetException}. Also if requested interface is a
	 * service and no provider found at run-time this factory method throws {@link NoProviderException}.
	 *
	 * @return this managed class instance.
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws NoProviderException if interface is a service and no provider found on run-time.
	 */
	T getInstance();

	<A extends Annotation> A scanAnnotation(Class<A> type);

	<A extends Annotation> A getAnnotation(Class<A> type);

}
