package js.tiny.container.cdi;

import javax.inject.Provider;

import com.jslib.injector.IInjector;
import com.jslib.injector.IProvisionInvocation;
import com.jslib.injector.ProvisionException;

import js.lang.NoProviderException;
import js.util.Classes;

/**
 * Load instance using Java services loader. This provider is a provisioning one that creates a new instance every time
 * {@link #get()} is invoked. Throws provisioning exception if Java services loader fails to load requested type.
 * 
 * This provider fires {@link IProvisionInvocation<T>} event after instance successfully loaded.
 * 
 * @param <T> instance generic type.
 * @author Iulian Rotaru
 */
class ServiceProvider<T> implements Provider<T> {
	private final IInjector injector;
	private final Class<T> type;

	public ServiceProvider(IInjector injector, Class<T> type) {
		this.injector = injector;
		this.type = type;
	}

	/**
	 * Creates a new instance using Java services loader and fires {@link IProvisionInvocation<T>} event.
	 * 
	 * @throws ProvisionException if Java services loader fails to load this type.
	 */
	@Override
	public T get() {
		try {
			T instance = Classes.loadService(type);
			injector.fireEvent(IProvisionInvocation.create(this, instance));
			return instance;
		} catch (NoProviderException e) {
			throw new ProvisionException(e);
		}
	}

	@Override
	public String toString() {
		return type.getCanonicalName() + ":SERVICE";
	}
}
