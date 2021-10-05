package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.ejb.Remote;
import javax.ejb.Schedule;
import javax.inject.Inject;

import js.annotation.ContextParam;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ManagedLifeCycle;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;

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
	 * Get this managed class dependencies, possible empty. Dependency fields are marked with {@link Inject} annotation.
	 * Returned sequence is in a no particular order.
	 * 
	 * @return managed class dependencies, possible empty.
	 */
	Iterable<Field> getDependencies();

	/**
	 * Get managed methods owned by this managed class, sequence with no order guaranteed and possible empty. Note that managed
	 * class is not required to create managed methods for every method declared on interface classes. For example a managed
	 * class may have only few methods declared remotely accessible and will create managed methods only for those.
	 * 
	 * @return managed methods sequence, in no particular order and possible empty.
	 */
	Iterable<IManagedMethod> getManagedMethods();

	Iterable<IManagedMethod> getNetMethods();

	IManagedMethod getPostConstructMethod();

	IManagedMethod getPreDestroyMethod();

	/**
	 * Get managed method wrapping requested Java method. This getter is designed to be used with managed classes of
	 * {@link InstanceType#PROXY} type. Attempting to use with other types is considered a bug.
	 * 
	 * @param method Java method to search for wrapper managed method.
	 * @return managed method or null if not found.
	 * @throws NoSuchMethodException if there is no managed method wrapping requested Java method.
	 * @throws BugError if attempt to use this getter on not {@link InstanceType#PROXY} types.
	 */
	IManagedMethod getManagedMethod(Method method) throws NoSuchMethodException;

	/**
	 * Get managed method by name, method that should be usable for remote requests. This getter tries to locate named managed
	 * method declared by this managed class and return it; if not found returns null. Returned managed method is guaranteed to
	 * be remotely accessible.
	 * 
	 * @param methodName the name of managed method intended for remote access.
	 * @return requested managed method, possible null.
	 */
	IManagedMethod getNetMethod(String methodName);

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
	 * Test if this managed class is remotely accessible, that is, is a net class. A managed class is remotely accessible if it
	 * is tagged so with {@link Remote} annotation or has at least one method accessible remote, see
	 * {@link IManagedMethod#isRemotelyAccessible()}.
	 * <p>
	 * A remotely accessible managed class is also knows as <code>net class</code>.
	 * 
	 * @return true only if this managed class is remotely accessible.
	 */
	boolean isRemotelyAccessible();

	/**
	 * Remote class implementation URL as declared into managed class descriptor. This value has meaning only if managed class
	 * is of {@link InstanceType#REMOTE} type.
	 * 
	 * @return remote class implementation URL.
	 */
	String getImplementationURL();

	/**
	 * Flag indicating that this managed class should be instantiated automatically by container. Note that created instance is
	 * a singleton and managed instance scope should be {@link InstanceScope#APPLICATION}.
	 * <p>
	 * This flag is true for following conditions:
	 * <ul>
	 * <li>this managed class has {@link Schedule} methods,
	 * <li>this managed class implements {@link ManagedLifeCycle} interface.
	 * </ul>
	 * 
	 * @return true if managed instance should be created automatically by container.
	 */
	boolean isAutoInstanceCreation();

	/**
	 * Get managed class fields annotated with {@link ContextParam} annotation. Map key is the context parameter name. This
	 * fields will be initialized from container runtime context by {@link ContextParamProcessor}. Note that both static and
	 * instance fields are acceptable.
	 * 
	 * @return managed class fields that should be initialized from container runtime context.
	 */
	Map<String, Field> getContextParamFields();

	<T extends IContainerServiceMeta> T getServiceMeta(Class<T> type);
	
	<T extends Annotation> T getAnnotation(Class<T> type);
}
