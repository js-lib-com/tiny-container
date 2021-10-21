package js.tiny.container.cdi.impl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.inject.Provider;
import javax.inject.Singleton;

import js.tiny.container.cdi.IInjector;
import js.tiny.container.cdi.IProviderDecorator;
import js.tiny.container.cdi.IProviderService;
import js.tiny.container.cdi.IProviders;

public class Providers implements IProviders {
	static final Map<Class<? extends Annotation>, IProviderDecorator<?>> SCOPED_PROVIDERS = new HashMap<>();
	static {
		SCOPED_PROVIDERS.put(Singleton.class, SingletonProvider.factory());

		for (IProviderService<?> service : ServiceLoader.load(IProviderService.class)) {
			SCOPED_PROVIDERS.put(service.getScope(), service.getProviderFactory());
		}
	}

	private final IInjector injector;

	public Providers(IInjector injector) {
		this.injector = injector;
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type) {
		return new ClassProvider<>(injector, type);
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type, String uri) {
		return new RemoteProvider<T>(type, uri);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IProviderDecorator<T> getScopedProviderDecorator(Class<? extends Annotation> scope) {
		return (IProviderDecorator<T>) SCOPED_PROVIDERS.get(scope);
	}
}