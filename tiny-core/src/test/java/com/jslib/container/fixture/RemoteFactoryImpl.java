package com.jslib.container.fixture;

import com.jslib.rmi.InvocationPropertiesProvider;
import com.jslib.rmi.RemoteFactory;
import com.jslib.rmi.UnsupportedProtocolException;
import com.jslib.util.Classes;

public class RemoteFactoryImpl implements RemoteFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemoteInstance(Class<? super T> interfaceClass, String implementationURL, InvocationPropertiesProvider... propertiesProvider) throws UnsupportedProtocolException {
		return (T) Classes.newInstance(interfaceClass);
	}

}
