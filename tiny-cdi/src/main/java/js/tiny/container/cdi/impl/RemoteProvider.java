package js.tiny.container.cdi.impl;

import javax.inject.Provider;

public class RemoteProvider<T> implements Provider<T> {
	private final Class<T> type;
	private final String hostURI;

	public RemoteProvider(Class<T> type, String hostURI) {
		this.type = type;
		this.hostURI = hostURI;
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}
}