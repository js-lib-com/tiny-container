package js.tiny.container.cdi.service;

import js.tiny.container.spi.IManagedClass;

public interface IInstancePostConstructionListener<T> {

	void onInstancePostConstruction(IManagedClass<T> managedClass, T instance);

}
