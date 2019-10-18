package js.tiny.container.core;

import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;
import js.rmi.RemoteFactory;
import js.rmi.UnsupportedProtocolException;
import js.tiny.container.InstanceFactory;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;

/**
 * Managed instances factory with application scope. A managed instance is one created from a managed class. Managed instances
 * are not created with Java new operator but handled by this factory based on properties from managed class. There is a single
 * application factory implementation per application. Into web contexts, where multiple applications can be deployed, there is
 * an application factory for each web application instance.
 * <p>
 * This interface is the main front-end for managed instances <code>retrieval</code>. It is named <code>retrieval</code> and not
 * <code>creation</code> because an instance is not always created. It can be reused from scope caches, loaded using standard
 * Java service loader or proxied to a remote class, depending on managed class scope. To retrieve a managed instance one should
 * know its registered interface. It is also possible to have multiple instances of the same interface in which case caller
 * should provide the instance name. Is is considered a bug if supplied interface is not registered. Registration is
 * implementation detail but it should be able to locate instances based on interface class and optional instance name.
 * <p>
 * Main factory method is {@link #getInstance(Class, Object...)} and its named variant
 * {@link #getInstance(String, Class, Object...)}. They always return managed instances, reused or fresh created but never null.
 * Application is not required to test returned value but if managed instance retrieval fails unchecked exception or error is
 * thrown. Failing conditions are almost exclusively development or deployment mistakes. For a sound deployment, on production
 * only out of memory could happen.
 * <p>
 * Mentioned factory methods fail if requested managed class is not registered or a service provider is not found. If this
 * behavior is not desirable there is {@link #getOptionalInstance(Class, Object...)} that returns null in such conditions. It is
 * application responsibility to check returned value and take recovery measures.
 * <p>
 * Application factory is able to retrieve instances for remotely deployed classes. In this case managed class type is declared
 * to be <code>REMOTE</code> and factory implementation creates a Java Proxy that knows HTTP-RMI. This proxy is returned by
 * above factory methods and application can invoke methods directly on it, as if would be local instance. A managed class
 * declared as remote should have a property that configure URL where remote class is deployed. If this URL is missing
 * implementation should perform discovery.
 * <p>
 * For special case when remote class URL is obtained at run-time, perhaps from user interface, there is
 * {@link #getRemoteInstance(String, Class)} that retrieve a remote instance for specified URL.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface AppFactory extends RemoteFactory {
	/**
	 * Return instance for requested interface, newly created or reused but never null, throwing exception if implementation not
	 * found. If requested interface is not registered this factory method throws bug error. Depending on managed class
	 * {@link InstanceScope}, new instance can be created on the fly or reused from scope specific pools. If instance should be
	 * created, delegates managed class. Please note that optional constructor arguments are actually used only if instance is
	 * created by this call; if instance is reused constructor arguments are simply ignored. Also note that not all
	 * implementations does support constructor arguments, see {@link InstanceFactory}.
	 * <p>
	 * This factory method consider missing managed class as logic flaw. So if one dares to request an instance for a not
	 * registered interface {@link BugError} will be thrown. If this behavior is not desirable, there if
	 * {@link #getOptionalInstance(Class, Object...)} method that returns null is requested interface has no implementation.
	 * <p>
	 * At least theoretically this factory method always succeed and does return a not null instance. Implementation should fail
	 * only for severe errors like out of memory or similar extreme conditions. Anyway, since instance creation may involve user
	 * defined constructors is possible also to throw {@link InvocationException}. Finally if requested interface is a service
	 * and no provider found at run-time this factory method throws {@link NoProviderException}.
	 * <p>
	 * To conclude, recommended use case is to just use returned instance and let JVM fail since all failing conditions are more
	 * or less for developer or deployer and should never happen in production.
	 * 
	 * @param interfaceClass requested interface class,
	 * @param args optional constructor arguments.
	 * @param <T> managed class implementation.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * @throws IllegalArgumentException if <code>interfaceClass</code> argument is null.
	 * @throws IllegalArgumentException if <code>args</code> argument does not respect constructor signature or implementation
	 *             does not support arguments but caller provides them.
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
	<T> T getInstance(Class<? super T> interfaceClass, Object... args);

	/**
	 * Alternative for instance retrieval that allows for multiple instances per scope. Usually, for a given scope - except
	 * {@link InstanceScope#LOCAL}, there can be a single managed instance. If {@link #getInstance(Class, Object...)} is called
	 * multiple times in a given scope the same managed instance is reused. Named instances allow for multiple instances of a
	 * given interface class but still reused by name in its scope. It is clearly that does not make sense to used names on
	 * local instances, although there is no formal restriction.
	 * <p>
	 * When used on a managed class with {@link InstanceType#REMOTE} type, instance name is used for discovery, even if remote
	 * class URL is defined into class descriptor.
	 * <p>
	 * In other respects this method behaves identically {@link #getInstance(Class, Object...)}.
	 * 
	 * @param instanceName instance name,
	 * @param interfaceClass requested interface class,
	 * @param args optional constructor arguments.
	 * @param <T> managed class implementation.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * @throws IllegalArgumentException if <code>instanceName</code> argument is null or empty or <code>interfaceClass</code>
	 *             argument is null.
	 * @throws IllegalArgumentException if <code>args</code> argument does not respect constructor signature or implementation
	 *             does not support arguments but caller provides them.
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
	<T> T getInstance(String instanceName, Class<? super T> interfaceClass, Object... args);

	/**
	 * Convenient instance retrieval alternative that returns null if implementation not found. Use this factory method version
	 * when implementation is not defined at application build but loaded at run-time. If implementation is not available this
	 * method returns null instead of throwing exception.
	 * <p>
	 * If implementation is found this factory method behaves like {@link #getInstance(Class, Object...)} including exceptions
	 * related to arguments and other failing conditions.
	 * 
	 * @param interfaceClass requested interface class,
	 * @param args optional implementation constructor arguments.
	 * @return managed instance or null if no implementation found.
	 * @param <T> managed class implementation.
	 * @throws IllegalArgumentException if <code>interfaceClass</code> argument is null.
	 * @throws IllegalArgumentException if <code>args</code> argument does not respect constructor signature or implementation
	 *             does not support arguments but caller provides them.
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws ConverterException if attempt to initialize a field with a type for which there is no converter,
	 * @throws BugError if dependency value cannot be created or circular dependency is detected.
	 * @throws BugError if instance configuration fails either due to bad configuration object or fail on configuration user
	 *             defined logic.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 * @throws BugError if attempt to assign field to not POJO type.
	 */
	<T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args);

	/**
	 * Create a Java Proxy instance for a remote deployed class. As result from sample code below, one needs to know remote
	 * application context URL where class is deployed and have local interface for remote services. Interface to remote
	 * services is not required to define all services supplied by remote class. It's enough to declare only those used in this
	 * particular context; but methods signature should be respected, including package name. The recommended use case is for
	 * service provider to supply a SPI package so that remote class interface signature is guaranteed.
	 * 
	 * <pre>
	 * interface WeatherService {
	 *    Weather getCurrentWeather(double latitude, double longitude);
	 * }
	 * ...
	 * String implementationURL = &quot;http://weather.com/&quot;;
	 * WeatherService service = appFactory.getInstance(implementationURL, WeatherService.class);
	 * Weather weather = service.getCurrentWeather(47.1569, 27.5903);
	 * </pre>
	 * 
	 * Implementation will return a Java Proxy instance able to send class and method names and actual arguments to remote
	 * server and to retrieve returned value, using HTTP-RMI protocol. It is not required to perform URL validity and is caller
	 * responsibility to ensure given implementation URL points to and existing remote class and that remote class actually
	 * implements given interface. If given arguments does not match an existing remote class there will be exception on actual
	 * remote method invocation.
	 * <p>
	 * This remote factory method is designed to be used when implementation URL is retrieved at runtime, perhaps via user
	 * interface or some custom discovery mechanism. Otherwise uses standard managed instance factory methods and declare remote
	 * interface as managed class of type {@link InstanceType#REMOTE}, see class description.
	 * 
	 * @param implementationURL the URL of remote implementation,
	 * @param interfaceClass interface implemented by remote class.
	 * @param <T> managed class implementation.
	 * @return remote class proxy instance.
	 * @throws IllegalArgumentException if either argument is null or <code>interfaceClass</code> argument is not actually an
	 *             interface.
	 * @throws UnsupportedProtocolException if URL protocol is not supported.
	 */
	<T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass);
}
