package js.tiny.container.cdi.service;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Scope;
import javax.inject.Singleton;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.IBinding;
import js.tiny.container.cdi.IInjector;
import js.tiny.container.cdi.IModule;
import js.tiny.container.cdi.IProvisionInvocation;
import js.tiny.container.cdi.IProvisionListener;
import js.tiny.container.cdi.IScope;
import js.tiny.container.cdi.Key;
import js.tiny.container.cdi.Names;
import js.tiny.container.cdi.ScopedProvider;
import js.tiny.container.cdi.ThreadScoped;
import js.tiny.container.cdi.impl.SingletonScope;
import js.tiny.container.cdi.impl.ThreadScope;
import js.tiny.container.spi.IManagedClass;

public class CDI implements IInjector {
	private static final Log log = LogFactory.getLog(CDI.class);

	private static CDI instance;
	private static final Object mutex = new Object();

	public static CDI create(IModule... modules) {
		log.trace("create(Module...)");
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new CDI();
					instance.configure(modules);
				}
			}
		}
		return instance;
	}

	// --------------------------------------------------------------------------------------------

	private final Map<Class<? extends Annotation>, IScope> scopes;
	private final Map<Key<?>, Provider<?>> bindings;

	private CDI() {
		log.trace("CDI()");

		this.scopes = new HashMap<>();
		bind(Singleton.class, new SingletonScope());
		bind(ThreadScoped.class, new ThreadScope());

		this.bindings = new HashMap<>();
	}

	private void configure(IModule... modules) {
		log.trace("configure(Module...)");
		for (IModule module : modules) {
			module.configure(this).bindings().forEach(this::bind);
		}
	}

	public void bind(IBinding<?> binding) {
		log.debug("Bind |%s| to provider |%s|.", binding.key(), binding.provider());
		bindings.put(binding.key(), binding.provider());
	}

	public void bind(Class<? extends Annotation> annotation, IScope scope) {
		if (!annotation.isAnnotationPresent(Scope.class)) {
			throw new IllegalArgumentException("Not a scope annotation: " + annotation);
		}
		log.debug("Register |%s| to scope |%s|.", annotation, scope);
		scopes.put(annotation, scope);
	}

	public IScope getScope(Class<? extends Annotation> annotation) {
		return scopes.get(annotation);
	}

	@Override
	public <T> T getInstance(Class<T> type, Annotation qualifier) {
		Key<T> key = Key.get(type, qualifier);
		@SuppressWarnings("unchecked")
		Provider<T> provider = (Provider<T>) bindings.get(key);
		if (provider == null) {
			throw new IllegalStateException("No provider for " + key);
		}
		return provider.get();
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		return getInstance(type, (Annotation) null);
	}

	public <T> T getScopeInstance(Class<T> type) {
		Key<T> key = Key.get(type);
		@SuppressWarnings("unchecked")
		Provider<T> provider = (Provider<T>) bindings.get(key);
		if (provider == null) {
			throw new IllegalStateException("No provider for " + key);
		}
		if(!(provider instanceof ScopedProvider)) {
			throw new IllegalStateException("Not a scoped provider " + provider);
		}
		ScopedProvider<T> scopedProvider = (ScopedProvider<T>)provider;
		return scopedProvider.getScopeInstance();
	}

	private final Map<Provider<?>, IManagedClass<?>> providedClasses = new HashMap<>();

	public <T> void bindProvidedClass(Provider<T> provider, IManagedClass<T> managedClass) {
		providedClasses.put(provider, managedClass);
	}

	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> type, IInstancePostConstructionListener<T> instanceListener) {
		IProvisionListener<T> provisionListener = invocation -> {
			instanceListener.onInstancePostConstruction((IManagedClass<T>) providedClasses.get(invocation.provider()), (T) invocation.instance());
		};
		bindListener(provisionListener);
		try {
			return getInstance(type, (Annotation) null);
		} finally {
			unbindListener(provisionListener);
		}
	}

	private final Set<IProvisionListener<?>> provisionListeners = Collections.synchronizedSet(new HashSet<>());

	public <T> void bindListener(IProvisionListener<T> listener) {
		provisionListeners.add(listener);
	}

	private <T> void unbindListener(IProvisionListener<T> listener) {
		provisionListeners.remove(listener);
	}

	@SuppressWarnings("unchecked")
	public <T> void fireEvent(IProvisionInvocation<T> event) {
		provisionListeners.forEach(listener -> {
			((IProvisionListener<T>)listener).onProvision(event);
		});
	}

	@Override
	public <T> T getInstance(Class<T> type, String name) {
		return getInstance(type, Names.named(name));
	}
}
