package com.jslib.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import com.jslib.lang.InvocationException;
import com.jslib.lang.NoProviderException;

/**
 * Managed class implements class and instance container services and facilitates remote access to business methods, via
 * reflection. A managed class is created by a parent container and is a this wrapper for Java class implementing application
 * business logic. Managed class is immutable.
 * 
 * Container services are provided at relevant extension points and are grouped into:
 * <ul>
 * <li>{@link IClassPostLoadedProcessor}: services executed after managed class creation,
 * <li>{@link IInstancePostConstructProcessor}: services executed after instances creation,
 * <li>{@link IInstancePreDestroyProcessor}: services executed before instances destruction.
 * 
 * Container services are implemented as extension modules - discovered and loaded at runtime with standard Java service loader,
 * and custom services are supported. At application code level container services are declared with Java annotations with
 * runtime retention. Managed class provides an utility method for annotations scanning - see {@link #scanAnnotation(Class)}.
 * 
 * A managed class has a bind declared to container injector with a scope depending on application needs. There is a method to
 * retrieve managed class instance, see {@link #getInstance()}. On injector bindings managed class is identified by
 * <code>interface class</code>, see {@link #getInterfaceClass()}.
 * 
 * Managed class provides access to implementation methods via method name allowing business method invocation via reflection.
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
	 * Gets managed methods owned by this managed class. Managed methods are those defined by implementation class and are not
	 * static.
	 * 
	 * @return managed methods collection, in no particular order.
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
