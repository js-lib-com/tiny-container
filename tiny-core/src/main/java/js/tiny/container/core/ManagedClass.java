package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IClassDescriptor;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.InstanceType;

/**
 * Managed class implements extension points for class and instance services and facilitates remote access to business methods
 * via reflection.
 * 
 * @author Iulian Rotaru
 */
public final class ManagedClass<T> implements IManagedClass<T> {
	private static final Log log = LogFactory.getLog(ManagedClass.class);

	/** Back reference to parent container. */
	private final Container container;

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

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services. This list contains processors in execution order.
	 */
	private final FlowProcessorsSet<IInstancePostConstructProcessor> instancePostConstructors = new FlowProcessorsSet<>();

	private final FlowProcessorsSet<IInstancePreDestroyProcessor> instancePreDestructors = new FlowProcessorsSet<>();

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

	@Override
	public String getSignature() {
		return interfaceClass.getCanonicalName();
	}

	private void scan() {
		for (Method method : implementationClass.getDeclaredMethods()) {
			IManagedMethod managedMethod = new ManagedMethod(this, method);
			managedMethod.scanServices(container.getServices());
			methodsPool.put(method.getName(), managedMethod);
		}

		for (IContainerService service : container.getServices()) {
			if (service instanceof IClassPostLoadedProcessor) {
				IClassPostLoadedProcessor processor = (IClassPostLoadedProcessor) service;
				processor.onClassPostLoaded(this);
			}
			if (service instanceof IInstancePostConstructProcessor) {
				IInstancePostConstructProcessor processor = (IInstancePostConstructProcessor) service;
				if (processor.bind(this)) {
					instancePostConstructors.add(processor);
				}
			}
			if (service instanceof IInstancePreDestroyProcessor) {
				IInstancePreDestroyProcessor processor = (IInstancePreDestroyProcessor) service;
				if (processor.bind(this)) {
					instancePreDestructors.add(processor);
				}
			}
		}

	}

	void executeInstancePostConstructors(Object instance) {
		instancePostConstructors.forEach(processor -> {
			processor.onInstancePostConstruct(instance);
		});
	}

	void executeInstancePreDestructors(Object instance) {
		instancePreDestructors.forEach(processor -> {
			processor.onInstancePreDestroy(instance);
		});
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED CLASS SPI

	@Override
	public IContainer getContainer() {
		return container;
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
	public T getInstance() {
		return container.getInstance(interfaceClass);
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

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if (implementationClass == null) {
			return null;
		}
		A annotation = implementationClass.getAnnotation(annotationClass);
		if (annotation == null) {
			for (Class<?> interfaceClass : implementationClass.getInterfaces()) {
				annotation = interfaceClass.getAnnotation(annotationClass);
				if (annotation != null) {
					break;
				}
			}
		}
		return annotation;
	}
}
