package js.tiny.container.cdi.impl;

import javax.inject.Provider;

public class ServiceProvider<T> implements Provider<T> {
	private final Class<T> type;
	
	public ServiceProvider(Class<T> type) {
		this.type = type;
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}

}
