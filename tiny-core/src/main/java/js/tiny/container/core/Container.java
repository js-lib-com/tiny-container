package js.tiny.container.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import com.jslib.injector.ProvisionException;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.InstanceInvocationHandler;
import js.lang.ManagedPreDestroy;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.CDI;
import js.tiny.container.service.ConfigurableInstanceProcessor;
import js.tiny.container.service.FlowProcessorsSet;
import js.tiny.container.service.InstanceFieldsInitializationProcessor;
import js.tiny.container.service.ResourcesInjectionProcessor;
import js.tiny.container.service.InstancePostConstructProcessor;
import js.tiny.container.service.InstanceStartupProcessor;
import js.tiny.container.service.LoggerInstanceProcessor;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IInstancePreDestructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;
import js.util.Params;

/**
 * Container core implementation.
 *  
 * @author Iulian Rotaru
 */
public class Container implements IContainer, Configurable {
	private static final Log log = LogFactory.getLog(Container.class);

	protected final CDI cdi;

	private final Set<IContainerService> containerServices = new HashSet<>();

	/**
	 * Master cache for all managed classes registered to container. Since an application has one and only one container
	 * instance, managed classes pool is unique per application. This pool is initialized by {@link #config(Config)} method.
	 */
	private final Map<Class<?>, IManagedClass<?>> classesPool = new HashMap<>();

	private final FlowProcessorsSet<IContainerStartProcessor> containerStartProcessors = new FlowProcessorsSet<>();

	/**
	 * Class post-load processors are executed after {@link ManagedClass} creation and generally deals with managed
	 * implementation static fields initialization, but is not limited to.
	 * <p>
	 * These processors are registered by {@link #registerClassPostLoadProcessor(IClassPostLoadedProcessor)}. Note that these
	 * processors are global and executed for ALL managed classes.
	 */
	private final FlowProcessorsSet<IClassPostLoadedProcessor> classPostLoadedProcessors = new FlowProcessorsSet<>();

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services. This list contains processors in execution order.
	 * <p>
	 * There are a number of built-in processor created by constructor but subclass may register new ones via
	 * {@link #registerInstanceProcessor(IInstancePostConstructionProcessor)}.
	 */
	private final FlowProcessorsSet<IInstancePostConstructionProcessor> instancePostConstructionProcessors = new FlowProcessorsSet<>();

	private final FlowProcessorsSet<IInstancePreDestructionProcessor> instancePreDestructionProcessors = new FlowProcessorsSet<>();

	public Container() {
		this(CDI.create());
	}

	/**
	 * Create factories and processors for instance retrieval but leave classes pool loading for {@link #config(Config)}. This
	 * constructor creates built-in factories and processors but subclass may add its own.
	 */
	public Container(CDI cdi) {
		this.cdi = cdi;
		this.cdi.bindInstance(IContainer.class, this);

		// load external and built-in container services

		for (IContainerServiceProvider provider : ServiceLoader.load(IContainerServiceProvider.class)) {
			IContainerService service = provider.getService(this);
			log.debug("Load container service |%s|.", service.getClass());
			containerServices.add(service);

			if (service instanceof IContainerStartProcessor) {
				containerStartProcessors.add((IContainerStartProcessor) service);
			}
			if (service instanceof IClassPostLoadedProcessor) {
				classPostLoadedProcessors.add((IClassPostLoadedProcessor) service);
			}
			if (service instanceof IInstancePostConstructionProcessor) {
				instancePostConstructionProcessors.add((IInstancePostConstructionProcessor) service);
			}
			if (service instanceof IInstancePreDestructionProcessor) {
				instancePreDestructionProcessors.add((IInstancePreDestructionProcessor) service);
			}
		}

		containerStartProcessors.add(new InstanceStartupProcessor());

		instancePostConstructionProcessors.add(new ResourcesInjectionProcessor());
		instancePostConstructionProcessors.add(new InstanceFieldsInitializationProcessor());
		instancePostConstructionProcessors.add(new ConfigurableInstanceProcessor());
		instancePostConstructionProcessors.add(new InstancePostConstructProcessor());
		instancePostConstructionProcessors.add(new LoggerInstanceProcessor());
	}

	/**
	 * Create all managed classes registered to this container via external application descriptor. For every found managed
	 * class execute {@link #classPostLoadedProcessors}. After managed classes initialization configure CDI.
	 * 
	 * @param config container configuration object.
	 * @throws ConfigException if container configuration fails.
	 */
	@Override
	public void config(Config config) throws ConfigException {
		log.trace("config(Config)");

		log.debug("Load managed classes from application descriptor.");
		Config managedClassesSection = config.getChild("managed-classes");
		if (managedClassesSection != null) {
			for (Config classDescriptor : managedClassesSection.getChildren()) {
				if (!classDescriptor.hasAttribute("interface")) {
					classDescriptor.setAttribute("interface", classDescriptor.getAttribute("class"));
				}

				ManagedClass<?> managedClass = new ManagedClass<>(this, classDescriptor);
				log.debug("Register managed class |%s|.", managedClass);
				classesPool.put(managedClass.getInterfaceClass(), managedClass);

				classPostLoadedProcessors.forEach(processor -> {
					processor.onClassPostLoaded(managedClass);
				});
			}
		}

		log.debug("Configure CDI.");
		cdi.configure(classesPool.values());
	}

	/** Execute container services registered to {@link #containerStartProcessors}. */
	public void start() {
		log.trace("start()");
		containerStartProcessors.forEach(processor -> {
			processor.onContainerStart(this);
		});
	}

	/**
	 * Destroy container and release caches, factories and processors. This is container global clean-up invoked at application
	 * unload; after executing this method no managed instance can be created or reused. Attempting to use {@link AppFactory}
	 * after container destroy may lead to not predictable behavior.
	 * <p>
	 * Before caches clean-up, this method invokes {@link ManagedPreDestroy} on all managed instances that are active, that is,
	 * cached in all scope factories. Takes care to execute pre-destroy in reverse order from application descriptor
	 * declarations. This ensure {@link App} is destroyed last since App class descriptor is declared first into application
	 * descriptor.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void destroy() {
		log.trace("destroy()");

		// classes pool is not sorted
		// bellow sorted set is used to ensure reverse order on managed classes destruction

		// comparison is based on managed class key that is created incrementally
		// compare second with first to ensure descending sorting
		Set<IManagedClass> sortedClasses = new TreeSet<>((o1, o2) -> o2.getKey().compareTo(o1.getKey()));
		for (IManagedClass managedClass : classesPool.values()) {
			if (!managedClass.getInstanceScope().isLOCAL()) {
				sortedClasses.add(managedClass);
			}
		}

		for (IManagedClass managedClass : sortedClasses) {
			Object instance = cdi.getScopeInstance(managedClass.getInterfaceClass());
			if (instance == null) {
				log.debug("Cannot obtain instance for pre-destroy method for class |%s|.", managedClass);
				continue;
			}

			// sorted managed classes contains only implementations of pre-destroy interface
			// in case instance is a Java Proxy takes care to execute pre-destroy hook on wrapped instance
			// in order to avoid adding container services to this finalization hook
			if (instance instanceof InstanceInvocationHandler) {
				instance = Classes.unproxy(instance);
			}
			log.debug("Pre-destroy managed instance |%s|.", instance.getClass());

			final Object finalInstance = instance;
			instancePreDestructionProcessors.forEach(processor -> {
				processor.onInstancePreDestruction(managedClass, finalInstance);
			});
		}

		for (IContainerService containerService : containerServices) {
			containerService.destroy();
		}

		classesPool.clear();
		classPostLoadedProcessors.clear();
		instancePostConstructionProcessors.clear();
		instancePreDestructionProcessors.clear();
	}

	// --------------------------------------------------------------------------------------------

	@Override
	public <T> T getInstance(Class<? super T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");

		@SuppressWarnings("unchecked")
		IManagedClass<T> managedClass = (IManagedClass<T>) classesPool.get(interfaceClass);
		if (managedClass == null) {
			throw new BugError("No managed class associated with interface class |%s|.", interfaceClass);
		}

		return getInstance(managedClass);
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");

		@SuppressWarnings("unchecked")
		IManagedClass<T> managedClass = (IManagedClass<T>) classesPool.get(interfaceClass);
		if (managedClass == null) {
			return null;
		}

		// here is a piece of code that uses exception for normal logic flow but I do not see alternative
		// ServiceInstanceFactory should throw exception that propagates to AppFactory and application code
		// on the other hand this getOptionalInstance() should return null for missing service provider

		try {
			return getInstance(managedClass);
		} catch (ProvisionException e) {
			// log record is not an error since exception is expected
			log.debug(e);
			return null;
		}
	}

	@Override
	public <T> T getInstance(IManagedClass<T> managedClass) {
		return cdi.getInstance(managedClass.getInterfaceClass(), (instanceManagedClass, instance) -> {
			instancePostConstructionProcessors.forEach(processor -> {
				processor.onInstancePostConstruction(instanceManagedClass, instance);
			});
		});
	}

	// ----------------------------------------------------
	// CONTAINER SPI

	@Override
	public Iterable<IManagedClass<?>> getManagedClasses() {
		return classesPool.values();
	}

	@Override
	public boolean isManagedClass(Class<?> interfaceClass) {
		return classesPool.containsKey(interfaceClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		return (IManagedClass<T>) classesPool.get(interfaceClass);
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		return new Iterable<IManagedMethod>() {
			@Override
			public Iterator<IManagedMethod> iterator() {
				return new ManagedMethodsIterator();
			}
		};
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASSES

	/**
	 * Iterator for all managed methods defined by this container. Traverses all managed methods from all managed classes from
	 * {@link Container#classesPool}. There is no guarantee for a particular order.
	 * 
	 * @author Iulian Rotaru
	 */
	private class ManagedMethodsIterator implements Iterator<IManagedMethod> {
		/** Managed classes iterator. */
		private final Iterator<IManagedClass<?>> classesIterator;

		/** Iterator on managed methods from current managed class. */
		private Iterator<IManagedMethod> currentMethodsIterator;

		/**
		 * Initialize iterators for managed classes and current class methods.
		 */
		public ManagedMethodsIterator() {
			classesIterator = classesPool.values().iterator();
			if (!classesIterator.hasNext()) {
				throw new BugError("Empty classes pool.");
			}
			currentMethodsIterator = nextMethodIterator();
		}

		@Override
		public boolean hasNext() {
			while (!currentMethodsIterator.hasNext()) {
				if (!classesIterator.hasNext()) {
					return false;
				}
				currentMethodsIterator = nextMethodIterator();
			}
			return true;
		}

		@Override
		public IManagedMethod next() {
			return currentMethodsIterator.next();
		}

		private Iterator<IManagedMethod> nextMethodIterator() {
			return classesIterator.next().getManagedMethods().iterator();
		}
	}

	// --------------------------------------------------------------------------------------------

	public void config(IManagedClass<?>... managedClasses) {
		for (IManagedClass<?> managedClass : managedClasses) {
			classesPool.put(managedClass.getInterfaceClass(), managedClass);
		}
	}

	Map<Class<?>, IManagedClass<?>> classesPool() {
		return classesPool;
	}

	Collection<IContainerService> getServices() {
		return containerServices;
	}
}
