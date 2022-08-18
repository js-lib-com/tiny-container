package com.jslib.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.cdi.IClassBinding;
import com.jslib.container.spi.IClassPostLoadedProcessor;
import com.jslib.container.spi.IConnector;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IContainerService;
import com.jslib.container.spi.IInstanceLifecycleListener;
import com.jslib.container.spi.IInstancePostConstructProcessor;
import com.jslib.container.spi.IInstancePreDestroyProcessor;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.lang.InstanceInvocationHandler;
import com.jslib.util.Classes;

/**
 * Implementation for {@link IManagedClass} interface.
 * 
 * @author Iulian Rotaru
 */
class ManagedClass<T> implements IManagedClass<T>, IInstanceLifecycleListener {
	private static final Log log = LogFactory.getLog(ManagedClass.class);

	/** Back reference to parent container. */
	private final Container container;

	/** Wrapped business interface exposed by {@link #getInterfaceClass()}. */
	private final Class<T> interfaceClass;

	/** Wrapped business class exposed by {@link #getImplementationClass()}. */
	private final Class<? extends T> implementationClass;

	/** Managed methods defined by implementation class. Static methods are not included. */
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

		for (Method method : interfaceClass.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}
			ManagedMethod managedMethod = new ManagedMethod(this, method);
			if (managedMethod.scanServices(container.getServices())) {
				servicesFound = true;
			}
			managedMethods.put(method.getName(), managedMethod);
			
			// TODO: refactor managed classes creation logic; CDI should not create class bindings for service and remote
			// providers
			// TODO: check for methods overload
			// if (managedMethods.put(method.getName(), managedMethod) != null) {
			// throw new IllegalStateException("Method overloaded not supported. See managed method " + managedMethod);
			// }
		}

		for (IContainerService service : container.getServices()) {
			if (service instanceof IConnector) {
				IConnector connector = (IConnector) service;
				if (connector.bind(this)) {
					servicesFound = true;
				}
			}
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
	public void onInstanceCreated(Object instance) {
		// in case instance is a Java Proxy
		// takes care to execute post-construct processors on wrapped instance in order to avoid adding container services
		if (instance instanceof Proxy) {
			if (!(Proxy.getInvocationHandler(instance) instanceof InstanceInvocationHandler)) {
				return;
			}
			instance = Classes.unproxy(instance);
		}

		for (IInstancePostConstructProcessor processor : instancePostConstructors) {
			processor.onInstancePostConstruct(instance);
		}
	}

	@Override
	public void onInstanceOutOfScope(Object instance) {
		// in case instance is a Java Proxy
		// takes care to execute pre-destroy processors on wrapped instance in order to avoid adding container services
		if (instance instanceof Proxy) {
			if (!(Proxy.getInvocationHandler(instance) instanceof InstanceInvocationHandler)) {
				return;
			}
			instance = Classes.unproxy(instance);
		}

		for (IInstancePreDestroyProcessor processor : instancePreDestructors) {
			processor.onInstancePreDestroy(instance);
		}
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
			log.error("Missing remote method |{java_method}| from |{java_type}|.", methodName, implementationClass);
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
