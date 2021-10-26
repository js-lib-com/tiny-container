package js.tiny.container.cdi.service;

import javax.inject.Provider;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.IBinding;
import js.tiny.container.cdi.IScope;
import js.tiny.container.cdi.Key;
import js.tiny.container.cdi.SessionScoped;
import js.tiny.container.cdi.impl.ClassProvider;
import js.tiny.container.cdi.impl.RemoteProvider;
import js.tiny.container.cdi.impl.SingletonScopeProvider;
import js.tiny.container.cdi.impl.ThreadScopeProvider;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

public class ContainerService implements IClassPostLoadedProcessor {
	private static final Log log = LogFactory.getLog(ContainerService.class);

	private final CDI cdi;

	public ContainerService() {
		log.trace("ContainerService()");
		this.cdi = CDI.create();
	}

	@Override
	public Priority getPriority() {
		return Priority.REGISTER;
	}

	@Override
	public <T> void onClassPostLoaded(IManagedClass<T> managedClass) {
		log.debug("CDI register managed class |%s|.", managedClass);

		final Key<T> key = Key.get(managedClass.getInterfaceClass());
		Provider<T> provider = null;

		final InstanceType instanceType = managedClass.getInstanceType();
		if (instanceType.isPOJO()) {
			provider = new ClassProvider<>(cdi, managedClass.getImplementationClass());
			// at this point provider is a provisioning provider, that is, one that create new instances
			// add it to provided classes cache before provider scope decoration
			cdi.bindProvidedClass(provider, managedClass);
		} else if (instanceType.isPROXY()) {
			provider = new ClassProvider<>(cdi, managedClass.getImplementationClass());
			// at this point provider is a provisioning provider, that is, one that create new instances
			// add it to provided classes cache before provider scope decoration
			cdi.bindProvidedClass(provider, managedClass);
			provider = new ProxyProvider<>(managedClass, provider);
		} else if (instanceType.isREMOTE()) {
			provider = new RemoteProvider<>(managedClass.getInterfaceClass(), managedClass.getImplementationURL());
		} else if (instanceType.isSERVICE()) {
			provider = new ServiceProvider<>(cdi, managedClass.getInterfaceClass());
			// at this point provider is a provisioning provider, that is, one that create new instances
			// add it to provided classes cache before provider scope decoration
			cdi.bindProvidedClass(provider, managedClass);
		} else {
			throw new IllegalStateException("No provider for instance type " + instanceType);
		}

		final InstanceScope instanceScope = managedClass.getInstanceScope();
		if (instanceScope.isLOCAL()) {
			// local scope always creates a new instance
		} else if (instanceScope.isAPPLICATION()) {
			provider = new SingletonScopeProvider<>(provider);
		} else if (instanceScope.isTHREAD()) {
			provider = new ThreadScopeProvider<>(provider);
		} else if (instanceScope.isSESSION()) {
			IScope scope = cdi.getScope(SessionScoped.class);
			if (scope != null) {
				provider = scope.scope(provider);
			}
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
		public Provider<T> provider() {
			return provider;
		}
	}
}
