package js.tiny.container.rmi;

import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;

public class HttpRmiConnector implements IConnector {
	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		if (managedClass.scanAnnotation(jakarta.ejb.Remote.class) != null) {
			return true;
		}
		if (managedClass.scanAnnotation(javax.ejb.Remote.class) != null) {
			return true;
		}
		return false;
	}
}
