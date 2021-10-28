package js.tiny.container.cdi;

import javax.inject.Provider;

import com.jslib.injector.IInjector;
import com.jslib.injector.IProvisionInvocation;

import js.util.Classes;

public class ServiceProvider<T> implements Provider<T> {
	private final IInjector injector;
	private final Class<T> type;

	public ServiceProvider(IInjector injector, Class<T> type) {
		this.injector = injector;
		this.type = type;
	}

	@Override
	public T get() {
		T instance = Classes.loadService(type);
		injector.fireEvent(IProvisionInvocation.create(this, instance));
		return instance;
	}

	@Override
	public String toString() {
		return type.getCanonicalName() + ":SERVICE";
	}
}
