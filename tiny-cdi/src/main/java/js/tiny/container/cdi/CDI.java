package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.impl.Providers;

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

	private final IProviders providers;
	private final Map<Key<?>, Provider<?>> bindings;

	private CDI() {
		log.trace("CDI()");
		this.providers = new Providers(this);
		this.bindings = new HashMap<>();
	}

	private void configure(IModule... modules) {
		log.trace("configure(Module...)");
		for (IModule module : modules) {
			module.setScopeProviders(providers);
			module.configure();
			module.getBindings().forEach(this::bind);
		}
	}

	public void bind(IBinding<?> binding) {
		log.debug("Register |%s| to provider |%s|.", binding.key(), binding.provider());
		bindings.put(binding.key(), binding.provider());
	}

	public IProviders getProviders() {
		return providers;
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

	@Override
	public <T> T getInstance(Class<T> type, String name) {
		return getInstance(type, Names.named(name));
	}
}
