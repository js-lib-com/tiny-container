package js.tiny.container;

import js.converter.Converter;
import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;
import js.tiny.container.core.AppContext;
import js.tiny.container.core.AppFactory;
import js.tiny.container.core.SecurityContext;

/**
 * Container services for framework internals and plugins. This interface is a service provider interface and is not intended
 * for applications consumption.
 * <p>
 * Beside services provided by inherited interfaces container SPI deals mainly with managed classes and methods.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ContainerSPI extends AppFactory, SecurityContext {
	/**
	 * Retrieve instance of requested managed class. Depending on managed class scope a new managed instance can be created or
	 * it can be reused from caches. Optional constructor arguments are used only if a new local managed instance is created.
	 * <p>
	 * Under normal circumstances this factory method always succeed and does return a not null instance. Implementation should
	 * fail only for severe errors like out of memory or similar extreme conditions. Anyway, since instance creation may involve
	 * user defined constructors is possible also to throw {@link InvocationException}. Also if requested interface is a service
	 * and no provider found at run-time this factory method throws {@link NoProviderException}. Finally, this factory method
	 * may also fail for other conditions that are related to development stage and considered bugs.
	 * 
	 * @param managedClass managed class to return instance for,
	 * @param args optional implementation constructor arguments, if new local instance should be created.
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
	<T> T getInstance(ManagedClassSPI managedClass, Object... args);

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
	ManagedClassSPI getManagedClass(Class<?> interfaceClass);

	/**
	 * Get all managed classes registered to this container.
	 * 
	 * @return container managed classes, in no particular order.
	 */
	Iterable<ManagedClassSPI> getManagedClasses();

	/**
	 * Get all managed methods, from all managed classes, registered to this container.
	 * 
	 * @return container managed methods, in no particular order.
	 */
	Iterable<ManagedMethodSPI> getManagedMethods();

	Iterable<ManagedMethodSPI> getNetMethods();

	/**
	 * Get basic authentication realm. If realm is not defined into application descriptor uses context name.
	 * <p>
	 * Basic authentication realm is loaded from application descriptor, <code>login</code> section.
	 * 
	 * <pre>
	 * &lt;login&gt;
	 * 	&lt;property name="realm" value="Fax2e-mail" /&gt;
	 * 	...
	 * &lt;/login&gt;
	 * </pre>
	 * 
	 * @return basic authentication realm
	 */
	String getLoginRealm();

	/**
	 * Get location for application login page, relative or absolute to servlet container root.
	 * <p>
	 * Login page location is loaded from application descriptor, <code>login</code> section.
	 * 
	 * <pre>
	 * &lt;login&gt;
	 * 	...
	 * 	&lt;property name="page" value="index.htm" /&gt;
	 * &lt;/login&gt;
	 * </pre>
	 * 
	 * @return login page location or null if not login page declared.
	 */
	String getLoginPage();

	/**
	 * Set context property, mainly for testing purposes. If given value is not already a string it is converter using
	 * {@link Converter#asString(Object)}. Set value is retrievable via {@link AppContext}.
	 * 
	 * @param name property name,
	 * @param value property value.
	 * @throws ConverterException if <code>value</code> object cannot be converter to string.
	 */
	void setProperty(String name, Object value);
}
