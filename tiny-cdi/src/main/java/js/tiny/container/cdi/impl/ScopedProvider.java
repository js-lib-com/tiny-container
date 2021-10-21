package js.tiny.container.cdi.impl;

import javax.inject.Provider;

public abstract class ScopedProvider<T> implements Provider<T> {
	private final Provider<T> provider;

	protected ScopedProvider(Provider<T> provider) {
		this.provider = provider;
	}

	@Override
	public T get() {
		return provider.get();
	}
}