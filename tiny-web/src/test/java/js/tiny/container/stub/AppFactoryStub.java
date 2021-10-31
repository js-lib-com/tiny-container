package js.tiny.container.stub;

import js.tiny.container.spi.IFactory;

public class AppFactoryStub implements IFactory {
	@Override
	public <T> T getInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(Class<? super T>)");
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T>)");
	}
}
