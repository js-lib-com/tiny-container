package js.tiny.container.stub;

import java.util.List;

import js.injector.IBindingBuilder;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

public class ContainerSpiStub implements IContainer {
	@Override
	public <T> IBindingBuilder<T> bind(Class<T> interfaceClass) {
		// TODO Auto-generated method stub
		return null;
	}

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
	public List<IManagedClass<?>> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}
}
