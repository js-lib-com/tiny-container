package js.tiny.container.cdi;

import com.jslib.injector.ITypedProvider;
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
class ServiceProvider<T> implements ITypedProvider<T> {
	private final Class<T> type;

	public ServiceProvider(Class<T> type) {
		this.type = type;
	}

	@Override
	public Class<? extends T> type() {
		return type;
	}

	/**
	 * Creates a new instance using Java services loader and fires {@link IProvisionInvocation<T>} event.
	 * 
	 * @throws ProvisionException if Java services loader fails to load this type.
	 */
	@Override
	public T get() {
		try {
			return Classes.loadService(type);
		} catch (NoProviderException e) {
			throw new ProvisionException(e);
		}
	}

	@Override
	public String toString() {
		return type.getCanonicalName() + ":SERVICE";
	}
}
