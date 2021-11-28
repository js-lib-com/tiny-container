package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import js.lang.InvocationException;
import js.lang.NoProviderException;

/**
 * Managed class provides extension points for class and instance services and facilitates remote access to business methods,
 * via reflection.
 * 
 * @author Iulian Rotaru
 */
public interface IManagedClass<T> {

	/**
	 * Gets parent container that creates this managed class.
	 * 
	 * @return parent container.
	 */
	IContainer getContainer();

	/**
	 * Gets managed interface class. Note that returned class is not mandatory Java interface. Here term <code>interface</code>
	 * denotes a class that identify managed class and indeed usually is Java interface. Anyway, it can be as well an abstract
	 * or a concrete base class. The point is, managed class implementation must implement or extend this
	 * <code>interface</code>.
	 * 
	 * @return managed class interface.
	 */
	Class<T> getInterfaceClass();

	/**
	 * Gets implementation class, extending {@link #getInterfaceClass()}.
	 * 
	 * @return managed class implementation.
	 */
	Class<? extends T> getImplementationClass();

	/**
	 * Gets managed methods owned by this managed class.
	 * 
	 * @return managed methods sequence, in no particular order and possible empty.
	 */
	Collection<IManagedMethod> getManagedMethods();

	/**
	 * Gets managed method by name. This getter tries to locate named managed method declared by this managed class and returns
	 * it; if not found returns null.
	 * 
	 * @param methodName simple managed method name, as returned by {@link IManagedMethod#getName()}.
	 * @return requested managed method or null if no method with requested name.
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

	/**
	 * Scan managed class annotation and return it or null if annotation not present. Should scan for requested annotation on
	 * both implementation and interface classes, in this order. Interface should be that declared by this managed class - see
	 * {@link #getInterfaceClass()}, not detected from wrapped Java class.
	 * 
	 * @param annotationClass annotation class.
	 * @return annotation instance or null if not present.
	 * @param <A> annotation generic type.
	 */
	<A extends Annotation> A scanAnnotation(Class<A> annotationClass);

}
