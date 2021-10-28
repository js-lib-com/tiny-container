package com.jslib.injector.impl;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.inject.Provider;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.rmi.RemoteFactory;
import js.rmi.RemoteFactoryProvider;
import js.rmi.UnsupportedProtocolException;
import js.util.Strings;

class RemoteProvider<T> implements Provider<T> {
	private static final Log log = LogFactory.getLog(RemoteProvider.class);

	private static final Map<String, RemoteFactory> remoteFactories = new HashMap<>();
	static {
		for (RemoteFactoryProvider provider : ServiceLoader.load(RemoteFactoryProvider.class)) {
			for (String protocol : provider.getProtocols()) {
				if (remoteFactories.put(protocol, provider.getRemoteFactory()) != null) {
					throw new BugError("Invalid runtime environment. Remote factory protocol override |%s|. See remote factory provider |%s|.", protocol, provider);
				}
			}
		}
	}

	private final RemoteFactory remoteFactory;
	private final Class<T> type;
	private final String implementationURL;

	public RemoteProvider(Class<T> type, String implementationURL) {
		log.trace("RemoteProvider(Class<T>, String)");
		
		String protocol = Strings.getProtocol(implementationURL);
		if (protocol == null) {
			throw new UnsupportedProtocolException(new MalformedURLException("Protocol not found on " + implementationURL));
		}
		this.remoteFactory = remoteFactories.get(protocol);

		this.type = type;
		this.implementationURL = implementationURL;
	}

	@Override
	public T get() {
		return remoteFactory.getRemoteInstance(type, implementationURL);
	}
}