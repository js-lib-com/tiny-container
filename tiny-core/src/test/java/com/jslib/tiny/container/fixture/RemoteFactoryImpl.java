package com.jslib.tiny.container.fixture;

import com.jslib.rmi.UnsupportedProtocolException;
import com.jslib.util.Classes;

public class RemoteFactoryImpl implements com.jslib.rmi.RemoteFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemoteInstance(Class<? super T> interfaceClass, String implementationURL) throws UnsupportedProtocolException {
		return (T) Classes.newInstance(interfaceClass);
	}

}
