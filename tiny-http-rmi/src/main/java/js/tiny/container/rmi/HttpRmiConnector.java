package js.tiny.container.rmi;

import jakarta.ejb.Remote;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;

public class HttpRmiConnector implements IConnector {
	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		if (managedClass.scanAnnotation(Remote.class) != null) {
			return true;
		}
		return false;
	}
}
