package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.InstanceType;
import js.util.Params;

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
	 * Optional managed class implementation. It can be null if managed class does not require implementation, for example if is
	 * a remote class or a Java service.
	 */
	private final Class<? extends T> implementationClass;

	/**
	 * Managed methods pool for managed classes of type {@link InstanceType#PROXY}. Used to find out managed method bound to
	 * interface method.
	 */
	private final Map<String, IManagedMethod> methodsPool = new HashMap<>();

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services. This list contains processors in execution order.
	 */
	private final FlowProcessorsSet<IInstancePostConstructProcessor> instancePostConstructors = new FlowProcessorsSet<>();

	private final FlowProcessorsSet<IInstancePreDestroyProcessor> instancePreDestructors = new FlowProcessorsSet<>();

	public ManagedClass(Container container, Class<T> interfaceClass, Class<? extends T> implementationClass) {
		Params.notNull(container, "Container");
		Params.notNull(interfaceClass, "Interface class");
		Params.notNull(implementationClass, "Implementation class");

		this.container = container;
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
	}

	void scanServices() {
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

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		A annotation = implementationClass.getAnnotation(annotationClass);
		if (annotation == null) {
			annotation = interfaceClass.getAnnotation(annotationClass);
		}
		return annotation;
	}

	@Override
	public String toString() {
		return interfaceClass.getCanonicalName();
	}
}
