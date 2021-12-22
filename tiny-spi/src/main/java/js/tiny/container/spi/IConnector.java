package js.tiny.container.spi;

public interface IConnector extends IContainerService {

	default <T> boolean bind(IManagedClass<T> managedClass) {
		return false;
	}

}
