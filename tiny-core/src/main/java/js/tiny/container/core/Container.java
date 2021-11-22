package js.tiny.container.core;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.jslib.injector.ProvisionException;

import js.app.container.AppContainer;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.InstanceInvocationHandler;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.Binding;
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IInstanceCreatedListener;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IManagedClass;
import js.util.Classes;
import js.util.Params;

/**
 * Container core implementation.
 * 
 * @author Iulian Rotaru
 */
public class Container implements IContainer, AppContainer, IInstanceCreatedListener {
	private static final Log log = LogFactory.getLog(Container.class);

	protected final CDI cdi;

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
	 * Create factories and processors for instance retrieval but leave classes pool loading for {@link #config(Config)}. This
	 * constructor creates built-in factories and processors but subclass may add its own.
	 */
	public Container(CDI cdi) {
		this.cdi = cdi;
		this.cdi.setInstanceCreatedListener(this);
		this.cdi.bindInstance(IContainer.class, this);
		this.cdi.bindInstance(AppContainer.class, this);
	}

	/**
	 * Create all managed classes registered to this container via external application descriptor. For every found managed
	 * class execute {@link #classPostLoadedProcessors}. After managed classes initialization configure CDI.
	 * 
	 * @param config container configuration object.
	 */
	public void config(Config config) {
		log.trace("config(Config)");
		List<Binding<?>> bindings = cdi.configure(config, interfaceClass -> managedInterfaces.get(interfaceClass));

		for (IContainerService service : ServiceLoader.load(IContainerService.class)) {
			log.debug("Load container service |%s|.", service.getClass());
			service.create(this);
			services.add(service);

			if (service instanceof IContainerStartProcessor) {
				containerStartProcessors.add((IContainerStartProcessor) service);
			}
		}

		for (Binding<?> binding : bindings) {
			ManagedClass<?> managedClass = new ManagedClass<>(this, binding);
			managedClass.scanServices();

			log.debug("Register managed class |%s|.", managedClass);
			managedClasses.add(managedClass);

			managedInterfaces.put(binding.getInterfaceClass(), managedClass);
			managedImplementations.put(binding.getImplementationClass(), managedClass);
		}
	}

	public void config(Object... modules) throws ConfigException {
		cdi.configure(modules);
	}

	/** Execute container services registered to {@link #containerStartProcessors}. */
	public void start() {
		log.trace("start()");
		containerStartProcessors.forEach(processor -> {
			processor.onContainerStart(this);
		});
	}

	/** Clean-up managed classes by invoking pre-destroy method in reverse creation order then destroy all services. */
	@Override
	public void close() {
		log.trace("close()");

		// although not specified in apidoc, list iterator size should be provided
		ListIterator<IManagedClass<?>> iterator = managedClasses.listIterator(managedClasses.size());
		while (iterator.hasPrevious()) {
			IManagedClass<?> managedClass = iterator.previous();
			Object instance = cdi.getScopeInstance(managedClass.getInterfaceClass());
			if (instance == null) {
				continue;
			}

			// in case instance is a Java Proxy takes care to execute pre-destroy hook on wrapped instance
			// in order to avoid adding container services to this finalization hook
			if (instance instanceof Proxy) {
				if (!(Proxy.getInvocationHandler(instance) instanceof InstanceInvocationHandler)) {
					continue;
				}
				instance = Classes.unproxy(instance);
			}

			log.debug("Pre-destroy managed instance |%s|.", managedClass);
			((ManagedClass<?>) managedClass).executeInstancePreDestructors(instance);
		}

		for (IContainerService service : services) {
			log.debug("Destroy container service |%s|.", service);
			service.destroy();
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
		// is legal to have instances without managed classes, e.g. POJO
		if (managedClass != null) {
			managedClass.executeInstancePostConstructors(instance);
		}
	}

	@Override
	public <T> T getOptionalInstance(Class<T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");

		// here is a piece of code that uses exception for normal logic flow but I do not see alternative
		// ServiceInstanceFactory should throw exception that propagates to AppFactory and application code
		// on the other hand this getOptionalInstance() should return null for missing service provider

		try {
			return getInstance(interfaceClass);
		} catch (ProvisionException e) {
			return null;
		}
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

	Map<Class<?>, IManagedClass<?>> managedClassesByInterface() {
		return managedInterfaces;
	}

}
