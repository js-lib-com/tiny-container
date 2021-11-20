package js.tiny.container.core;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IInstanceCreatedListener;
import js.tiny.container.spi.IClassDescriptor;
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

	private final Set<IContainerService> containerServices = new HashSet<>();

	/**
	 * Master cache for all managed classes registered to container. Since an application has one container instance, managed
	 * classes cache is unique per application.
	 */
	private List<IManagedClass<?>> managedClasses = new ArrayList<>();

	/** Managed classes indexed by interface class. */
	private final Map<Class<?>, IManagedClass<?>> managedClassesByInterface = new LinkedHashMap<>();

	/** Managed classes indexed by implementation class. */
	private final Map<Class<?>, ManagedClass<?>> managedClassesByImplementation = new LinkedHashMap<>();

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
		this.cdi.bindInstance(IContainer.class, this);
		this.cdi.bindInstance(AppContainer.class, this);
	}

	/**
	 * Create all managed classes registered to this container via external application descriptor. For every found managed
	 * class execute {@link #classPostLoadedProcessors}. After managed classes initialization configure CDI.
	 * 
	 * @param config container configuration object.
	 * @throws ConfigException if container configuration fails.
	 */
	public void config(List<IClassDescriptor<?>> descriptors) throws ConfigException {
		log.trace("config(Config)");
		cdi.configure(descriptors, descriptor -> managedClassesByInterface.get(descriptor.getInterfaceClass()));
		cdi.bindListener(this);

		for (IContainerService service : ServiceLoader.load(IContainerService.class)) {
			log.debug("Load container service |%s|.", service.getClass());
			service.create(this);
			containerServices.add(service);

			if (service instanceof IContainerStartProcessor) {
				containerStartProcessors.add((IContainerStartProcessor) service);
			}
		}

		for (IClassDescriptor<?> descriptor : descriptors) {
			ManagedClass<?> managedClass = new ManagedClass<>(this, descriptor);
			log.debug("Register managed class |%s|.", managedClass);
			managedClasses.add(managedClass);

			managedClassesByInterface.put(descriptor.getInterfaceClass(), managedClass);
			managedClassesByImplementation.put(descriptor.getImplementationClass(), managedClass);
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

		ListIterator<IManagedClass<?>> iterator = managedClasses.listIterator();
		while (iterator.hasPrevious()) {
			IManagedClass<?> managedClass = iterator.previous();
			Object instance = cdi.getScopeInstance(managedClass.getInterfaceClass());
			if (instance == null) {
				return;
			}

			// in case instance is a Java Proxy takes care to execute pre-destroy hook on wrapped instance
			// in order to avoid adding container services to this finalization hook
			if (instance instanceof Proxy) {
				if (!(Proxy.getInvocationHandler(instance) instanceof InstanceInvocationHandler)) {
					return;
				}
				instance = Classes.unproxy(instance);
			}

			log.debug("Pre-destroy managed instance |%s|.", managedClass);
			((ManagedClass<?>) managedClass).executeInstancePreDestructors(instance);
		}

		for (IContainerService service : containerServices) {
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
		ManagedClass<?> managedClass = managedClassesByImplementation.get(instance.getClass());
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
	public Iterable<IManagedClass<?>> getManagedClasses() {
		return managedClasses;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		return (IManagedClass<T>) managedClassesByInterface.get(interfaceClass);
	}

	// --------------------------------------------------------------------------------------------

	Map<Class<?>, IManagedClass<?>> classesPool() {
		return managedClassesByInterface;
	}

	Collection<IContainerService> getServices() {
		return containerServices;
	}
}
