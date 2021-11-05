package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
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
 * Managed class implementation.
 *  
 * @author Iulian Rotaru
 */
public final class ManagedClass<T> implements IManagedClass<T> {
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

	/** Managed instance scope used for life span management. */
	private final InstanceScope instanceScope;

	/** Managed instance type used to select CDI instance provider. */
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

	/**
	 * Optional remote class implementation URL that can be used if this managed class is {@link InstanceType#REMOTE}. This
	 * field is loaded from <code>url</code> attribute from class descriptor.
	 */
	private final String implementationURL;

	/**
	 * Managed methods pool for managed classes of type {@link InstanceType#PROXY}. Used to find out managed method bound to
	 * interface method.
	 */
	private final Map<String, IManagedMethod> methodsPool = new HashMap<>();

	/** Cached value of managed class string representation, merely for logging. */
	private final String string;

	private final Map<Class<? extends IServiceMeta>, IServiceMeta> serviceMetas = new HashMap<>();

	private final Set<IContainerService> services = new HashSet<>();

	/**
	 * Loads this managed class state from class descriptor then delegates {@link #scan()}. Annotations scanning is
	 * performed only if this managed class type requires implementation, see {@link InstanceType#requiresImplementation()}.
	 * 
	 * @param container parent container,
	 * @param descriptor class descriptor from <code>managed-class</code> section.
	 * @throws ConfigException if configuration is invalid.
	 */
	public ManagedClass(Container container, Config descriptor) throws ConfigException {
		this.container = container;

		// loading order matters; do not change it
		this.instanceScope = descriptor.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION);
		this.instanceType = descriptor.getAttribute("type", InstanceType.class, InstanceType.POJO, ConfigException.class);
		this.implementationClass = loadImplementationClass(descriptor);
		this.interfaceClass = loadInterfaceClass(descriptor);
		this.implementationURL = loadImplementationURL(descriptor);

		this.key = KEY_SEED.getAndIncrement();
		this.string = buildStringRepresentation(descriptor);

		if (this.instanceType.requiresImplementation()) {
			scan();
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
	private void scan() {
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
	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	@Override
	public Class<? extends T> getImplementationClass() {
		return implementationClass;
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
