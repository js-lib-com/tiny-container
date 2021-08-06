package js.tiny.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import js.annotation.ContextParam;
import js.converter.Converter;
import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.annotation.Asynchronous;
import js.tiny.container.annotation.Controller;
import js.tiny.container.annotation.Cron;
import js.tiny.container.annotation.Inject;
import js.tiny.container.annotation.Intercepted;
import js.tiny.container.annotation.Local;
import js.tiny.container.annotation.Private;
import js.tiny.container.annotation.Produces;
import js.tiny.container.annotation.Public;
import js.tiny.container.annotation.Remote;
import js.tiny.container.annotation.RequestPath;
import js.tiny.container.annotation.RolesAllowed;
import js.tiny.container.annotation.Service;
import js.tiny.container.annotation.TestConstructor;
import js.tiny.container.core.AppFactory;
import js.tiny.container.servlet.ContextParamProcessor;
import js.transaction.Immutable;
import js.transaction.Mutable;
import js.transaction.Transactional;
import js.util.Classes;
import js.util.Types;

/**
 * Managed class resemble Java {@link Class} and is configurable from external class descriptor and by annotations. Class
 * descriptor is a configuration element from <code>managed-classes</code> section from application descriptor. Managed class
 * parses and validates class descriptor and annotations and initialize its state accordingly; managed class is immutable. Also
 * takes care to parse managed class configuration, if exist into application descriptor, see below <code>Descriptor</code>
 * section. Anyway, managed class has no means to create instances, like Java class has. This is because instance creation
 * algorithm is implemented into container and exposes by {@link AppFactory} interface.
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
 * {@link AppFactory#getInstance(Class, Object...)} refers to this <code>interface</code>. In most cases it is indeed a Java
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
 * Even if <code>url</code> attribute is provided, when use {@link AppFactory#getInstance(String, Class, Object...)} to retrieve
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
 * <td>{@link Local}
 * <td>N/A
 * <td>Forbid remote access to particular methods inside a {@link Remote} accessible managed instance.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Controller}
 * <td>Class request URI path
 * <td>A controller is a remote accessible managed class that has methods returning resources.
 * <td>true
 * <td>false
 * <td>false
 * <tr>
 * <td>{@link Service}
 * <td>Class request URI path
 * <td>A service is a managed class that has methods remotely accessible.
 * <td>true
 * <td>false
 * <td>false
 * <tr>
 * <td>{@link RequestPath}
 * <td>Method request URI path
 * <td>Managed method binding to particular resource path.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Private}
 * <td>N/A
 * <td>Private remote accessible managed methods, that cannot be invoked without authorization.
 * <td>false
 * <td>true
 * <td>false
 * <tr>
 * <td>{@link Public}
 * <td>N/A
 * <td>Remote accessible entity that do not require authorization.
 * <td>true
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
 * <td>{@link Intercepted}
 * <td>interceptor class
 * <td>An intercepted managed method executes an interceptor cross-cutting logic whenever is invoked.
 * <td>true
 * <td>true
 * <td>false
 * </table>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class ManagedClass implements ManagedClassSPI {
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
	 * Managed class interfaces, usually one but multiple supported. If class descriptor has only <code>class</code> attribute
	 * this field is initialized from {@link #implementationClass}.
	 */
	private final Class<?>[] interfaceClasses;

	/**
	 * Optional managed class implementation has value only if {@link InstanceType#requiresImplementation()} says so. It can be
	 * null if managed class does not require implementation, for example if is a remote class.
	 */
	private final Class<?> implementationClass;

	/** Cached implementation constructor or null if this managed class has no implementation. */
	private final Constructor<?> constructor;

	/** Fields tagged with {@link Inject} annotation, in no particular order. */
	private final Collection<Field> dependencies;

	/**
	 * Optional remote class implementation URL that can be used if this managed class is {@link InstanceType#REMOTE}. This
	 * field is loaded from <code>url</code> attribute from class descriptor.
	 */
	private final String implementationURL;

	/**
	 * Managed methods pool for managed classes of type {@link InstanceType#PROXY}. Used by {@link ManagedProxyHandler} to find
	 * out managed method bound to interface method.
	 */
	private final Map<Method, ManagedMethodSPI> methodsPool = new HashMap<>();

	/** Pool of net methods, that is, methods remotely accessible. */
	private final Map<String, ManagedMethodSPI> netMethodsPool = new HashMap<>();

	/**
	 * Map of fields annotated with {@link ContextParam} annotation. Map key is the context parameter name. This fields will be
	 * initialized from container runtime context by {@link ContextParamProcessor}. Note that both static and instance fields
	 * are acceptable.
	 */
	private final Map<String, Field> contextParamFields = new HashMap<>();

	/** Collection of managed methods annotated with {@link Cron} annotation. */
	private final List<ManagedMethodSPI> cronMethodsPool = new ArrayList<>();

	/** Cached value of managed class string representation, merely for logging. */
	private final String string;

	/**
	 * A managed class is remotely accessible, also known as net class, if is annotated with {@link Remote} or has at least one
	 * remotely accessible method.
	 */
	private boolean remotelyAccessible;

	/** A managed class is transactional if is annotated with {@link Transactional} or has at least one transactional method. */
	private boolean transactional;

	/**
	 * Transactional resource may support multiple schemas. This allows to limit the scope of resource objects accessible within
	 * transaction boundaries. This field store the name of the schema as set by {@link js.transaction.Transactional#schema()}
	 * annotation. Note that annotation should be applied to class; on managed methods schema value is ignored.
	 * <p>
	 * Transactional schema is optional with default to null.
	 */
	private String transactionalSchema;

	/**
	 * Request URI path for this managed class configured by {@link Remote}, {@link Controller} or {@link Service} annotations.
	 */
	private String requestPath;

	/**
	 * Flag indicating that this managed class should be instantiated automatically by container, see {@link Container#start()}.
	 * Note that created instance is a singleton and managed instance scope should be {@link InstanceScope#APPLICATION}.
	 * <p>
	 * This flag is true for following conditions:
	 * <ul>
	 * <li>this managed class has {@link Cron} methods,
	 * <li>this managed class implements {@link ManagedLifeCycle} interface.
	 * </ul>
	 */
	private boolean autoInstanceCreation;

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
		this.interfaceClasses = loadInterfaceClasses(descriptor);
		this.implementationURL = loadImplementationURL(descriptor);

		// get declared constructor return null if no implementation class
		this.constructor = getDeclaredConstructor(this.implementationClass);
		// scan dependencies return empty collection if no implementation class
		this.dependencies = scanDependencies(this.implementationClass);

		if (this.instanceType.requiresImplementation()) {
			scanAnnotations();
			initializeStaticFields();
		}

		this.key = KEY_SEED.getAndIncrement();
		this.string = buildStringRepresentation(descriptor);
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

	/**
	 * Scan annotations for managed classes that requires implementation. Annotations are processed only for managed classes
	 * that have {@link #implementationClass} that is primary source scanned for annotation. If an annotation is not present
	 * into implementation class try to find it on interface(s). See <a href="#annotations">Annotations</a> section from class
	 * description.
	 * 
	 * @throws BugError for insane conditions.
	 */
	private void scanAnnotations() {
		// set remote type and request URI path from @Remote, @Controller or @Service
		boolean remoteType = false;
		Controller controllerAnnotation = getAnnotation(implementationClass, Controller.class);
		if (controllerAnnotation != null) {
			remoteType = true;
			requestPath = controllerAnnotation.value();
		}
		Service serviceAnnotation = getAnnotation(implementationClass, Service.class);
		if (serviceAnnotation != null) {
			remoteType = true;
			requestPath = serviceAnnotation.value();
		}

		Remote remoteAnnotation = getAnnotation(implementationClass, Remote.class);
		if (remoteAnnotation != null) {
			remoteType = true;
		}
		RequestPath requestPathAnnotation = getAnnotation(implementationClass, RequestPath.class);
		if (requestPathAnnotation != null) {
			requestPath = requestPathAnnotation.value();
		}

		if (requestPath != null && requestPath.isEmpty()) {
			requestPath = null;
		}
		if (remoteType) {
			remotelyAccessible = true;
		}

		// set transactional annotation, optional schema and immutable type
		Transactional transactionalType = getAnnotation(implementationClass, Transactional.class);
		if (transactionalType != null) {
			transactionalSchema = transactionalType.schema();
			// transactional annotation schema() returns empty string if schema not set; it should be null
			if (transactionalSchema.isEmpty()) {
				transactionalSchema = null;
			}
		}
		boolean immutableType = hasAnnotation(implementationClass, Immutable.class);
		if (transactionalType == null && immutableType) {
			throw new BugError("@Immutable annotation without @Transactional on class |%s|.", implementationClass.getName());
		}
		if (transactionalType != null && !instanceType.isPROXY()) {
			throw new BugError("@Transactional requires |%s| type but found |%s| on |%s|.", InstanceType.PROXY, instanceType, implementationClass);
		}

		Class<? extends Interceptor> classInterceptor = getInterceptorClass(implementationClass);
		boolean publicType = hasAnnotation(implementationClass, Public.class);

		String[] typeRoles = null;
		RolesAllowed rolesAllowed = getAnnotation(implementationClass, RolesAllowed.class);
		if (rolesAllowed != null) {
			typeRoles = rolesAllowed.value();
		}

		// managed classes does not support public inheritance
		for (Method method : implementationClass.getDeclaredMethods()) {
			final int modifiers = method.getModifiers();
			if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
				// scans only public and non-static methods
				continue;
			}
			Method interfaceMethod = getInterfaceMethod(method);
			ManagedMethod managedMethod = null;

			boolean remoteMethod = hasAnnotation(method, Remote.class);
			if (!remoteMethod) {
				remoteMethod = remoteType;
			}
			if (hasAnnotation(method, Local.class)) {
				if (!remoteMethod) {
					throw new BugError("@Local annotation on not remote method |%s|.", method);
				}
				remoteMethod = false;
			}
			if (remoteMethod) {
				// if at least one owned managed method is remote this managed class become remote too
				remotelyAccessible = true;
			}

			// load method interceptor annotation and if missing uses class annotation; is legal for both to be null
			Class<? extends Interceptor> methodInterceptor = getInterceptorClass(method);
			if (methodInterceptor == null) {
				methodInterceptor = classInterceptor;
			}
			// if method is intercepted, either by method or class annotation, create intercepted managed method
			if (methodInterceptor != null) {
				if (!instanceType.isPROXY() && !remotelyAccessible) {
					throw new BugError("@Intercepted method |%s| supported only on PROXY type or remote accessible classes.", method);
				}
				managedMethod = new ManagedMethod(this, methodInterceptor, interfaceMethod);
			}

			// handle remote accessible methods

			boolean publicMethod = hasAnnotation(method, Public.class);
			if (publicMethod && !remotelyAccessible) {
				throw new BugError("@Public annotation on not remote method |%s|.", method);
			}
			if (!publicMethod) {
				publicMethod = publicType;
			}
			if (hasAnnotation(method, Private.class)) {
				if (!remotelyAccessible) {
					throw new BugError("@Private annotation on not remote method |%s|.", method);
				}
				publicMethod = false;
			}
			RequestPath methodPath = getAnnotation(method, RequestPath.class);
			if (!remotelyAccessible && methodPath != null) {
				throw new BugError("@MethodPath annotation on not remote method |%s|.", method);
			}
			if (remoteMethod) {
				if (managedMethod == null) {
					managedMethod = new ManagedMethod(this, interfaceMethod);
				}
				managedMethod.setRequestPath(methodPath != null ? methodPath.value() : null);
				managedMethod.setRemotelyAccessible(remoteMethod);
				managedMethod.setAccess(publicMethod ? Access.PUBLIC : Access.PRIVATE);
			}

			// handle declarative transaction
			// 1. allow transactional only on PROXY
			// 2. do not allow immutable on method if not transactional
			// 3. do not allow mutable on method if not transactional
			// 4. if PROXY create managed method if not already created from above remote method logic

			if (transactionalType == null) {
				transactionalType = getAnnotation(method, Transactional.class);
			}
			if (transactionalType != null) {
				transactional = true;
				if (!instanceType.isPROXY()) {
					throw new BugError("@Transactional requires |%s| type but found |%s|.", InstanceType.PROXY, instanceType);
				}
			}

			boolean immutable = hasAnnotation(method, Immutable.class);
			if (immutable && !transactional) {
				log.debug("@Immutable annotation without @Transactional on method |%s|.", method);
			}
			if (!immutable) {
				immutable = immutableType;
			}
			if (hasAnnotation(method, Mutable.class)) {
				if (!transactional) {
					log.debug("@Mutable annotation without @Transactional on method |%s|.", method);
				}
				immutable = false;
			}

			if (instanceType.isPROXY() && managedMethod == null) {
				managedMethod = new ManagedMethod(this, interfaceMethod);
			}
			if (transactional) {
				managedMethod.setTransactional(true);
				managedMethod.setImmutable(immutable);
			}

			// handle asynchronous mode
			// 1. instance type should be PROXY or managed class should be flagged for remote access
			// 2. transactional method cannot be executed asynchronously
			// 3. asynchronous method should be void

			boolean asynchronousMethod = hasAnnotation(method, Asynchronous.class);
			if (asynchronousMethod) {
				if (!instanceType.isPROXY() && !remotelyAccessible) {
					throw new BugError("Not supported instance type |%s| for asynchronous method |%s|.", instanceType, method);
				}
				if (transactional) {
					throw new BugError("Transactional method |%s| cannot be executed asynchronous.", method);
				}
				if (!Types.isVoid(method.getReturnType())) {
					throw new BugError("Asynchronous method |%s| must be void.", method);
				}

				// at this point either instance type is PROXY or remote flag is true; checked by above bug error
				// both conditions has been already processed with managed method creation
				// so managed method must be already created
				managedMethod.setAsynchronous(asynchronousMethod);
			}

			Cron cronMethod = getAnnotation(method, Cron.class);
			if (cronMethod != null) {
				if (remotelyAccessible) {
					throw new BugError("Remote accessible method |%s| cannot be executed by cron.", method);
				}
				if (transactional) {
					throw new BugError("Transactional method |%s| cannot be executed by cron.", method);
				}
				if (!Types.isVoid(method.getReturnType())) {
					throw new BugError("Cron method |%s| must be void.", method);
				}

				if (managedMethod == null) {
					managedMethod = new ManagedMethod(this, interfaceMethod);
				}
				managedMethod.setCronExpression(cronMethod.value());
				cronMethodsPool.add(managedMethod);
				autoInstanceCreation = true;
			}

			if (managedMethod == null) {
				continue;
			}

			rolesAllowed = getAnnotation(method, RolesAllowed.class);
			String[] methodRoles = rolesAllowed != null ? rolesAllowed.value() : typeRoles;
			if (methodRoles != null) {
				managedMethod.setRoles(methodRoles);
			}

			Produces producesMethod = getAnnotation(method, Produces.class);
			if (producesMethod != null) {
				managedMethod.setReturnContentType(producesMethod.value());
			}

			// store managed method, if created, to managed methods pool
			methodsPool.put(interfaceMethod, managedMethod);
			if (managedMethod.isRemotelyAccessible() && netMethodsPool.put(method.getName(), managedMethod) != null) {
				throw new BugError("Overloading is not supported for net method |%s|.", managedMethod);
			}
		}

		for (Field field : implementationClass.getDeclaredFields()) {
			ContextParam contextParam = field.getAnnotation(ContextParam.class);
			if (contextParam != null) {
				field.setAccessible(true);
				contextParamFields.put(contextParam.value(), field);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED CLASS SPI

	@Override
	public ContainerSPI getContainer() {
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
	public Class<?>[] getInterfaceClasses() {
		return interfaceClasses;
	}

	@Override
	public Class<?> getInterfaceClass() {
		if (interfaceClasses.length > 1) {
			throw new BugError("Attempt to treat multiple interfaces as a single one.");
		}
		return interfaceClasses[0];
	}

	@Override
	public Class<?> getImplementationClass() {
		return implementationClass;
	}

	@Override
	public Constructor<?> getConstructor() {
		return constructor;
	}

	@Override
	public Iterable<Field> getDependencies() {
		return dependencies;
	}

	@Override
	public Iterable<ManagedMethodSPI> getManagedMethods() {
		return methodsPool.values();
	}

	@Override
	public Iterable<ManagedMethodSPI> getNetMethods() {
		return netMethodsPool.values();
	}

	@Override
	public Iterable<ManagedMethodSPI> getCronMethods() {
		return cronMethodsPool;
	}

	@Override
	public ManagedMethodSPI getManagedMethod(Method method) throws NoSuchMethodException {
		if (!instanceType.equals(InstanceType.PROXY)) {
			throw new BugError("Managed method getter can be used only on |%s| types.", InstanceType.PROXY);
		}
		ManagedMethodSPI managedMethod = methodsPool.get(method);
		if (managedMethod == null) {
			throw new NoSuchMethodException(String.format("Missing managed method |%s#%s|.", implementationClass.getName(), method.getName()));
		}
		return managedMethod;
	}

	@Override
	public ManagedMethodSPI getNetMethod(String methodName) {
		ManagedMethodSPI managedMethod = netMethodsPool.get(methodName);
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
	public boolean isTransactional() {
		return transactional;
	}

	@Override
	public String getTransactionalSchema() {
		return transactionalSchema;
	}

	@Override
	public boolean isRemotelyAccessible() {
		return remotelyAccessible;
	}

	@Override
	public String getRequestPath() {
		if (!remotelyAccessible) {
			throw new BugError("Attempt to retrive request URI path for local managed class |%s|.", this);
		}
		return requestPath;
	}

	@Override
	public String getImplementationURL() {
		return implementationURL;
	}

	@Override
	public Map<String, Field> getContextParamFields() {
		return contextParamFields;
	}

	// --------------------------------------------------------------------------------------------
	// CLASS DESCRIPTOR UTILITY METHODS

	@Override
	public boolean isAutoInstanceCreation() {
		return autoInstanceCreation;
	}

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
	private Class<?> loadImplementationClass(Config descriptor) throws ConfigException {
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

		Class<?> implementationClass = Classes.forOptionalName(implementationName);
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
	 * Load interface classes from class descriptor. Attempt to load interface classes from <code>interface</code> attribute or
	 * child elements. If none found returns implementation class that should be already initialized.
	 * <p>
	 * Perform sanity checks on loaded interface classes and throws configuration exception is:
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
	private Class<?>[] loadInterfaceClasses(Config descriptor) throws ConfigException {
		List<String> interfaceNames = new ArrayList<>();

		if (!descriptor.hasChildren()) {
			if (!descriptor.hasAttribute("interface")) {
				if (instanceType.requiresInterface()) {
					throw new ConfigException("Managed type |%s| requires <interface> attribute. See class descriptor |%s|.", instanceType, descriptor);
				}
				// if interface is not required and is missing uses implementation class
				return new Class<?>[] { implementationClass };
			}
			interfaceNames.add(descriptor.getAttribute("interface"));

			if ("REMOTE".equals(descriptor.getAttribute("type"))) {
				String url = descriptor.getAttribute("url");
				if (url == null || url.isEmpty()) {
					throw new ConfigException("Managed type REMOTE requires <url> attribute. See class descriptor |%s|.", descriptor);
				}
				if (url.startsWith("${")) {
					throw new ConfigException("Remote implementation <url> property not resolved. See class descriptor |%s|.", descriptor);
				}
			}
		} else {
			for (int i = 0; i < descriptor.getChildrenCount(); ++i) {
				String interfaceName = descriptor.getChild(i).getAttribute("name");
				if (interfaceName == null) {
					throw new ConfigException("Missing <name> attribute from interface declaration. See class descriptor |%s|.", descriptor);
				}
				interfaceNames.add(interfaceName);
			}
		}

		Class<?>[] interfaceClasses = new Class<?>[interfaceNames.size()];
		for (int i = 0; i < interfaceNames.size(); ++i) {
			final String interfaceName = interfaceNames.get(i);
			final Class<?> interfaceClass = Classes.forOptionalName(interfaceName);

			if (interfaceClass == null) {
				throw new ConfigException("Managed class interface |%s| not found.", interfaceName);
			}
			if (Types.isKindOf(interfaceClass, ManagedLifeCycle.class)) {
				autoInstanceCreation = true;
			}
			if (instanceType.requiresInterface() && !interfaceClass.isInterface()) {
				throw new ConfigException("Managed type |%s| requires interface to make Java Proxy happy but got |%s|.", instanceType, interfaceClass);
			}
			if (implementationClass != null && !Types.isKindOf(implementationClass, interfaceClass)) {
				throw new ConfigException("Implementation |%s| is not a kind of interface |%s|.", implementationClass, interfaceClass);
			}

			interfaceClasses[i] = interfaceClass;
		}
		return interfaceClasses;
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
	private static Constructor<?> getDeclaredConstructor(Class<?> implementationClass) {
		if (implementationClass == null) {
			return null;
		}
		Constructor<?>[] declaredConstructors = implementationClass.getDeclaredConstructors();
		if (declaredConstructors.length == 0) {
			throw new BugError("Invalid implementation class |%s|. Missing constructor.", implementationClass);
		}
		Constructor<?> defaultConstructor = null;
		Constructor<?> constructor = null;

		for (Constructor<?> declaredConstructor : declaredConstructors) {
			// synthetic constructors are created by compiler to circumvent JVM limitations, JVM that is not evolving with
			// the same speed as the language; for example, to allow outer class to access private members on a nested class
			// compiler creates a constructor with a single argument of very nested class type
			if (declaredConstructor.isSynthetic()) {
				continue;
			}
			if (declaredConstructor.getAnnotation(TestConstructor.class) != null) {
				continue;
			}
			if (declaredConstructor.getParameterTypes().length == 0) {
				defaultConstructor = declaredConstructor;
				continue;
			}
			if (constructor != null) {
				throw new BugError("Implementation class |%s| has not a single constructor with parameters.", implementationClass);
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
	 * Scan class dependencies declared by {@link Inject} annotation. This method scans all fields, no matter private, protected
	 * or public. Anyway it is considered a bug if inject annotation is found on final or static field.
	 * <p>
	 * Returns a collection of reflective fields with accessibility set but in not particular order. If given class argument is
	 * null returns empty collection.
	 * 
	 * @param clazz class to scan dependencies for, null tolerated.
	 * @return dependencies collection, in no particular order.
	 * @throws BugError if inject annotation is used on final or static field.
	 */
	private static Collection<Field> scanDependencies(Class<?> clazz) {
		if (clazz == null) {
			return Collections.emptyList();
		}
		Collection<Field> dependencies = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Inject.class)) {
				continue;
			}
			if (Modifier.isFinal(field.getModifiers())) {
				throw new BugError("Attempt to inject final field |%s|.", field.getName());
			}
			if (Modifier.isStatic(field.getModifiers())) {
				throw new BugError("Attempt to inject static field |%s|.", field.getName());
			}
			field.setAccessible(true);
			dependencies.add(field);
		}
		return dependencies;
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
		for (Class<?> interfaceClass : interfaceClasses) {
			builder.append(interfaceClass.getName());
			builder.append(':');
		}
		builder.append(instanceType);
		builder.append(':');
		builder.append(instanceScope);
		builder.append(':');
		builder.append(remotelyAccessible ? "NET" : "LOCAL");
		if (implementationURL != null) {
			builder.append(':');
			builder.append(implementationURL);
		}
		return builder.toString();
	}

	// --------------------------------------------------------------------------------------------
	// ANNOTATIONS SCANNER UTILITY METHODS

	/**
	 * Get class annotation or null if none found. This getter uses extended annotation searching scope: it searches first on
	 * given class then tries with all class interfaces. Note that only interfaces are used as alternative for annotation
	 * search. Super class is not included.
	 * <p>
	 * Returns null if no annotation found on base class or interfaces.
	 * 
	 * @param clazz base class to search for annotation,
	 * @param annotationClass annotation to search for.
	 * @param <T> annotation type.
	 * @return annotation instance or null if none found.
	 */
	private static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationClass) {
		T annotation = clazz.getAnnotation(annotationClass);
		if (annotation == null) {
			for (Class<?> interfaceClass : clazz.getInterfaces()) {
				annotation = interfaceClass.getAnnotation(annotationClass);
				if (annotation != null) {
					break;
				}
			}
		}
		return annotation;
	}

	/**
	 * Test if class has requested annotation. This predicate uses extended annotation searching scope: it searches first on
	 * given class then tries with all class interfaces. Note that only interfaces are used as alternative for annotation
	 * search. Super class is not included.
	 * <p>
	 * Returns false if no annotation found on base class or interfaces.
	 * 
	 * @param clazz base class to search for annotation,
	 * @param annotationClass annotation to search for.
	 * @return true if annotation found.
	 */
	private static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
		Annotation annotation = clazz.getAnnotation(annotationClass);
		if (annotation != null) {
			return true;
		}
		for (Class<?> interfaceClass : clazz.getInterfaces()) {
			annotation = interfaceClass.getAnnotation(annotationClass);
			if (annotation != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get method annotation or null if none found. This getter uses extended annotation searching scope: it searches first on
	 * given method declaring class then tries with all class interfaces. Note that only interfaces are used as alternative for
	 * method annotation search. Super class is not included.
	 * <p>
	 * Returns null if no method annotation found on base class or interfaces.
	 * 
	 * @param method method to search for annotation,
	 * @param annotationClass annotation to search for.
	 * @param <T> annotation type.
	 * @return annotation instance or null if none found.
	 */
	private static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
		T annotation = method.getAnnotation(annotationClass);
		if (annotation == null) {
			for (Class<?> interfaceClass : method.getDeclaringClass().getInterfaces()) {
				try {
					annotation = interfaceClass.getMethod(method.getName(), method.getParameterTypes()).getAnnotation(annotationClass);
					if (annotation != null) {
						return annotation;
					}
				} catch (NoSuchMethodException unused) {
				}
			}
		}
		return annotation;
	}

	/**
	 * Test if method has requested annotation. This predicate uses extended annotation searching scope: it searches first on
	 * given method declaring class then tries with all class interfaces. Note that only interfaces are used as alternative for
	 * annotation search. Super class is not included.
	 * <p>
	 * Returns false if no method annotation found in declaring class or interfaces.
	 * 
	 * @param method method to search for annotation,
	 * @param annotationClass annotation to search for.
	 * @return true if annotation found.
	 */
	private static boolean hasAnnotation(Method method, Class<? extends Annotation> annotationClass) {
		Annotation annotation = method.getAnnotation(annotationClass);
		if (annotation != null) {
			return true;
		}
		for (Class<?> interfaceClass : method.getDeclaringClass().getInterfaces()) {
			try {
				annotation = interfaceClass.getMethod(method.getName(), method.getParameterTypes()).getAnnotation(annotationClass);
				if (annotation != null) {
					return true;
				}
			} catch (NoSuchMethodException unused) {
			}
		}
		return false;
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

	/**
	 * Get class value of intercepted annotation of a given class. Returns null if given class has no {@link Intercepted}
	 * annotation.
	 * 
	 * @param clazz annotated class.
	 * @return interceptor class or null if intercepted annotation is missing.
	 */
	private static Class<? extends Interceptor> getInterceptorClass(Class<?> clazz) {
		Intercepted intercepted = getAnnotation(clazz, Intercepted.class);
		return intercepted != null ? intercepted.value() : null;
	}

	/**
	 * Get class value of intercepted annotation of a given method. Returns null if given method has no {@link Intercepted}
	 * annotation.
	 * 
	 * @param method annotated method.
	 * @return interceptor class or null if intercepted annotation is missing.
	 */
	private static Class<? extends Interceptor> getInterceptorClass(Method method) {
		Intercepted intercepted = getAnnotation(method, Intercepted.class);
		return intercepted != null ? intercepted.value() : null;
	}
}
