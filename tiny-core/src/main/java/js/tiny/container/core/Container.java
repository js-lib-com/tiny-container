package js.tiny.container.core;

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

import javax.inject.Singleton;

import js.embedded.container.EmbeddedContainer;
import js.injector.IBindingBuilder;
import js.injector.IScopeFactory;
import js.injector.ProvisionException;
import js.lang.Config;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.BindingParametersBuilder;
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IClassBinding;
import js.tiny.container.cdi.IManagedLoader;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IManagedClass;
import js.util.Params;

/**
 * Container core implementation.
 * 
 * @author Iulian Rotaru
 */
public class Container implements IContainer, EmbeddedContainer, IManagedLoader {
	private static final Log log = LogFactory.getLog(Container.class);

	private final CDI cdi;

	private final Set<IContainerService> services = new HashSet<>();

	/**
	 * Master cache for all managed classes registered to container. Since an application has one container instance, managed
	 * classes cache is unique per application.
	 */
	private List<IManagedClass<?>> managedClasses = new ArrayList<>();

	/** Managed classes indexed by interface class. */
	private final Map<Class<?>, IManagedClass<?>> managedInterfaces = new HashMap<>();

	/** Managed classes indexed by implementation class. */
	private final Map<Class<?>, ManagedClass<?>> managedImplementations = new HashMap<>();

	private final FlowProcessorsSet<IContainerStartProcessor> containerStartProcessors = new FlowProcessorsSet<>();

	public Container() {
		this(CDI.create());
	}

	/**
	 * Create factories and processors for instance retrieval but leave classes pool loading for {@link #configure(Config)}.
	 * This constructor creates built-in factories and processors but subclass may add its own.
	 */
	public Container(CDI cdi) {
		this.cdi = cdi;
		this.cdi.setManagedLoader(this);
		this.cdi.setInstanceCreatedListener(this);

		bind(IContainer.class).instance(this).build();
		bind(EmbeddedContainer.class).instance(this).build();

		for (IContainerService service : ServiceLoader.load(IContainerService.class)) {
			log.debug("Load container service |%s|.", service.getClass());
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

	public void configure(Object... modules) {
		log.trace("configure(Object...)");
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

			log.debug("Create managed class |%s|.", managedClass);
			managedClasses.add(managedClass);

			managedInterfaces.put(binding.getInterfaceClass(), managedClass);
			managedImplementations.put(binding.getImplementationClass(), managedClass);
		}
	}

	/** Execute container start processors, registered to {@link #containerStartProcessors}. */
	public void start() {
		log.trace("start()");
		containerStartProcessors.forEach(processor -> processor.onContainerStart(this));
	}

	private static final AtomicInteger LOW_VALUE = new AtomicInteger(Integer.MAX_VALUE >> 1);

	/** Execute container close processors, registered to {@link #containerCloseProcessors}, then destroy all services. */
	@Override
	public void close() {
		log.trace("close()");

		SortedMap<Integer, IManagedClass<?>> managedClasses = new TreeMap<>(Collections.reverseOrder());
		for (IManagedClass<?> managedClass : this.managedClasses) {
			javax.annotation.Priority priorityAnnotation = managedClass.scanAnnotation(javax.annotation.Priority.class);
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
