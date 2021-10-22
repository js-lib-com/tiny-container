package js.tiny.container.cdi.service;

import javax.inject.Provider;

import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IBinding;
import js.tiny.container.cdi.Key;
import js.tiny.container.cdi.impl.ClassProvider;
import js.tiny.container.cdi.impl.ProxyProvider;
import js.tiny.container.cdi.impl.RemoteProvider;
import js.tiny.container.cdi.impl.ServiceProvider;
import js.tiny.container.cdi.impl.SingletonProvider;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

public class ContainerService implements IClassPostLoadedProcessor {
	private final CDI cdi;

	public ContainerService() {
		this.cdi = CDI.create();
	}

	@Override
	public Priority getPriority() {
		return Priority.REGISTER;
	}

	@Override
	public <T> void onClassPostLoaded(IManagedClass<T> managedClass) {
		final Class<T> type = managedClass.getInterfaceClass();

		final Key<T> key = Key.get(type);

		Provider<T> provider = null;

		final InstanceType instanceType = managedClass.getInstanceType();
		if (instanceType == InstanceType.POJO) {
			provider = new ClassProvider<>(cdi, type);
		} else if (instanceType == InstanceType.PROXY) {
			provider = new ProxyProvider<>(new ClassProvider<>(cdi, type));
		} else if (instanceType == InstanceType.REMOTE) {
			provider = new RemoteProvider<>(type, managedClass.getImplementationURL());
		} else if (instanceType == InstanceType.SERVICE) {
			provider = new ServiceProvider<>(type);
		} else {
			throw new IllegalStateException("No provider for instance type " + instanceType);
		}

		final InstanceScope instanceScope = managedClass.getInstanceScope();
		if (instanceScope == InstanceScope.LOCAL) {

		} else if (instanceScope == InstanceScope.APPLICATION) {
			provider = new SingletonProvider<>(provider);
		} else if (instanceScope == InstanceScope.THREAD) {

		} else if (instanceScope == InstanceScope.SESSION) {

		} else {
			throw new IllegalStateException("No provider for instance scope " + instanceScope);
		}

		cdi.bind(new Binding<>(key, provider));
	}

	private static class Binding<T> implements IBinding<T> {
		private final Key<T> key;
		private final Provider<T> provider;

		public Binding(Key<T> key, Provider<T> provider) {
			this.key = key;
			this.provider = provider;
		}

		@Override
		public Key<T> key() {
			return key;
		}

		@Override
		public Provider<? extends T> provider() {
			return provider;
		}
	}
}
