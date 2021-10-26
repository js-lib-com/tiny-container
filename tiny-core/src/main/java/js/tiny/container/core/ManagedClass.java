package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.Path;

import js.converter.Converter;
import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.InstanceFactory;
import js.tiny.container.cdi.ScopeFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
import js.util.Classes;
import js.util.Strings;
import js.util.Types;

/**
 * Managed class resemble Java {@link Class} and is configurable from external class descriptor and by annotations. Class
 * descriptor is a configuration element from <code>managed-classes</code> section from application descriptor. Managed class
 * parses and validates class descriptor and annotations and initialize its state accordingly; managed class is immutable. Also
 * takes care to parse managed class configuration, if exist into application descriptor, see below <code>Descriptor</code>
 * section. Anyway, managed class has no means to create instances, like Java class has. This is because instance creation
 * algorithm is implemented into container and exposes by {@link IFactory} interface.
 * <p>
 * As stated, managed class parses given class descriptor and scans all interfaces and implementation classes annotations in
 * order to initialize its internal state. Since is immutable, initialization is fully completed on construction, there is no
 * setters. If constructor fails to validate its internal state throws {@link ConfigException} and container creation aborts.
 * <p>
 * Managed class holds information used by container for managed instances life cycle, that is, instances creation or reusing,
 * initialization and instance declarative services. Here are managed class core properties and related class descriptor
 * attributes, used by container for instances life cycle:
 * <ul>
 * <li>{@link #interfaceClasses} used to identify managed instances - initialized from <code>interface</code> attribute or child
 * element,
 * <li>{@link #implementationClass} used to create instances when managed class is local - initialized from <code>class</code>
 * attribute,
 * <li>{@link #instanceType} selector for {@link InstanceFactory} strategy - initialized from <code>type</code> attribute,
 * <li>{@link #instanceScope} selector for {@link ScopeFactory} strategy - initialized from <code>scope</code> attribute.
 * </ul>
 * <p>
 * Container uses interface classes to identify managed classes in classes pool. For this reason is not possible to use the same
 * interface class for two different managed classes.
 * <p>
 * Managed class has an interface and associated implementation. Usually there is only one interface but support for multiple
 * interface exists. Note that <code>interface</code> concept is not identical with Java interface. It is in fact the class used
 * to identify managed class and related instances; <code>interfaceClass</code> parameter from
 * {@link IFactory#getInstance(Class, Object...)} refers to this <code>interface</code>. In most cases it is indeed a Java
 * interface but can be abstract class or even standard Java class. Implementation class is optional depending on
 * {@link #instanceType}. Anyway, if implementation exists it must be an instantiable class, package private accepted.
 * 
 * <h3>Class Descriptor</h3> Managed classes use class descriptors to declare managed class interface(s), implementation, type
 * and scope. In application descriptor there is a predefined <code>managed-classes</code> section and inside it all managed
 * classes are declared, an element per class - this configuration element is the class descriptor. Every managed class
 * descriptor has a name, i.e. the element tag; this name is used to declare managed class configuration section.
 * <p>
 * Lets consider a UserManagerImpl class that implements UserManager interface. Into application descriptor there is
 * <code>managed-classes</code> section used to declare all managed classes. Below <code>user-manager</code> section is managed
 * class specific configuration.
 * 
 * <pre>
 * class UserManagerImpl implements UserManager {
 * 	static File USERS_PATH;
 * }
 * 
 * // application descriptor
 * ...
 * // managed classes section
 * &lt;managed-classes&gt;
 * 	// class descriptor for managed class
 * 	&lt;user-manager interface="js.admin.UserManager" implementation="js.admin.UserManagerImpl" type="POJO" scope="APPLICATION" /&gt;
 * 	...
 * &lt;/managed-classes&gt;
 * 
 * // managed class configuration
 * &lt;user-manager&gt;
 * 	&lt;static-field name="USERS_PATH" value="/usr/share/tomcat/conf/users" /&gt;
 * &lt;/manager&gt;
 * </pre>
 * 
 * In above sample code, descriptor class for js.admin.UserManager has <code>user-manager</code> name. This name is used to
 * declare managed class configuration - the below section with the same name. Class configuration section is supplied to
 * {@link Configurable#config(Config)}, of course if managed class implements {@link Configurable}. Every managed class
 * configuration section has a specific content that has meaning only to owning managed class.
 * <p>
 * Managed classes support multiple interfaces. In sample, transactional resource is a single POJO instance with application
 * scope. It is accessible by both its own class and by transaction context interface. No mater which interface is used to
 * retrieve the managed instance, the same managed class is used in the end.
 * 
 * <pre>
 * &lt;data-source class="js.core.TransactionalResource"&gt;
 * 	&lt;interface name="js.core.TransactionalResource" /&gt;
 * 	&lt;interface name="js.core.TransactionContext" /&gt;
 * &lt;/data-source&gt;
 * </pre>
 * <p>
 * Also, a managed instance can be loaded from a service using Java service loader. In this case managed class has no
 * implementation declared and has {@link InstanceType#SERVICE} type.
 * 
 * <pre>
 * &lt;transaction-manager interface="js.core.TransactionManager" type="SERVICE" /&gt;
 * </pre>
 * <p>
 * Finally, a managed class could describe a managed instance deployed on a remote host. For this uses
 * {@link InstanceType#REMOTE} type and add <code>url</code> attribute with remote host address. Managed instances created for
 * this managed class will actually be a Java proxy that delegates method invocation to a HTTP-RMI client.
 * 
 * <pre>
 * &lt;weather-service interface="ro.bbnet.WeatherService" type="REMOTE" url="http://bbnet.ro" /&gt;
 * </pre>
 * 
 * Even if <code>url</code> attribute is provided, when use {@link IFactory#getInstance(String, Class, Object...)} to retrieve
 * named instances, container will enact a discovery process based on provided instance name that in this case should be unique
 * on local network.
 * 
 * <h3 id="annotations">Annotations</h3>
 * <p>
 * Managed class supports declarative services via annotations. This implementation follows JSR-250 guidelines for annotations
 * inheritance. Basically annotations are searched in implementation class; this is true for method annotations which are
 * searched also in implementation class. For this reason only managed classes of types that require implementation may have
 * annotations, see {@link InstanceType#requiresImplementation()}.
 * <p>
 * Optionally, managed class supports an extended annotations searching scope, something resembling JAX-RS, annotation
 * inheritance. If annotation is not found in implementation class it is searched in super class or implemented interface.
 * <p>
 * For your convenience here is the list of supported annotations. For details see annotations class description.
 * <table summary="Annotations List">
 * <tr>
 * <th>Annotation Class
 * <th>Arguments
 * <th>Description
 * <th>Type
 * <th>Method
 * <th>Field
 * <tr>
 * <td>{@link Remote}
 * <td>Class request URI path
 * <td>Grant remote access from client logic to managed methods deployed in container.
 * <td>true
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link DenyAll}
 * <td>N/A
 * <td>Forbid remote access to particular methods inside a {@link Remote} accessible managed instance.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link PermitAll}
 * <td>N/A
 * <td>Remote accessible entity that do not require authorization.
 * <td>true
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Singleton}
 * <td>Class request URI path
 * <td>Configure managed instance with {@link InstanceScope#APPLICATION}.
 * <td>true
 * <td>false
 * <td>false
 * <tr>
 * <td>{@link Startup}
 * <td>N/A
 * <td>Private remote accessible managed methods, that cannot be invoked without authorization.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Stateless}
 * <td>Class request URI path
 * <td>Configure managed instance with {@link InstanceScope#LOCAL}.
 * <td>true
 * <td>false
 * <td>false
 * <tr>
 * <td>{@link Stateful}
 * <td>N/A
 * <td>Configure managed instance with {@link InstanceScope#SESSION}.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Path}
 * <td>Method request URI path
 * <td>Managed method binding to particular resource path.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Transactional}
 * <td>N/A
 * <td>Execute managed method into transactional scope.
 * <td>true
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Immutable}
 * <td>N/A
 * <td>Immutable, that is, read-only transaction.
 * <td>true
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Mutable}
 * <td>N/A
 * <td>Force mutable transaction on particular method inside a {@link Immutable} managed instance.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Asynchronous}
 * <td>N/A
 * <td>Execute managed method in a separated thread of execution.
 * <td>true
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Inject}
 * <td>N/A
 * <td>Managed instance field value injection.
 * <td>false
 * <td>false
 * <td>true
 * <tr>
 * <td>{@link Interceptors}
 * <td>interceptor class
 * <td>An intercepted managed method executes an interceptor cross-cutting logic whenever is invoked.
 * <td>true
 * <td>true
 * <td>false
 * </table>
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class ManagedClass<T> implements IManagedClass<T> {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(ManagedClass.class);

	/**
	 * Seed for managed classes key. These keys are created incrementally, but not necessarily in sequence because of concurrent
	 * applications boot, and can be used to sort managed classes in creation order; creation order is the order of class
	 * descriptor declarations on application descriptor.
	 */
	private static final AtomicInteger KEY_SEED = new AtomicInteger(0);

	/** Back reference to parent container. */
	private final Container container;

	/**
	 * Managed class key used to uniquely identifying this managed class. This key is incremental, but not necessarily in
	 * sequence, and can be used to sort managed classes in creation order.
	 */
	private final Integer key;

	/**
	 * Optional managed class configuration object. Every managed class has an name used in <code>managed-classes</code> section
	 * from application descriptor. This name can be used to name configuration section usable for every managed class that
	 * implements {@link Configurable} interface or for field initialization. This field store this configuration section parsed
	 * as a configuration object. It is null if configuration section is missing.
	 */
	private final Config config;

	/** Managed instance scope used for life span management. */
	private final InstanceScope instanceScope;

	/** Managed instance type used to select {@link InstanceFactory}. */
	private final InstanceType instanceType;

	/**
	 * Managed class interface. If class descriptor has only <code>class</code> attribute this field is initialized from
	 * {@link #implementationClass}.
	 */
	private final Class<T> interfaceClass;

	/**
	 * Optional managed class implementation has value only if {@link InstanceType#requiresImplementation()} says so. It can be
	 * null if managed class does not require implementation, for example if is a remote class.
	 */
	private final Class<? extends T> implementationClass;

	/** Cached implementation constructor or null if this managed class has no implementation. */
	private final Constructor<? extends T> constructor;

	/**
	 * Optional remote class implementation URL that can be used if this managed class is {@link InstanceType#REMOTE}. This
	 * field is loaded from <code>url</code> attribute from class descriptor.
	 */
	private final String implementationURL;

	/**
	 * Managed methods pool for managed classes of type {@link InstanceType#PROXY}. Used by {@link ManagedProxyHandler} to find
	 * out managed method bound to interface method.
	 */
	private final Map<String, IManagedMethod> methodsPool = new HashMap<>();

	/** Cached value of managed class string representation, merely for logging. */
	private final String string;

	private final Map<Class<? extends IServiceMeta>, IServiceMeta> serviceMetas = new HashMap<>();

	private final Set<IContainerService> services = new HashSet<>();

	/**
	 * Loads this managed class state from class descriptor then delegates {@link #scanAnnotations()}. Annotations scanning is
	 * performed only if this managed class type requires implementation, see {@link InstanceType#requiresImplementation()}.
	 * 
	 * @param container parent container,
	 * @param descriptor class descriptor from <code>managed-class</code> section.
	 * @throws ConfigException if configuration is invalid.
	 */
	public ManagedClass(Container container, Config descriptor) throws ConfigException {
		this.container = container;
		// if configuration section is missing this.config field remains null
		this.config = descriptor.getRoot().getChild(descriptor.getName());

		// loading order matters; do not change it
		this.instanceScope = loadInstanceScope(descriptor);
		this.instanceType = loadInstanceType(descriptor);
		this.implementationClass = loadImplementationClass(descriptor);
		this.interfaceClass = loadInterfaceClass(descriptor);
		this.implementationURL = loadImplementationURL(descriptor);

		this.key = KEY_SEED.getAndIncrement();
		this.string = buildStringRepresentation(descriptor);

		// get declared constructor return null if no implementation class
		this.constructor = getDeclaredConstructor(this.implementationClass);

		if (this.instanceType.requiresImplementation()) {
			scanAnnotations();
			initializeStaticFields();
		}
	}

	/**
	 * This managed class string representation.
	 * 
	 * @return string representation.
	 * @see #string
	 */
	@Override
	public String toString() {
		return string;
	}

	@Override
	public Set<IContainerService> getServices() {
		return services;
	}

	/**
	 * Scan annotations for managed classes that requires implementation. Annotations are processed only for managed classes
	 * that have {@link #implementationClass} that is primary source scanned for annotation. If an annotation is not present
	 * into implementation class try to find it on interface(s). See <a href="#annotations">Annotations</a> section from class
	 * description.
	 * 
	 * @throws BugError for insane conditions.
	 */
	private void scanAnnotations() {
		for (Method method : implementationClass.getDeclaredMethods()) {
			Method interfaceMethod = getInterfaceMethod(method);
			IManagedMethod managedMethod = new ManagedMethod(this, interfaceMethod);
			methodsPool.put(interfaceMethod.getName(), managedMethod);
		}

		for (IContainerService service : container.getServices()) {
			if (service instanceof IServiceMetaScanner) {
				IServiceMetaScanner scanner = (IServiceMetaScanner) service;
				for (IServiceMeta serviceMeta : scanner.scanServiceMeta(this)) {
					log.debug("Add service meta |%s| to managed class |%s|", serviceMeta.getClass(), this);
					services.add(service);
					serviceMetas.put(serviceMeta.getClass(), serviceMeta);
				}
			}
		}

		for (IManagedMethod method : methodsPool.values()) {
			ManagedMethod managedMethod = (ManagedMethod) method;
			for (IContainerService service : container.getServices()) {
				if (service instanceof IMethodInvocationProcessor) {
					managedMethod.addInvocationProcessor((IMethodInvocationProcessor) service);
				}
				if (service instanceof IServiceMetaScanner) {
					IServiceMetaScanner scanner = (IServiceMetaScanner) service;
					for (IServiceMeta serviceMeta : scanner.scanServiceMeta(managedMethod)) {
						managedMethod.addServiceMeta(serviceMeta);
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED CLASS SPI

	@Override
	public IContainer getContainer() {
		return container;
	}

	@Override
	public Integer getKey() {
		return key;
	}

	@Override
	public Config getConfig() {
		return config;
	}

	@Override
	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	@Override
	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}

	@Override
	public Constructor<? extends T> getConstructor() {
		return constructor;
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		return methodsPool.values();
	}

	@Override
	public IManagedMethod getManagedMethod(String methodName) {
		IManagedMethod managedMethod = methodsPool.get(methodName);
		if (managedMethod == null) {
			log.error("Missing remote method |%s| from |%s|.", methodName, implementationClass);
			return null;
		}
		return managedMethod;
	}

	@Override
	public InstanceScope getInstanceScope() {
		return instanceScope;
	}

	@Override
	public InstanceType getInstanceType() {
		return instanceType;
	}

	@Override
	public String getImplementationURL() {
		return implementationURL;
	}

	private final Map<String, Object> attributes = new HashMap<>();

	@Override
	public void setAttribute(Object context, String name, Object value) {
		attributes.put(key(context, name), value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A getAttribute(Object context, String name, Class<A> type) {
		String key = key(context, name);
		Object value = attributes.get(key);
		if (value == null) {
			return null;
		}
		if (!Types.isInstanceOf(value, type)) {
			throw new ClassCastException(String.format("Cannot cast attribute |%s| to type |%s|.", key, type));
		}
		return (A) value;
	}

	private static final String key(Object context, String name) {
		if (!(context instanceof Class)) {
			context = context.getClass();
		}
		return Strings.concat(((Class<?>) context).getCanonicalName(), '#', name);
	}

	// --------------------------------------------------------------------------------------------
	// CLASS DESCRIPTOR UTILITY METHODS

	/**
	 * Load optional implementation class from class descriptor and applies insanity checks. Load implementation class from
	 * <code>class</code> attribute from descriptor. If <code>class</code> attribute not present return null.
	 * <p>
	 * Beside loading implementation class this utility method performs sanity checks and throws configuration exception if:
	 * <ul>
	 * <li>instance type requires implementation but <code>class</code> attribute is missing,
	 * <li>instance type does not require implementation but <code>class</code> attribute is present,
	 * <li>class not found on run-time class path,
	 * <li>implementation class is a Java interface,
	 * <li>implementation class is abstract,
	 * <li>implementation class implements managed life cycle but instance scope is not {@link InstanceScope#APPLICATION}.
	 * </ul>
	 * 
	 * @param descriptor class descriptor.
	 * @return loaded implementation class or null.
	 * @throws ConfigException if sanity check fails.
	 */
	private Class<? extends T> loadImplementationClass(Config descriptor) throws ConfigException {
		String implementationName = descriptor.getAttribute("class");
		if (implementationName == null) {
			if (instanceType.requiresImplementation()) {
				throw new ConfigException("Managed type |%s| requires <class> attribute. See class descriptor |%s|.", instanceType, descriptor);
			}
			return null;
		}
		if (!instanceType.requiresImplementation()) {
			throw new ConfigException("Managed type |%s| forbids <class> attribute. See class descriptor |%s|.", instanceType, descriptor);
		}

		Class<? extends T> implementationClass = Classes.forOptionalName(implementationName);
		if (implementationClass == null) {
			throw new ConfigException("Managed class implementation |%s| not found.", implementationName);
		}

		if (implementationClass.isInterface()) {
			throw new ConfigException("Managed class implementation |%s| cannot be an interface. See class descriptor |%s|.", implementationClass, descriptor);
		}
		int implementationModifiers = implementationClass.getModifiers();
		if (Modifier.isAbstract(implementationModifiers)) {
			throw new ConfigException("Managed class implementation |%s| cannot be abstract. See class descriptor |%s|.", implementationClass, descriptor);
		}
		if (Types.isKindOf(implementationClass, ManagedLifeCycle.class) && !InstanceScope.APPLICATION.equals(instanceScope)) {
			throw new ConfigException("Bad scope |%s| used with managed life cycle. See class descriptor |%s|.", instanceScope, descriptor);
		}

		return implementationClass;
	}

	/**
	 * Load interface class from class descriptor. Attempt to load interface class from <code>interface</code> attribute. If not
	 * found returns implementation class that should be already initialized.
	 * <p>
	 * Perform sanity checks on loaded interface class and throws configuration exception if:
	 * <ul>
	 * <li>instance type requires interface but none found,
	 * <li><code>name</code> attribute is missing from child element,
	 * <li>interface class not found on run-time class path,
	 * <li>implementation class is not implemented by already configured implementation class.
	 * </ul>
	 * <p>
	 * This utility method should be loaded after {@link #loadImplementationClass(Config)} otherwise behavior is not defined.
	 * 
	 * @param descriptor class descriptor.
	 * @return interface classes array.
	 * @throws ConfigException if sanity check fails.
	 */
	@SuppressWarnings("unchecked")
	private Class<T> loadInterfaceClass(Config descriptor) throws ConfigException {
		if (!descriptor.hasAttribute("interface")) {
			if (instanceType.requiresInterface()) {
				throw new ConfigException("Managed type |%s| requires <interface> attribute. See class descriptor |%s|.", instanceType, descriptor);
			}
			// if interface is not required and is missing uses implementation class
			return (Class<T>) implementationClass;
		}

		if ("REMOTE".equals(descriptor.getAttribute("type"))) {
			String url = descriptor.getAttribute("url");
			if (url == null || url.isEmpty()) {
				throw new ConfigException("Managed type REMOTE requires <url> attribute. See class descriptor |%s|.", descriptor);
			}
			if (url.startsWith("${")) {
				throw new ConfigException("Remote implementation <url> property not resolved. See class descriptor |%s|.", descriptor);
			}
		}

		String interfaceName = descriptor.getAttribute("interface");
		final Class<T> interfaceClass = Classes.forOptionalName(interfaceName);

		if (interfaceClass == null) {
			throw new ConfigException("Managed class interface |%s| not found.", interfaceName);
		}
		if (instanceType.requiresInterface() && !interfaceClass.isInterface()) {
			throw new ConfigException("Managed type |%s| requires interface to make Java Proxy happy but got |%s|.", instanceType, interfaceClass);
		}
		if (implementationClass != null && !Types.isKindOf(implementationClass, interfaceClass)) {
			throw new ConfigException("Implementation |%s| is not a kind of interface |%s|.", implementationClass, interfaceClass);
		}

		return interfaceClass;
	}

	/**
	 * Return instance scope loaded from class descriptor, <code>scope</code> attribute. If scope is not defined use
	 * {@link InstanceScope#APPLICATION} as default value.
	 * 
	 * @param descriptor managed class descriptor.
	 * @return loaded instance scope.
	 * @throws ConfigException if there is no {@link ScopeFactory} registered for loaded instance scope.
	 */
	private InstanceScope loadInstanceScope(Config descriptor) throws ConfigException {
		InstanceScope instanceScope = descriptor.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION);
		if (!container.hasScopeFactory(instanceScope)) {
			throw new ConfigException("Not registered managed instance scope value |%s|. See class descriptor |%s|.", instanceScope, descriptor);
		}
		return instanceScope;
	}

	/**
	 * Return instance type loaded from class descriptor, <code>type</code> attribute. If type is not defined uses
	 * {@link InstanceType#POJO} as default value.
	 * 
	 * @param descriptor managed class descriptor.
	 * @return loaded instance type.
	 * @throws ConfigException if there is no {@link InstanceFactory} registered for loaded instance type.
	 */
	private InstanceType loadInstanceType(Config descriptor) throws ConfigException {
		InstanceType instanceType = descriptor.getAttribute("type", InstanceType.class, InstanceType.POJO);
		if (!container.hasInstanceFactory(instanceType)) {
			throw new ConfigException("Not registered managed instance type value |%s|. See class descriptor |%s|.", instanceType, descriptor);
		}
		return instanceType;
	}

	/**
	 * Load remote class URL from class descriptor, <code>url</code> attribute. This getter does not perform URL validation; it
	 * returns URL value as declared by attribute.
	 * 
	 * @param descriptor class descriptor.
	 * @return remote class URL value.
	 * @throws ConfigException if instance type is {@link InstanceType#REMOTE} and <code>url</code> attribute is missing.
	 */
	private String loadImplementationURL(Config descriptor) throws ConfigException {
		String implementationURL = descriptor.getAttribute("url");
		if (instanceType.equals(InstanceType.REMOTE) && implementationURL == null) {
			throw new ConfigException("Remote managed class requires <url> attribute. See class descriptor |%s|.", descriptor);
		}
		return implementationURL;
	}

	/**
	 * Get implementation class constructor. Managed class mandates a single constructor with parameters, no matter if private
	 * or formal parameters count. If both default constructor and constructor with parameters are defined this method returns
	 * constructor with parameters. Constructors annotated with {@link TestConstructor} are ignored. It is not allowed to have
	 * more than a single constructor with parameters, of course less those marked for test. Returns null if implementation
	 * class is missing.
	 * 
	 * @param implementationClass implementation class, possible null.
	 * @return implementation class constructor or null if given implementation class is null.
	 * @throws BugError if implementation class is an interface, primitive or array and has no constructor at all.
	 * @throws BugError if implementation class has more constructors with parameters.
	 */
	private static <T> Constructor<? extends T> getDeclaredConstructor(Class<T> implementationClass) {
		if (implementationClass == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Constructor<? extends T>[] declaredConstructors = (Constructor<? extends T>[]) implementationClass.getDeclaredConstructors();
		if (declaredConstructors.length == 0) {
			throw new BugError("Invalid implementation class |%s|. Missing constructor.", implementationClass);
		}
		Constructor<? extends T> defaultConstructor = null;
		Constructor<? extends T> constructor = null;

		for (Constructor<? extends T> declaredConstructor : declaredConstructors) {
			// synthetic constructors are created by compiler to circumvent JVM limitations, JVM that is not evolving with
			// the same speed as the language; for example, to allow outer class to access private members on a nested class
			// compiler creates a constructor with a single argument of very nested class type
			if (declaredConstructor.isSynthetic()) {
				continue;
			}

			if (declaredConstructor.getAnnotation(Inject.class) != null) {
				constructor = declaredConstructor;
				break;
			}

			if (declaredConstructor.getParameterTypes().length == 0) {
				defaultConstructor = declaredConstructor;
				continue;
			}
			if (constructor != null) {
				throw new BugError("Implementation class |%s| has not a single constructor with parameters. Use @Inject to declare which constructor to use.", implementationClass);
			}
			constructor = declaredConstructor;
		}

		if (constructor == null) {
			if (defaultConstructor == null) {
				throw new BugError("Invalid implementation class |%s|. Missing default constructor.", implementationClass);
			}
			constructor = defaultConstructor;
		}
		constructor.setAccessible(true);
		return constructor;
	}

	/**
	 * Initialize implementation class static, not final, fields. Static initializer reads name / value pairs from
	 * <code>static-field</code> configuration element, see sample below. String value is converted to instance field type using
	 * {@link Converter#asObject(String, Class)} utility. This means that configured value should be convertible to field type,
	 * otherwise {@link ConverterException} is thrown.
	 * <p>
	 * In sample there is a <code>person</code> managed instance that has a configuration section. Configuration declares three
	 * static fields that will be initialized with defined values when managed class is created.
	 * 
	 * <pre>
	 * &lt;managed-classes&gt;
	 * 	&lt;person class="js.app.Person" /&gt;
	 * &lt;/managed-classes&gt;
	 * ...
	 * &lt;person&gt;
	 * 	&lt;static-field name='name' value='John Doe' /&gt;
	 * 	&lt;static-field name='age' value='54' /&gt;
	 * 	&lt;static-field name='married' value='false' /&gt;
	 * &lt;/person&gt;
	 * ...
	 * class Person {
	 * 	private static String name;
	 * 	private static int age;
	 * 	private static boolean married;
	 * }
	 * </pre>
	 * 
	 * @throws ConfigException if static field descriptor is invalid, field is missing or is instance field.
	 */
	private void initializeStaticFields() throws ConfigException {
		if (config == null) {
			return;
		}
		for (Config config : config.findChildren("static-field")) {
			String fieldName = config.getAttribute("name");
			if (fieldName == null) {
				throw new ConfigException("Missing <name> attribute from static field initialization |%s|.", config.getParent());
			}
			if (!config.hasAttribute("value")) {
				throw new ConfigException("Missing <value> attribute from static field initialization |%s|.", config.getParent());
			}

			Field field = Classes.getOptionalField(implementationClass, fieldName);
			if (field == null) {
				throw new ConfigException("Missing managed class static field |%s#%s|.", implementationClass, fieldName);
			}
			int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers)) {
				throw new ConfigException("Attempt to execute static initialization on instance field |%s#%s|.", implementationClass, fieldName);
			}

			Object value = config.getAttribute("value", field.getType());
			log.debug("Intialize static field |%s#%s| |%s|", implementationClass, fieldName, value);
			Classes.setFieldValue(null, field, config.getAttribute("value", field.getType()));
		}
	}

	/**
	 * Build and return this managed class string representation.
	 * 
	 * @param descriptor managed class descriptor.
	 * @return this managed class string representation.
	 */
	private String buildStringRepresentation(Config descriptor) {
		StringBuilder builder = new StringBuilder();
		builder.append(descriptor.getName());
		builder.append(':');
		if (implementationClass != null) {
			builder.append(implementationClass.getName());
			builder.append(':');
		}
		if (interfaceClass != null) {
			builder.append(interfaceClass.getName());
			builder.append(':');
		}
		builder.append(instanceType);
		builder.append(':');
		builder.append(instanceScope);
		if (implementationURL != null) {
			builder.append(':');
			builder.append(implementationURL);
		}
		return builder.toString();
	}

	// --------------------------------------------------------------------------------------------
	// ANNOTATIONS SCANNER UTILITY METHODS

	@SuppressWarnings("unchecked")
	@Override
	public <S extends IServiceMeta> S getServiceMeta(Class<S> type) {
		return (S) serviceMetas.get(type);
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		A annotation = implementationClass.getAnnotation(type);
		if (annotation == null) {
			for (Class<?> interfaceClass : implementationClass.getInterfaces()) {
				annotation = interfaceClass.getAnnotation(type);
				if (annotation != null) {
					break;
				}
			}
		}
		return annotation;
	}

	/**
	 * Get Java reflective method from interface. This getter attempt to locate a method with the same signature as requested
	 * base class method in any interface the declaring class may have. If no method found in interfaces or no interface present
	 * return given base class method. If requested base class method is declared in multiple interfaces this getter returns the
	 * first found but there is no guarantee for order.
	 * 
	 * @param method base class method, not null.
	 * @return interface method or given base class method if no interface method found.
	 */
	private static Method getInterfaceMethod(Method method) {
		for (Class<?> interfaceClass : method.getDeclaringClass().getInterfaces()) {
			try {
				return interfaceClass.getMethod(method.getName(), method.getParameterTypes());
			} catch (NoSuchMethodException unused) {
			}
		}
		return method;
	}
}
