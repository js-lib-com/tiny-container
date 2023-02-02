package com.jslib.container.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.jslib.api.container.EmbeddedContainer;
import com.jslib.api.injector.IBindingBuilder;
import com.jslib.api.injector.IModule;
import com.jslib.api.injector.IScopeFactory;
import com.jslib.api.injector.ProvisionException;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.cdi.BindingParametersBuilder;
import com.jslib.container.cdi.CDI;
import com.jslib.container.cdi.IClassBinding;
import com.jslib.container.cdi.IManagedLoader;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IContainerService;
import com.jslib.container.spi.IContainerStartProcessor;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IThreadsPool;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.lang.Config;
import com.jslib.lang.ConfigException;
import com.jslib.util.Params;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

/**
 * Container core implementation.
 * 
 * @author Iulian Rotaru
 */
public class Container implements IContainer, EmbeddedContainer, IManagedLoader {
	private static final Log log = LogFactory.getLog(Container.class);

	private CDI cdi;

	private Set<IContainerService> services;

	/**
	 * Master cache for all managed classes registered to container. Since an application has one container instance, managed
	 * classes cache is unique per application.
	 */
	private List<IManagedClass<?>> managedClasses;

	/** Managed classes indexed by interface class. */
	private Map<Class<?>, IManagedClass<?>> managedInterfaces;

	/** Managed classes indexed by implementation class. */
	private Map<Class<?>, ManagedClass<?>> managedImplementations;

	private FlowProcessorsSet<IContainerStartProcessor> containerStartProcessors;

	protected void init(CDI cdi) {
		log.trace("CDI");

		this.cdi = cdi;
		this.cdi.setManagedLoader(this);
		this.cdi.setInstanceCreatedListener(this);

		this.services = new HashSet<>();
		this.managedClasses = new ArrayList<>();
		this.managedInterfaces = new HashMap<>();
		this.managedImplementations = new HashMap<>();
		this.containerStartProcessors = new FlowProcessorsSet<>();

		bind(IContainer.class).instance(this).build();
		bind(EmbeddedContainer.class).instance(this).build();
		bind(IThreadsPool.class).to(ThreadsPool.class).build();
		bind(ConverterRegistry.class).instance(ConverterRegistry.getInstance()).build();
		bind(Converter.class).instance(ConverterRegistry.getConverter()).build();

		for (IContainerService service : ServiceLoader.load(IContainerService.class)) {
			log.debug("Load container service |{java_type}|.", service.getClass());
			services.add(service);
		}
	}
	
	@Override
	public <T> IBindingBuilder<T> bind(Class<T> interfaceClass) {
		return new BindingParametersBuilder<>(cdi, interfaceClass);
	}

	@Override
	public void bindScope(Class<? extends Annotation> annotation, IScopeFactory<?> scopeFactory) {
		cdi.bindScope(annotation, scopeFactory);
	}

	/**
	 * Create all managed classes registered to this container via external application descriptor. For every found managed
	 * class execute {@link #classPostLoadedProcessors}. After managed classes initialization configure CDI.
	 * 
	 * @param config container configuration object.
	 * @throws ConfigException if module configuration is not valid.
	 */
	public void configure(Config config) throws ConfigException {
		log.trace("configure(Config)");
		services.forEach(service -> service.configure(this));
		try {
			create(cdi.configure(config));
		} catch (Exception e) {
			throw new ConfigException(e.getMessage());
		}
	}

	public void modules(IModule... modules) {
		log.trace("modules(Object...)");
		services.forEach(service -> service.configure(this));
		create(cdi.configure(modules));
	}

	protected void create(List<IClassBinding<?>> bindings) {
		log.trace("create(List<IClassBinding<?>>)");

		services.forEach(service -> {
			service.create(this);
			if (service instanceof IContainerStartProcessor) {
				containerStartProcessors.add((IContainerStartProcessor) service);
			}
		});

		for (IClassBinding<?> binding : bindings) {
			ManagedClass<?> managedClass = new ManagedClass<>(this, binding);
			if (!managedClass.scanServices()) {
				continue;
			}

			log.debug("Create managed class |{managed_class}|.", managedClass);
			managedClasses.add(managedClass);

			managedInterfaces.put(binding.getInterfaceClass(), managedClass);
			managedImplementations.put(binding.getImplementationClass(), managedClass);
		}

		services.forEach(service -> {
			service.postCreate(this);
		});
	}

	/** Execute container start processors, registered to {@link #containerStartProcessors}. */
	public void start() {
		log.debug("Start container.");
		containerStartProcessors.forEach(processor -> processor.onContainerStart(this));
	}

	private static final AtomicInteger LOW_VALUE = new AtomicInteger(Integer.MAX_VALUE >> 1);

	/** Execute container close processors, registered to {@link #containerCloseProcessors}, then destroy all services. */
	@Override
	public void close() {
		log.debug("Destroy container.");
		if(this.managedClasses == null) {
			log.debug("Destroy container before initialization.");
			return;
		}
		
		try {
			SortedMap<Integer, IManagedClass<?>> managedClasses = new TreeMap<>(Collections.reverseOrder());
			for (IManagedClass<?> managedClass : this.managedClasses) {
				Priority priorityAnnotation = managedClass.scanAnnotation(Priority.class);
				int priority = priorityAnnotation != null ? priorityAnnotation.value() : LOW_VALUE.getAndIncrement();
				managedClasses.put(priority, managedClass);
			}

			for (IManagedClass<?> managedClass : managedClasses.values()) {
				Object instance = cdi.getScopeInstance(Singleton.class, managedClass.getInterfaceClass());
				if (instance != null) {
					onInstanceOutOfScope(instance);
				}
			}

			services.forEach(IContainerService::destroy);
		} catch (Throwable t) {
			log.dump("Fatal error on container destroy:", t);
		}
	}

	// --------------------------------------------------------------------------------------------

	@Override
	public <T> T getInstance(Class<T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");
		return cdi.getInstance(interfaceClass);
	}

	@Override
	public void onInstanceCreated(Object instance) {
		ManagedClass<?> managedClass = managedImplementations.get(instance.getClass());
		// not all instances created by injector have managed classes
		if (managedClass != null) {
			managedClass.onInstanceCreated(instance);
		}
	}

	/**
	 * Receives notification that an instance was cleared from a scope cache.
	 * 
	 * @param instance
	 */
	@Override
	public void onInstanceOutOfScope(Object instance) {
		ManagedClass<?> managedClass = managedImplementations.get(instance.getClass());
		// not all instances created by injector have managed classes
		if (managedClass != null) {
			managedClass.onInstanceOutOfScope(instance);
		}
	}

	@Override
	public <T> T getOptionalInstance(Class<T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");
		try {
			return getInstance(interfaceClass);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public <T> T getScopeInstance(Class<? extends Annotation> scope, Class<T> interfaceClass) {
		return cdi.getScopeInstance(scope, interfaceClass);
	}

	@Override
	public List<IManagedClass<?>> getManagedClasses() {
		return managedClasses;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		return (IManagedClass<T>) managedInterfaces.get(interfaceClass);
	}

	private static final Converter converter = ConverterRegistry.getConverter();

	@Override
	public <T> T getInitParameter(String name, Class<T> type) {
		return converter.asObject(System.getProperty(name), type);
	}

	// --------------------------------------------------------------------------------------------

	Collection<IContainerService> getServices() {
		return services;
	}

	Map<Class<?>, IManagedClass<?>> managedInterfaces() {
		return managedInterfaces;
	}

	Map<Class<?>, ManagedClass<?>> managedImplementations() {
		return managedImplementations;
	}
}
