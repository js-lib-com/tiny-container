package js.tiny.container.stub;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

public class ContainerSpiStub implements IContainer {
	@Override
	public <T> T getInstance(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getOptionalInstance(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public Iterable<IManagedClass<?>> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}
}
