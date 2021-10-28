package com.jslib.injector;

import javax.inject.Provider;

public abstract class ScopedProvider<T> implements Provider<T> {
	protected final Provider<T> provider;

	protected ScopedProvider(Provider<T> provider) {
		this.provider = provider;
	}

	public abstract T getScopeInstance();
}