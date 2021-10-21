package js.tiny.container.cdi.impl;

import javax.inject.Provider;

class RemoteProvider<T> implements Provider<T> {
	private final Class<T> type;
	private final String url;

	public RemoteProvider(Class<T> type, String url) {
		this.type = type;
		this.url = url;
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}
}