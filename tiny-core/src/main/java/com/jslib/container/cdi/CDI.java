package com.jslib.container.cdi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IInstanceLifecycleListener;

import jakarta.inject.Provider;
import com.jslib.api.injector.AbstractModule;
import com.jslib.api.injector.IInjector;
import com.jslib.api.injector.IModule;
import com.jslib.api.injector.IProvisionInvocation;
import com.jslib.api.injector.IProvisionListener;
import com.jslib.api.injector.IScopeFactory;
import com.jslib.api.injector.ProvisionException;
import com.jslib.api.injector.ScopedProvider;
import com.jslib.lang.Config;
import com.jslib.util.Classes;
import com.jslib.util.Params;

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

	/** Explicit bindings, and instance and scope bindings collected from container. */
	private final StaticModule staticModule;

	private final BindingParametersModule parametersModule;

	/**
	 * Injector implementation used by this CDI instance. Injector is immutable; once configured is forbidden to alter its
	 * bindings.
	 */
	private final IInjector injector;

	/** Flag true only after CDI configuration complete. Used to assert CDI internal state consistency. */
	private final AtomicBoolean configured = new AtomicBoolean(false);

	private IManagedLoader managedLoader;
	private IInstanceLifecycleListener instanceCreatedListener;

	private CDI() {
		log.trace("CDI()");
		this.staticModule = new StaticModule();
		this.parametersModule = new BindingParametersModule();
		this.injector = IInjector.create();
	}

	public void setManagedLoader(IManagedLoader managedLoader) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to set managed class loader after injector configuration.");
		}
		this.managedLoader = managedLoader;
	}

	public void setInstanceCreatedListener(IInstanceLifecycleListener instanceCreatedListener) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to set instance created listener after injector configuration.");
		}
		injector.bindListener(this);
		this.instanceCreatedListener = instanceCreatedListener;
	}

	public <T> void bind(BindingParameters<T> binding) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to add binding after injector configuration: " + binding);
		}
		parametersModule.addBindingParameters(binding);
	}

	public <T> void bindInstance(Class<T> interfaceClass, T instance) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind instance after injector configuration: " + interfaceClass);
		}
		staticModule.instances.put(interfaceClass, instance);
	}

	public void bindScope(Class<? extends Annotation> annotation, IScopeFactory<?> scopeFactory) {
		if (configured.get()) {
			throw new IllegalStateException("Attempt to bind scope after injector configuration: " + annotation);
		}
		staticModule.scopeFactories.put(annotation, scopeFactory);
	}

	/**
	 * Create bindings for container managed classes and configure injector implementation.
	 * 
	 * @param classDescriptors container managed classes.
	 */
	public List<IClassBinding<?>> configure(Config config) {
		log.trace("configure(Config)");
		return configure(new ConfigModule(config));
	}

	public List<IClassBinding<?>> configure(IModule... modules) {
		log.trace("configure(IModule...)");

		ManagedModule managedModule = new ManagedModule(injector, managedLoader);
		managedModule.addModule(staticModule);
		managedModule.addModule(parametersModule);
		Arrays.stream(modules).forEach(managedModule::addModule);

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
	 * @throws ProvisionException if there is no injector bindings for requested interface class.
	 * @param <T> instance generic type.
	 */
	public <T> T getInstance(Class<T> interfaceClass) {
		return injector.getInstance(interfaceClass);
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
	 */
	public <T> T getScopeInstance(Class<? extends Annotation> scope, Class<T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");
		Provider<T> provider = injector.getProvider(interfaceClass);
		if (provider == null) {
			log.debug("No provider for |%s|.", interfaceClass);
			return null;
		}
		if (!(provider instanceof ScopedProvider)) {
			return null;
		}

		ScopedProvider<T> scopedProvider = (ScopedProvider<T>) provider;
		if (!scopedProvider.getScope().equals(scope)) {
			// scoped provider uses Jakarta packages but business code may still use deprecated Javax package
			// if this is the case rewrite the package
			String scopeClassName = scope.getCanonicalName();
			if (scopeClassName.startsWith("javax.")) {
				scope = Classes.forName("jakarta." + scopeClassName.substring("javax.".length()));
			}
			if (!scopedProvider.getScope().equals(scope)) {
				return null;
			}
		}
		return scopedProvider.getScopeInstance();
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
		final Map<Class<? extends Annotation>, IScopeFactory<?>> scopeFactories = new HashMap<>();

		@SuppressWarnings("unchecked")
		@Override
		protected void configure() {
			instances.forEach((interfaceClass, instance) -> {
				log.debug("CDI register instance for |%s|.", interfaceClass);
				bindInstance((Class<Object>) interfaceClass, instance);
			});

			scopeFactories.forEach((annotation, scope) -> {
				log.debug("CDI register scope |%s|.", annotation);
				injector.bindScopeFactory(annotation, scope);
			});
		}
	}
}
