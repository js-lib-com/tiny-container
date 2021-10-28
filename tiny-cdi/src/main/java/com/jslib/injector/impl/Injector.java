package com.jslib.injector.impl;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Scope;
import javax.inject.Singleton;

import com.jslib.injector.IBindingBuilder;
import com.jslib.injector.IInjector;
import com.jslib.injector.IModule;
import com.jslib.injector.IProvisionInvocation;
import com.jslib.injector.IProvisionListener;
import com.jslib.injector.IScope;
import com.jslib.injector.Key;
import com.jslib.injector.Names;
import com.jslib.injector.ScopedProvider;
import com.jslib.injector.ThreadScoped;

import js.log.Log;
import js.log.LogFactory;

public class Injector implements IInjector {
	private static final Log log = LogFactory.getLog(Injector.class);

	public static IInjector create(IModule... modules) {
		IInjector injector = new Injector();
		injector.configure(modules);
		return injector;
	}

	private final Map<Class<? extends Annotation>, IScope> scopes = new HashMap<>();

	private final Map<Key<?>, Provider<?>> bindings = new HashMap<>();

	private final Set<IProvisionListener<?>> provisionListeners = Collections.synchronizedSet(new HashSet<>());

	public Injector() {
		bindScope(Singleton.class, new SingletonScope());
		bindScope(ThreadScoped.class, new ThreadScope());
	}

	@Override
	public void configure(IModule... modules) {
		log.trace("configure(Module...)");
		for (IModule module : modules) {
			module.configure(this).bindings().forEach(binding -> {
				log.debug("Bind |%s| to provider |%s|.", binding.key(), binding.provider());
				bindings.put(binding.key(), binding.provider());
			});
		}
	}

	@Override
	public <T> IBindingBuilder<T> getBindingBuilder(Class<T> type) {
		return new BindingBuilder<>(this, new Binding<>(type));
	}

	@Override
	public <T, I extends T> void bindInstance(Key<T> key, I instance) {
		log.debug("Bind |%s| to instance |%s|.", key, instance);
		bindings.put(key, new InstanceProvider<>(instance));
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		return getInstance(type, (Annotation) null);
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
	public <T> T getInstance(Class<T> type, String name) {
		return getInstance(type, Names.named(name));
	}

	@Override
	public <T> T getScopeInstance(Class<T> type) {
		Key<T> key = Key.get(type);
		@SuppressWarnings("unchecked")
		Provider<T> provider = (Provider<T>) bindings.get(key);
		if (provider == null) {
			throw new IllegalStateException("No provider for " + key);
		}
		if (!(provider instanceof ScopedProvider)) {
			throw new IllegalStateException("Not a scoped provider " + provider);
		}
		ScopedProvider<T> scopedProvider = (ScopedProvider<T>) provider;
		return scopedProvider.getScopeInstance();
	}

	@Override
	public <T> void bindListener(IProvisionListener<T> provisionListener) {
		provisionListeners.add(provisionListener);
	}

	@Override
	public <T> void unbindListener(IProvisionListener<T> provisionListener) {
		provisionListeners.remove(provisionListener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> void fireEvent(IProvisionInvocation<T> provisionInvocation) {
		provisionListeners.forEach(listener -> {
			((IProvisionListener<T>) listener).onProvision(provisionInvocation);
		});
	}

	@Override
	public void bindScope(Class<? extends Annotation> annotation, IScope scope) {
		if (!annotation.isAnnotationPresent(Scope.class)) {
			throw new IllegalArgumentException("Not a scope annotation: " + annotation);
		}
		log.debug("Register |%s| to scope |%s|.", annotation, scope);
		scopes.put(annotation, scope);
	}

	@Override
	public IScope getScope(Class<? extends Annotation> annotation) {
		return scopes.get(annotation);
	}
}
