package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import js.lang.InstanceInvocationHandler;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.IClassBinding;
import js.tiny.container.cdi.IInstanceCreatedListener;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

/**
 * Implementation for {@link IManagedClass} interface.
 * 
 * @author Iulian Rotaru
 */
class ManagedClass<T> implements IManagedClass<T>, IInstanceCreatedListener {
	private static final Log log = LogFactory.getLog(ManagedClass.class);

	/** Back reference to parent container. */
	private final Container container;

	/** Wrapped business interface exposed by {@link #getInterfaceClass()}. */
	private final Class<T> interfaceClass;

	/** Wrapped business class exposed by {@link #getImplementationClass()}. */
	private final Class<? extends T> implementationClass;

	private final Map<String, IManagedMethod> managedMethods = new HashMap<>();

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services.
	 */
	private final FlowProcessorsSet<IInstancePostConstructProcessor> instancePostConstructors;

	private final FlowProcessorsSet<IInstancePreDestroyProcessor> instancePreDestructors;

	public ManagedClass(Container container, IClassBinding<T> binding) {
		this.container = container;
		this.interfaceClass = binding.getInterfaceClass();
		this.implementationClass = binding.getImplementationClass();

		this.instancePostConstructors = new FlowProcessorsSet<>();
		this.instancePreDestructors = new FlowProcessorsSet<>();
	}

	/**
	 * Create managed methods for implementation class and scan container services. Returns true if found at least one container
	 * service declared on interface or implementation classes or on any method.
	 * 
	 * @return true if at least one service was found.
	 */
	public boolean scanServices() {
		boolean servicesFound = false;

		for (Method method : implementationClass.getDeclaredMethods()) {
			ManagedMethod managedMethod = new ManagedMethod(this, method);
			if (managedMethod.scanServices(container.getServices())) {
				servicesFound = true;
			}
			managedMethods.put(method.getName(), managedMethod);
		}

		for (IContainerService service : container.getServices()) {
			if (service instanceof IClassPostLoadedProcessor) {
				IClassPostLoadedProcessor processor = (IClassPostLoadedProcessor) service;
				if (processor.onClassPostLoaded(this)) {
					servicesFound = true;
				}
			}
			if (service instanceof IInstancePostConstructProcessor) {
				IInstancePostConstructProcessor processor = (IInstancePostConstructProcessor) service;
				if (processor.bind(this)) {
					servicesFound = true;
					instancePostConstructors.add(processor);
				}
			}
			if (service instanceof IInstancePreDestroyProcessor) {
				IInstancePreDestroyProcessor processor = (IInstancePreDestroyProcessor) service;
				if (processor.bind(this)) {
					servicesFound = true;
					instancePreDestructors.add(processor);
				}
			}
		}

		return servicesFound;
	}

	@Override
	public void close() {
		Object instance = container.getScopeInstance(interfaceClass);
		if (instance == null) {
			return;
		}

		// in case instance is a Java Proxy takes care to execute pre-destroy processors on wrapped instance in order to avoid
		// adding container services to this finalization hook
		if (instance instanceof Proxy) {
			if (!(Proxy.getInvocationHandler(instance) instanceof InstanceInvocationHandler)) {
				return;
			}
			instance = Classes.unproxy(instance);
		}

		log.debug("Pre-destroy managed instance |%s|.", this);
		for (IInstancePreDestroyProcessor processor : instancePreDestructors) {
			processor.onInstancePreDestroy(instance);
		}
	}

	@Override
	public void onInstanceCreated(Object instance) {
		instancePostConstructors.forEach(processor -> processor.onInstancePostConstruct(instance));
	}

	public void onInstanceDestroyed(Object instance) {
		instancePreDestructors.forEach(processor -> processor.onInstancePreDestroy(instance));
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
		return managedMethods.values();
	}

	@Override
	public IManagedMethod getManagedMethod(String methodName) {
		IManagedMethod managedMethod = managedMethods.get(methodName);
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
