package js.tiny.container.cdi.impl;

import javax.inject.Provider;

public class ProxyProvider<T> implements Provider<T> {
	private final Provider<T> provider;
	
	public ProxyProvider(Provider<T> provider) {
		this.provider = provider;
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}
}
