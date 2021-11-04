package js.tiny.container.stub;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

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
	public <T> T getInstance(IManagedClass<T> managedClass) {
		throw new UnsupportedOperationException("getInstance(ManagedClassSPI managedClass)");
	}

	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public Iterable<IManagedClass<?>> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
	}
}
