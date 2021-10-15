package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collection;

import js.lang.BugError;
import js.lang.Config;
import js.tiny.container.core.InstanceScope;
import js.tiny.container.core.InstanceType;

/**
 * Service provider interface for managed class. Although public, this interface is designed for library internal usage.
 * Application should consider this interface as volatile and subject to change without notice.
 * <p>
 * Basically this interface provides utility methods for miscellaneous library needs. Its existing rationale is to hide managed
 * class implementation as much as possible.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface IManagedClass {
	/**
	 * Get parent container that created this managed class.
	 * 
	 * @return parent container.
	 */
	IContainer getContainer();

	Collection<IContainerService> getServices();

	/**
	 * Get the key uniquely identifying this managed class. Returned key is created incrementally, but not necessarily in
	 * sequence, and can be used to sort managed classes in creation order; creation order is the order of class descriptor
	 * declarations on application descriptor.
	 * 
	 * @return managed class unique key.
	 */
	Integer getKey();

	/**
	 * Get optional configuration object or null if no configuration section present into application descriptor. Every managed
	 * class has a name that is not required to be unique. A configuration section is an element under application descriptor
	 * root with managed class name. Since managed class name is not unique is possible to share a configuration section.
	 * <p>
	 * This method returns configuration section parsed into a configuration object. Returns null if there is no configuration
	 * section for this particular managed class.
	 * 
	 * @return this managed class configuration object, possible null.
	 */
	Config getConfig();

	/**
	 * Get managed interface classes. Note that returned classes are not mandatory Java interfaces. Here term
	 * <code>interface</code> denotes a class that identify managed class and usually is Java interface. Anyway, it can be as
	 * well an abstract or a concrete base class. The point is, managed class implementation must implement or extend this
	 * <code>interface</code>.
	 * 
	 * @return managed interface classes.
	 */
	Class<?>[] getInterfaceClasses();

	/**
	 * Convenient way to retrieve managed interface class when there is a single one. Useable when managed instance is
	 * guaranteed to have a single interface, e.g. a managed class of {@link InstanceType#REMOTE} type has always a single
	 * interface.
	 * <p>
	 * As with {@link #getInterfaceClasses()} returned class is not mandatory to be an actual Java interface. It can be for
	 * example an abstract or even concrete base class. {@link IManagedClass} uses <code>interface</code> term in a broader
	 * sense: it is the class that identify the managed class.
	 * 
	 * @return managed class interface.
	 * @throws BugError if attempt to use this getter on a managed class with multiple interfaces.
	 */
	Class<?> getInterfaceClass();

	/**
	 * Get optional implementation class, possible null. Not all managed classes require implementation, e.g. managed classes of
	 * {@link InstanceType#REMOTE} or {@link InstanceType#SERVICE} types. Anyway, if instance type requires so, implementation
	 * class should be not null.
	 * 
	 * @return managed class implementation, possible null.
	 * @see InstanceType#requiresImplementation()
	 */
	Class<?> getImplementationClass();

	/**
	 * Get implementation constructor. Constructor has meaningful value only for local managed classes, that is managed classes
	 * with implementation. Anyway, if {@link #getImplementationClass()} returns not null this getter should also return not
	 * null value.
	 * 
	 * @return implementation constructor, possible null.
	 */
	Constructor<?> getConstructor();

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
	 * Get managed instance scope. There are predefined scope values, see {@link InstanceScope}, but user defined scopes are
	 * supported.
	 * 
	 * @return managed class scope.
	 */
	InstanceScope getInstanceScope();

	/**
	 * Get managed instance type. There are predefined instance type values, see {@link InstanceType}, but user defined types
	 * are supported.
	 * 
	 * @return managed class type.
	 */
	InstanceType getInstanceType();

	/**
	 * Remote class implementation URL as declared into managed class descriptor. This value has meaning only if managed class
	 * is of {@link InstanceType#REMOTE} type.
	 * 
	 * @return remote class implementation URL.
	 */
	String getImplementationURL();

	<T extends Annotation> T getAnnotation(Class<T> type);

	<T extends IServiceMeta> T getServiceMeta(Class<T> type);

	void setAttribute(Object context, String name, Object value);

	<T> T getAttribute(Object context, String name, Class<T> type);
}
