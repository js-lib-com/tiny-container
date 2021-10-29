package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Provider;
import javax.inject.Singleton;

import com.jslib.injector.IBindingBuilder;
import com.jslib.injector.IInjector;
import com.jslib.injector.IProvisionListener;
import com.jslib.injector.IScope;
import com.jslib.injector.Key;
import com.jslib.injector.ThreadScoped;
import com.jslib.injector.impl.AbstractModule;
import com.jslib.injector.impl.Injector;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
import js.util.Params;

/**
 * Connector for injection implementation.
 * 
 * @author Iulian Rotaru
 */
public class CDI {
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

	/**
	 * Cache provisioning providers used to create instances for related managed classes. Remember that a provisioning provider
	 * is one that actually creates new instances; this is in contrast with a scope provider that uses internal cache and can
	 * reuse an instance.
	 * 
	 * This cache is updated by managed classes module when create injector bindings. It is used by instance post construct
	 * listener to retrieve managed class while knowing the provider used by injector.
	 */
	private final Map<Provider<?>, IManagedClass<?>> providedClasses;

	/** Flag true only after CDI configuration complete. Used to assert CDI internal state consistency. */
	private final AtomicBoolean configured = new AtomicBoolean(false);

	private CDI() {
		log.trace("CDI()");
		explicitBindings = new ExplicitBindingModule();
		injector = new Injector();
		providedClasses = new HashMap<>();
	}

	/**
	 * Create bindings for container managed classes and configure injector implementation.
	 * 
	 * @param managedClasses container managed classes.
	 */
	public void configure(Collection<IManagedClass<?>> managedClasses) {
		log.trace("configure(Collection<IManagedClass<?>> managedClasses");
		injector.configure(explicitBindings, new ManagedClassesModule(managedClasses));
		configured.set(true);
	}

	public <T> void bindInstance(Class<T> interfaceClass, T instance) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind instance after injector configuration: " + interfaceClass);
		}
		explicitBindings.instances.put(interfaceClass, instance);
	}

	public void bindScope(Class<? extends Annotation> annotation, IScope scope) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind scope after injector configuration: " + annotation);
		}
		explicitBindings.scopes.put(annotation, scope);
	}

	/**
	 * Retrieve an instance, be it newly created or reused from a scope cache. Instance to retrieve is identified by the
	 * interface class. Here the term interface class is used in a broad sense. It is not necessary to be a Java interface; it
	 * can be an abstract or even a concrete one. The point is that this interface class identifies the instance at injector
	 * bindings level.
	 * 
	 * Depending on injector binding and current context a new instance can be created or one can be reused from a scope cache.
	 * Instance post construction listener is invoked only if a new instance is created.
	 * 
	 * @param interfaceClass interface class used to identify the instance.
	 * @param instanceListener event listener for instance post construction.
	 * @return instance, newly created or reused from scope.
	 * @param <T> instance generic type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> interfaceClass, IInstancePostConstructionListener<T> instanceListener) {
		Params.notNull(interfaceClass, "Interface class");
		Params.notNull(instanceListener, "Instance listener");

		if (!configured.get()) {
			throw new IllegalStateException("Attempt to retrieve instance before injector configuration: " + interfaceClass);
		}

		IProvisionListener<T> provisionListener = invocation -> {
			instanceListener.onInstancePostConstruction((IManagedClass<T>) providedClasses.get(invocation.provider()), (T) invocation.instance());
		};
		injector.bindListener(provisionListener);
		try {
			return injector.getInstance(Key.get(interfaceClass));
		} finally {
			injector.unbindListener(provisionListener);
		}
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
		final Map<Class<?>, Object> instances = new HashMap<>();
		final Map<Class<? extends Annotation>, IScope> scopes = new HashMap<>();

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

	/**
	 * Injector module initialized from managed classes collection. This specialized module traverses container managed classes,
	 * creating injector bindings accordingly managed class instance type and scope.
	 * 
	 * @author Iulian Rotaru
	 */
	private class ManagedClassesModule extends AbstractModule {
		private final Collection<IManagedClass<?>> managedClasses;

		public ManagedClassesModule(Collection<IManagedClass<?>> managedClasses) {
			this.managedClasses = managedClasses;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected void configure() {
			managedClasses.forEach(managedClass -> {
				log.debug("CDI register managed class |%s|.", managedClass);

				IBindingBuilder bindingBuilder = bind(managedClass.getInterfaceClass());

				final InstanceType instanceType = managedClass.getInstanceType();
				if (instanceType.isPOJO()) {
					bindingBuilder.to(managedClass.getImplementationClass());
					providedClasses.put(bindingBuilder.getProvider(), managedClass);
				} else if (instanceType.isPROXY()) {
					bindingBuilder.to(managedClass.getImplementationClass());
					providedClasses.put(bindingBuilder.getProvider(), managedClass);
					bindingBuilder.toProvider(new ProxyProvider(managedClass, bindingBuilder.getProvider()));
				} else if (instanceType.isREMOTE()) {
					bindingBuilder.on(managedClass.getImplementationURL());
				} else if (instanceType.isSERVICE()) {
					bindingBuilder.toProvider(new ServiceProvider<>(injector, managedClass.getInterfaceClass()));
					providedClasses.put(bindingBuilder.getProvider(), managedClass);
				} else {
					throw new IllegalStateException("No provider for instance type " + instanceType);
				}

				final InstanceScope instanceScope = managedClass.getInstanceScope();
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
			});
		}
	}
}
