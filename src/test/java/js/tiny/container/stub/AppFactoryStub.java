package js.tiny.container.stub;

import js.tiny.container.core.AppFactory;

public class AppFactoryStub implements AppFactory {
	@Override
	public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
		throw new UnsupportedOperationException("getInstance(Class<? super T>, Object...)");
	}

	@Override
	public <T> T getInstance(String instanceName, Class<? super T> interfaceClass, Object... args) {
		throw new UnsupportedOperationException("getInstance(String, Class<? super T>, Object...)");
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T>, Object...)");
	}

	@Override
	public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getRemoteInstance(URL, Class<? super T>)");
	}

	@Override
	public <T> T loadService(Class<T> serviceInterface) {
		throw new UnsupportedOperationException("loadService(Class<T> serviceInterface)");
	}
}
