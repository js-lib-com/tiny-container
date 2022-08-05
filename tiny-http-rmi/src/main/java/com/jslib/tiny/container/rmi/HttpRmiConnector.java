package com.jslib.tiny.container.rmi;

import com.jslib.tiny.container.spi.IConnector;
import com.jslib.tiny.container.spi.IManagedClass;

import jakarta.ejb.Remote;

public class HttpRmiConnector implements IConnector {
	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		if (managedClass.scanAnnotation(Remote.class) != null) {
			return true;
		}
		return false;
	}
}
