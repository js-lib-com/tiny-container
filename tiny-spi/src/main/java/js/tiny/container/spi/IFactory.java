package js.tiny.container.spi;

import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;

/**
 * Managed instances factory with application scope. A managed instance is one created from a managed class. Managed instances
 * are not created with Java new operator but handled by this factory based on properties from managed class. There is a single
 * application factory implementation per application. Into web contexts, where multiple applications can be deployed, there is
 * an application factory for each web application instance.
 * 
 * This interface is the main front-end for managed instances <code>retrieval</code>. It is named <code>retrieval</code> and not
 * <code>creation</code> because an instance is not always created. It can be reused from scope caches, loaded using standard
 * Java service loader or proxied to a remote class, depending on managed class scope. To retrieve a managed instance one should
 * know its registered interface. It is also possible to have multiple instances of the same interface in which case caller
 * should provide the instance name. Is is considered a bug if supplied interface is not registered. Registration is
 * implementation detail but it should be able to locate instances based on interface class and optional instance name.
 * 
 * Main factory method is {@link #getInstance(Class)} and its named variant {@link #getInstance(String, Class)}. They always
 * return managed instances, reused or fresh created but never null. Application is not required to test returned value but if
 * managed instance retrieval fails unchecked exception or error is thrown. Failing conditions are almost exclusively
 * development or deployment mistakes. For a sound deployment, on production only out of memory could happen.
 * 
 * Mentioned factory methods fail if requested managed class is not registered or a service provider is not found. If this
 * behavior is not desirable there is {@link #getOptionalInstance(Class)} that returns null in such conditions. It is
 * application responsibility to check returned value and take recovery measures.
 * 
 * Application factory is able to retrieve instances for remotely deployed classes. In this case managed class type is declared
 * to be <code>REMOTE</code> and factory implementation creates a Java Proxy that knows HTTP-RMI. This proxy is returned by
 * above factory methods and application can invoke methods directly on it, as if would be local instance. A managed class
 * declared as remote should have a property that configure URL where remote class is deployed. If this URL is missing
 * implementation should perform discovery.
 * 
 * For special case when remote class URL is obtained at run-time, perhaps from user interface, there is
 * {@link #getRemoteInstance(String, Class)} that retrieve a remote instance for specified URL.
 * 
 * @author Iulian Rotaru
 */
public interface IFactory {

	/**
	 * Return instance for requested interface, newly created or reused but never null, throwing exception if implementation not
	 * found. If requested interface is not registered this factory method throws bug error. Depending on managed class
	 * {@link InstanceScope}, new instance can be created on the fly or reused from scope specific pools. If instance should be
	 * created, delegates managed class.
	 * 
	 * If instance is newly created this factory method takes care to resolve all instance dependencies - at field, constructor
	 * and method level, and execute post-construct method, of course if defined.
	 * 
	 * This factory method consider missing managed class as logic flaw. So if one dares to request an instance for a not
	 * registered interface {@link BugError} will be thrown. If this behavior is not desirable, there if
	 * {@link #getOptionalInstance(Class)} method that returns null is requested interface has no implementation.
	 * 
	 * At least theoretically this factory method always succeed and does return a not null instance. Implementation should fail
	 * only for severe errors like out of memory or similar extreme conditions. Anyway, since instance creation may involve user
	 * defined constructors is possible also to throw {@link InvocationException}. Finally if requested interface is a service
	 * and no provider found at run-time this factory method throws {@link NoProviderException}.
	 * 
	 * To conclude, recommended use case is to just use returned instance and let JVM fail since all failing conditions are more
	 * or less for developer or deployer and should never happen in production.
	 * 
	 * @param interfaceClass requested interface class.
	 * @param <T> managed class implementation.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * @throws IllegalArgumentException if <code>interfaceClass</code> argument is null.
	 * @throws NoProviderException if interface is a service and no provider found on run-time.
	 * @throws BugError if no implementation found for requested interface class.
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws ConverterException if attempt to initialize a field with a type for which there is no converter,
	 * @throws BugError if dependency value cannot be created or circular dependency is detected.
	 * @throws BugError if instance configuration fails either due to bad configuration object or fail on configuration user
	 *             defined logic.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 * @throws BugError if attempt to assign field to not POJO type.
	 */
	<T> T getInstance(Class<? super T> interfaceClass);

	/**
	 * Convenient instance retrieval alternative that returns null if implementation not found. Use this factory method version
	 * when implementation is not defined at application build but loaded at run-time. If implementation is not available this
	 * method returns null instead of throwing exception.
	 * 
	 * If implementation is found this factory method behaves like {@link #getInstance(Class)} including dependencies,
	 * post-construct and failing conditions.
	 * 
	 * @param interfaceClass requested interface class.
	 * @return managed instance or null if no implementation found.
	 * @param <T> managed class implementation.
	 * @throws IllegalArgumentException if <code>interfaceClass</code> argument is null.
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws ConverterException if attempt to initialize a field with a type for which there is no converter,
	 * @throws BugError if dependency value cannot be created or circular dependency is detected.
	 * @throws BugError if instance configuration fails either due to bad configuration object or fail on configuration user
	 *             defined logic.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 * @throws BugError if attempt to assign field to not POJO type.
	 */
	<T> T getOptionalInstance(Class<? super T> interfaceClass);
	
}
