package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import js.injector.AbstractModule;
import js.injector.IInjector;
import js.injector.IModule;
import js.injector.IProvisionInvocation;
import js.injector.IProvisionListener;
import js.injector.IScope;
import js.injector.Key;
import js.injector.ProvisionException;
import js.lang.Config;
import js.log.Log;
import js.log.LogFactory;
import js.util.Params;

/**
 * Facade for injection implementation.
 * 
 * @author Iulian Rotaru
 */
public class CDI implements IProvisionListener {
	private static final Log log = LogFactory.getLog(CDI.class);

	public static CDI create() {
		log.trace("create()");
		return new CDI();
	}

	public static CDI create(boolean proxyProcessing) {
		log.trace("create(boolean)");
		return new CDI(proxyProcessing);
	}

	// --------------------------------------------------------------------------------------------

	private final boolean proxyProcessing;
	
	/** Explicit bindings, and instance and scope bindings collected from container. */
	private final StaticModule staticModule;

	private final ContainerModule containerModule;

	/**
	 * Injector implementation used by this CDI instance. Injector is immutable; once configured is forbidden to alter its
	 * bindings.
	 */
	private final IInjector injector;

	/** Flag true only after CDI configuration complete. Used to assert CDI internal state consistency. */
	private final AtomicBoolean configured = new AtomicBoolean(false);
	
	private IManagedLoader managedLoader;
	private IInstanceCreatedListener instanceCreatedListener;

	private CDI() {
		this(false);
	}
	
	private CDI(boolean proxyProcessing) {
		log.trace("CDI(boolean)");
		this.proxyProcessing = proxyProcessing;
		this.staticModule = new StaticModule();
		this.containerModule = new ContainerModule();
		this.injector = IInjector.create();
	}

	public void setManagedLoader(IManagedLoader managedLoader) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to set managed class loader after injector configuration.");
		}
		this.managedLoader = managedLoader;
	}

	public void setInstanceCreatedListener(IInstanceCreatedListener instanceCreatedListener) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to set instance created listener after injector configuration.");
		}
		injector.bindListener(this);
		this.instanceCreatedListener = instanceCreatedListener;
	}

	public <T> void bind(ContainerBindingParameters<T> binding) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to add binding after injector configuration: " + binding);
		}
		containerModule.addBinding(binding);
	}

	public <T> void bindInstance(Class<T> interfaceClass, T instance) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind instance after injector configuration: " + interfaceClass);
		}
		staticModule.instances.put(interfaceClass, instance);
	}

	public void bindScope(Class<? extends Annotation> annotation, IScope<?> scope) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind scope after injector configuration: " + annotation);
		}
		staticModule.scopes.put(annotation, scope);
	}

	/**
	 * Create bindings for container managed classes and configure injector implementation.
	 * 
	 * @param classDescriptors container managed classes.
	 */
	public List<ClassBinding<?>> configure(Config config) {
		log.trace("configure(Config)");
		return configure(new ConfigModule(config));
	}

	public List<ClassBinding<?>> configure(Object... arguments) {
		log.trace("configure(Object...)");

		ManagedModule managedModule = new ManagedModule(injector, managedLoader, proxyProcessing);
		managedModule.addModule(staticModule);
		managedModule.addModule(containerModule);
		for (Object argument : arguments) {
			if (!(argument instanceof IModule)) {
				throw new IllegalArgumentException("Invalid module type " + argument.getClass());
			}
			managedModule.addModule((IModule) argument);
		}

		injector.configure(managedModule);
		configured.set(true);

		return managedModule.getClassBindings();
	}

	/**
	 * Retrieve an not null instance, be it newly created or reused from a scope cache. Instance to retrieve is identified by
	 * the interface class. Here the term interface class is used in a broad sense. It is not necessary to be a Java interface;
	 * it can be an abstract or even a concrete one. The point is that this interface class identifies the instance at injector
	 * bindings level.
	 * 
	 * Depending on injector binding and current context a new instance can be created or one can be reused from a scope cache.
	 * Instance post construction listener is invoked only if a new instance is created.
	 * 
	 * @param interfaceClass interface class used to identify the instance,
	 * @param instanceListener event listener for instance post construction.
	 * @return instance, newly created or reused from scope.
	 * @throws ProvisionException if there is no bindings for requested interface.
	 * @param <T> instance generic type.
	 */
	public <T> T getInstance(Class<T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");
		if (!configured.get()) {
			throw new IllegalStateException("Attempt to retrieve instance before injector configuration: " + interfaceClass);
		}
		return injector.getInstance(Key.get(interfaceClass));
	}

	@Override
	public <T> void onProvision(IProvisionInvocation<T> invocation) {
		instanceCreatedListener.onInstanceCreated(invocation.instance());
	}

	/**
	 * Retrieve an instance from a scope cache but does not create a new instance on cache miss. This method should be used only
	 * on instances configured with scope.
	 * 
	 * @param interfaceClass interface class used to identify the instance.
	 * @return instance from scope cache, possible null.
	 * @param <T> instance generic type.
	 * @throws IllegalStateException if there is no provide for given interface class or is not a scope provider.
	 */
	public <T> T getScopeInstance(Class<T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");
		return injector.getScopeInstance(interfaceClass);
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * Injector module for explicit bindings. This module is designed to facilitate container to bind instances and scope
	 * providers.
	 * 
	 * @author Iulian Rotaru
	 */
	private class StaticModule extends AbstractModule {
		final Map<Class<?>, Object> instances = new HashMap<>();
		final Map<Class<? extends Annotation>, IScope<?>> scopes = new HashMap<>();

		@SuppressWarnings("unchecked")
		@Override
		protected void configure() {
			instances.forEach((interfaceClass, instance) -> {
				log.debug("CDI register instance for |%s|.", interfaceClass);
				bindInstance((Class<Object>) interfaceClass, instance);
			});

			scopes.forEach((annotation, scope) -> {
				log.debug("CDI register scope |%s|.", annotation);
				injector.bindScope(annotation, scope);
			});
		}
	}
}