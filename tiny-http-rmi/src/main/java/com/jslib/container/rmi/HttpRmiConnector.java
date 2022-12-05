package com.jslib.container.rmi;

import com.jslib.api.json.Json;
import com.jslib.container.spi.IConnector;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IManagedClass;

import jakarta.ejb.Remote;

public class HttpRmiConnector implements IConnector {
	@Override
	public void configure(IContainer container) {
		container.bind(Json.class).service().build();
	}
	
	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		if (managedClass.scanAnnotation(Remote.class) != null) {
			return true;
		}
		return false;
	}
}
