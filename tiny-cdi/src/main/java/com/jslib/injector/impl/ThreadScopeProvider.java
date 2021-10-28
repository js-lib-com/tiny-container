package com.jslib.injector.impl;

import javax.inject.Provider;

import com.jslib.injector.ScopedProvider;

class ThreadScopeProvider<T> extends ScopedProvider<T> {
	private final ThreadLocal<T> tls = new InheritableThreadLocal<>();

	public ThreadScopeProvider(Provider<T> provider) {
		super(provider);
	}

	@Override
	public T getScopeInstance() {
		return tls.get();
	}

	@Override
	public T get() {
		T instance = tls.get();
		if (instance == null) {
			instance = provider.get();
			tls.set(instance);
		}
		return instance;
	}

	@Override
	public String toString() {
		return provider.toString() + ":THREAD";
	}
}