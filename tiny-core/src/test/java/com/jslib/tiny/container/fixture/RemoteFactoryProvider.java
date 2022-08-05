package com.jslib.tiny.container.fixture;

import com.jslib.rmi.RemoteFactory;

public class RemoteFactoryProvider implements com.jslib.rmi.RemoteFactoryProvider {
	@Override
	public String[] getProtocols() {
		return new String[] { "http" };
	}

	@Override
	public RemoteFactory getRemoteFactory() {
		return new RemoteFactoryImpl();
	}
}
