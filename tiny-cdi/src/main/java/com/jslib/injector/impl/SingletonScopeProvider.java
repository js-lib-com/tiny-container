package com.jslib.injector.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import com.jslib.injector.ScopedProvider;

class SingletonScopeProvider<T> extends ScopedProvider<T> {
	private static final Map<Provider<?>, Object> cache = new HashMap<>();

	public SingletonScopeProvider(Provider<T> provider) {
		super(provider);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getScopeInstance() {
		return (T) cache.get(provider);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		Object instance = cache.get(provider);
		if (instance == null) {
			synchronized (this) {
				instance = cache.get(provider);
				if (instance == null) {
					instance = provider.get();
					cache.put(provider, instance);
				}
			}
		}
		return (T) instance;
	}

	@Override
	public String toString() {
		return provider.toString() + ":SINGLETON";
	}
}