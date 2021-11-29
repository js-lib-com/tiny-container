package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.ClassBinding;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

/**
 * Implementation for {@link IManagedClass} interface.
 * 
 * @author Iulian Rotaru
 */
class ManagedClass<T> implements IManagedClass<T> {
	private static final Log log = LogFactory.getLog(ManagedClass.class);

	/** Back reference to parent container. */
	private final Container container;

	/** Wrapped business interface exposed by {@link #getInterfaceClass()}. */
	private final Class<T> interfaceClass;

	/** Wrapped business class exposed by {@link #getImplementationClass()}. */
	private final Class<? extends T> implementationClass;

	private final Map<String, IManagedMethod> methodsPool = new HashMap<>();

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services.
	 */
	private final FlowProcessorsSet<IInstancePostConstructProcessor> instancePostConstructors;

	private final FlowProcessorsSet<IInstancePreDestroyProcessor> instancePreDestructors;

	public ManagedClass(Container container, ClassBinding<T> binding) {
		this.container = container;
		this.interfaceClass = binding.getInterfaceClass();
		this.implementationClass = binding.getImplementationClass();

		this.instancePostConstructors = new FlowProcessorsSet<>();
		this.instancePreDestructors = new FlowProcessorsSet<>();
	}

	/** Create managed methods for implementation class and scan container services. */
	public void scanServices() {
		for (Method method : implementationClass.getDeclaredMethods()) {
			ManagedMethod managedMethod = new ManagedMethod(this, method);
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

	public void executeInstancePostConstructors(Object instance) {
		instancePostConstructors.forEach(processor -> {
			processor.onInstancePostConstruct(instance);
		});
	}

	public void executeInstancePreDestructors(Object instance) {
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
	public Collection<IManagedMethod> getManagedMethods() {
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
	public <A extends Annotation> A scanAnnotation(Class<A> annotationClass) {
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

	// --------------------------------------------------------------------------------------------

	FlowProcessorsSet<IInstancePostConstructProcessor> instancePostConstructors() {
		return instancePostConstructors;
	}

	FlowProcessorsSet<IInstancePreDestroyProcessor> instancePreDestructors() {
		return instancePreDestructors;
	}
}
