package js.tiny.container.stub;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

public class ContainerSpiStub implements IContainer {
	@Override
	public <T> T getInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getInstance(String instanceName, Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(String instanceName, Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getRemoteInstance(URL implementationURL, Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T loadService(Class<T> serviceInterface) {
		throw new UnsupportedOperationException("loadService(Class<T> serviceInterface)");
	}

	@Override
	public <T> T getInstance(IManagedClass managedClass) {
		throw new UnsupportedOperationException("getInstance(ManagedClassSPI managedClass)");
	}

	@Override
	public boolean isManagedClass(Class<?> interfaceClass) {
		throw new UnsupportedOperationException("isManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public IManagedClass getManagedClass(Class<?> interfaceClass) {
		throw new UnsupportedOperationException("getManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public Iterable<IManagedClass> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
	}
}
