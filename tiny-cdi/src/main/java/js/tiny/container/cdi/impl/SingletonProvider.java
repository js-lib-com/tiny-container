package js.tiny.container.cdi.impl;

import javax.inject.Provider;

import js.tiny.container.cdi.IProviderDecorator;

public class SingletonProvider<T> extends ScopedProvider<T> {
	public SingletonProvider(Provider<T> provider) {
		super(provider);
	}

	@Override
	public T get() {
		// if cache return it

		return super.get();
	}

	public static <T> IProviderDecorator<T> factory() {
		return provider -> new SingletonProvider<>(provider);
	}
}