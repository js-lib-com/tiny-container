package js.tiny.container.fixture;

import js.rmi.RemoteFactory;

public class RemoteFactoryProvider implements js.rmi.RemoteFactoryProvider {
	@Override
	public String[] getProtocols() {
		return new String[] { "http" };
	}

	@Override
	public RemoteFactory getRemoteFactory() {
		return new RemoteFactoryImpl();
	}
}
