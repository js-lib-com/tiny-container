package js.tiny.container.fixture;

import js.rmi.UnsupportedProtocolException;
import js.util.Classes;

public class RemoteFactoryImpl implements js.rmi.RemoteFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemoteInstance(Class<? super T> interfaceClass, String implementationURL) throws UnsupportedProtocolException {
		return (T) Classes.newInstance(interfaceClass);
	}

}
