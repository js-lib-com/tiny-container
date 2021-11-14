package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import js.lang.BugError;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IAnnotationsScanner;
import js.tiny.container.spi.IClassDescriptor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.InstanceType;

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
	 * Managed methods pool for managed classes of type {@link InstanceType#PROXY}. Used to find out managed method bound to
	 * interface method.
	 */
	private final Map<String, IManagedMethod> methodsPool = new HashMap<>();

	/** Cached value of managed class string representation, merely for logging. */
	private final String string;

	private final Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

	/**
	 * Loads this managed class state from class descriptor then delegates {@link #scan()}. Annotations scanning is performed
	 * only if this managed class type requires implementation, see {@link InstanceType#requiresImplementation()}.
	 * 
	 * @param container parent container,
	 * @param descriptor class descriptor from <code>managed-class</code> section.
	 * @throws ConfigException if configuration is invalid.
	 */
	public ManagedClass(Container container, IClassDescriptor<T> descriptor) throws ConfigException {
		this.container = container;

		this.interfaceClass = descriptor.getInterfaceClass();
		this.implementationClass = descriptor.getImplementationClass();
		this.key = KEY_SEED.getAndIncrement();
		this.string = buildStringRepresentation(descriptor);

		if (descriptor.getInstanceType().requiresImplementation()) {
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
			if (service instanceof IAnnotationsScanner) {
				IAnnotationsScanner scanner = (IAnnotationsScanner) service;
				for (Annotation serviceMeta : scanner.scanClassAnnotations(this)) {
					log.debug("Add service meta |%s| to managed class |%s|", serviceMeta.getClass(), this);
					annotations.put(serviceMeta.annotationType(), serviceMeta);
				}
			}
		}

		for (IManagedMethod method : methodsPool.values()) {
			ManagedMethod managedMethod = (ManagedMethod) method;
			for (IContainerService service : container.getServices()) {
				if (service instanceof IMethodInvocationProcessor) {
					managedMethod.addInvocationProcessor((IMethodInvocationProcessor) service);
				}
				if (service instanceof IAnnotationsScanner) {
					IAnnotationsScanner scanner = (IAnnotationsScanner) service;
					for (Annotation serviceMeta : scanner.scanMethodAnnotations(managedMethod)) {
						managedMethod.addAnnotation(serviceMeta);
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

	// --------------------------------------------------------------------------------------------
	// CLASS DESCRIPTOR UTILITY METHODS

	/**
	 * Build and return this managed class string representation.
	 * 
	 * @param descriptor managed class descriptor.
	 * @return this managed class string representation.
	 */
	private String buildStringRepresentation(IClassDescriptor<T> descriptor) {
		StringBuilder builder = new StringBuilder();
		if (descriptor.getInterfaceClass() != null) {
			builder.append(descriptor.getInterfaceClass().getName());
			builder.append(':');
		}
		if (descriptor.getImplementationClass() != null) {
			builder.append(descriptor.getImplementationClass().getName());
			builder.append(':');
		}
		builder.append(descriptor.getInstanceType());
		builder.append(':');
		builder.append(descriptor.getInstanceScope());
		if (descriptor.getImplementationURL() != null) {
			builder.append(':');
			builder.append(descriptor.getImplementationURL());
		}
		return builder.toString();
	}

	// --------------------------------------------------------------------------------------------
	// ANNOTATIONS SCANNER UTILITY METHODS

	@SuppressWarnings("unchecked")
	@Override
	public <S extends Annotation> S getAnnotation(Class<S> type) {
		return (S) annotations.get(type);
	}

	@Override
	public <A extends Annotation> A scanAnnotation(Class<A> type) {
		if (implementationClass == null) {
			return null;
		}
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
