package js.tiny.container.stub;

import java.lang.annotation.Annotation;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

public class ManagedClassSpiStub<T> implements IManagedClass<T> {
	@Override
	public Class<T> getInterfaceClass() {
		throw new UnsupportedOperationException("getInterfaceClass()");
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
	}

	@Override
	public IManagedMethod getManagedMethod(String methodName) {
		throw new UnsupportedOperationException("getManagedMethod(String methodName)");
	}

	@Override
	public Class<? extends T> getImplementationClass() {
		throw new UnsupportedOperationException("getImplementationClass()");
	}

	@Override
	public IContainer getContainer() {
		throw new UnsupportedOperationException("getContainer()");
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		throw new UnsupportedOperationException("getAnnotation(Class<T> type)");
	}

	@Override
	public T getInstance() {
		throw new UnsupportedOperationException("getInstance()");
	}

	@Override
	public String getSignature() {
		// TODO Auto-generated method stub
		return null;
	}
}
