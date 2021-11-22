package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import com.jslib.injector.IBindingBuilder;
import com.jslib.injector.IInjector;
import com.jslib.injector.IModule;
import com.jslib.injector.IProvisionInvocation;
import com.jslib.injector.IProvisionListener;
import com.jslib.injector.IScope;
import com.jslib.injector.Key;
import com.jslib.injector.ProvisionException;
import com.jslib.injector.ThreadScoped;
import com.jslib.injector.impl.AbstractModule;
import com.jslib.injector.impl.Injector;

import js.lang.Config;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
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

	// --------------------------------------------------------------------------------------------

	/** Explicit instance and scope bindings collected from container. */
	private final ExplicitBindingModule explicitBindings;

	/**
	 * Injector implementation used by this CDI instance. Injector is immutable; once configured is forbidden to alter its
	 * bindings.
	 */
	private final IInjector injector;

	/** Flag true only after CDI configuration complete. Used to assert CDI internal state consistency. */
	private final AtomicBoolean configured = new AtomicBoolean(false);

	private CDI() {
		log.trace("CDI()");
		this.explicitBindings = new ExplicitBindingModule();
		this.injector = new Injector();
	}

	/**
	 * Create bindings for container managed classes and configure injector implementation.
	 * 
	 * @param classDescriptors container managed classes.
	 */
	public List<Binding<?>> configure(Config config, Function<Class<?>, IManagedClass<?>> managedClassFactory) {
		log.trace("configure(Config)");

		ManagedClassesModule managedClassesModule = new ManagedClassesModule(config, managedClassFactory);
		injector.configure(explicitBindings, managedClassesModule);
		configured.set(true);

		List<Binding<?>> containerBindings = new ArrayList<>();
		containerBindings.addAll(explicitBindings.bindings.stream().filter(binding -> binding.getImplementationClass() != null).collect(Collectors.toList()));
		containerBindings.addAll(managedClassesModule.containerBindings);
		return containerBindings;
	}

	public Config configure(Object... modules) throws ConfigException {
		log.trace("configure(Object...)");
		List<IModule> injectorModules = new ArrayList<>();
		injectorModules.add(explicitBindings);

		for (Object module : modules) {
			if (!(module instanceof IModule)) {
				throw new IllegalArgumentException("Invalid module type " + module.getClass());
			}
			injectorModules.add((IModule) module);
		}

		// configBuilder.getManagedClasses().forEach(managedClass->{});

		injector.configure(injectorModules.toArray(new IModule[0]));

		CDIConfigBuilder configBuilder = new CDIConfigBuilder();
		for (Object module : modules) {
			configBuilder.addModule((IModule) module);
		}

		// configured.set(true);
		return configBuilder.build();
	}

	public <T> void bind(Binding<T> binding) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to add binding after injector configuration: " + binding);
		}
		explicitBindings.bindings.add(binding);
	}

	public <T> void bindInstance(Class<T> interfaceClass, T instance) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind instance after injector configuration: " + interfaceClass);
		}
		explicitBindings.instances.put(interfaceClass, instance);
	}

	public void bindScope(Class<? extends Annotation> annotation, IScope<?> scope) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind scope after injector configuration: " + annotation);
		}
		explicitBindings.scopes.put(annotation, scope);
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
		listener.onInstanceCreated(invocation.instance());
	}

	private IInstanceCreatedListener listener;

	public void setInstanceCreatedListener(IInstanceCreatedListener listener) {
		injector.bindListener(this);
		this.listener = listener;
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
	private class ExplicitBindingModule extends AbstractModule {
		final List<Binding<?>> bindings = new ArrayList<>();
		final Map<Class<?>, Object> instances = new HashMap<>();
		final Map<Class<? extends Annotation>, IScope<?>> scopes = new HashMap<>();

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected void configure() {
			bindings.forEach(binding -> {
				IBindingBuilder<?> builder = bind(binding.getInterfaceClass());
				if (binding.getImplementationClass() != null) {
					builder.to((Class) binding.getImplementationClass());
				}
				if (binding.getScopeClass() != null) {
					builder.in(binding.getScopeClass());
				}
			});

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

	/**
	 * Injector module initialized from managed classes collection. This specialized module traverses container managed classes,
	 * creating injector bindings accordingly managed class instance type and scope.
	 * 
	 * @author Iulian Rotaru
	 */
	private class ManagedClassesModule extends AbstractModule {
		private final Config config;
		private final Function<Class<?>, IManagedClass<?>> managedClassFactory;

		private final List<Binding<?>> containerBindings = new ArrayList<>();

		public ManagedClassesModule(Config config, Function<Class<?>, IManagedClass<?>> managedClassFactory) {
			this.config = config;
			this.managedClassFactory = managedClassFactory;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected void configure() {
			for (Config managedClassConfig : config.getChildren()) {

				final Class<?> implementationClass = managedClassConfig.getAttribute("class", Class.class);
				final Class<?> interfaceClass = managedClassConfig.getAttribute("interface", Class.class, implementationClass);
				final InstanceType instanceType = managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO);
				final InstanceScope instanceScope = managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION);
				final URI implementationURL = managedClassConfig.getAttribute("url", URI.class);

				log.debug("CDI register managed class |%s|.", managedClassConfig);
				IBindingBuilder bindingBuilder = bind(interfaceClass);

				switch (instanceType) {
				case POJO:
					bindingBuilder.to(implementationClass);
					containerBindings.add(new Binding(interfaceClass, implementationClass));
					break;

				case PROXY:
					bindingBuilder.to(implementationClass);
					bindingBuilder.toProvider(new ProxyProvider(interfaceClass, managedClassFactory, bindingBuilder.getProvider()));
					containerBindings.add(new Binding(interfaceClass, implementationClass));
					break;

				case REMOTE:
					bindingBuilder.on(implementationURL);
					break;

				case SERVICE:
					bindingBuilder.toProvider(new ServiceProvider<>(interfaceClass));
					break;

				default:
					throw new IllegalStateException("No provider for instance type " + instanceType);
				}

				if (instanceScope.isLOCAL()) {
					// local scope always creates a new instance
				} else if (instanceScope.isAPPLICATION()) {
					bindingBuilder.in(Singleton.class);
				} else if (instanceScope.isTHREAD()) {
					bindingBuilder.in(ThreadScoped.class);
				} else if (instanceScope.isSESSION()) {
					bindingBuilder.in(SessionScoped.class);
				} else {
					throw new IllegalStateException("No provider for instance scope " + instanceScope);
				}
			}
		}
	}
}
