package js.tiny.container.cdi;

import jakarta.inject.Provider;
import js.injector.IBinding;
import js.injector.Key;
import js.injector.ScopedProvider;

/**
 * Injector binding implementation for proxy provider. This binding provider uses {@link ProxyProvider} to wrap given
 * provisioning binding. If provisioning binding is a scoped binding takes care to keep it outermost, that is, insert proxy
 * provider inside scoped provider.
 * 
 * This class is designed specifically for {@link ProxyProvider} and is used only for embedded containers when proxy processing
 * is enabled.
 * 
 * @author Iulian Rotaru
 */
class ProxyBinding<T> implements IBinding<T> {
	public static <T> IBinding<T> create(IManagedLoader managedLoader, IBinding<T> provisioningBinding) {
		Key<T> key = provisioningBinding.key();
		Class<T> interfaceClass = key.type();

		Provider<T> provider = provisioningBinding.provider();
		if (provider instanceof ScopedProvider) {
			ScopedProvider<T> scopedProvider = (ScopedProvider<T>) provider;
			scopedProvider.setProvisioningProvider(new ProxyProvider<>(interfaceClass, managedLoader, scopedProvider.getProvisioningProvider()));
			provider = scopedProvider;
		} else {
			provider = new ProxyProvider<>(interfaceClass, managedLoader, provider);
		}

		return new ProxyBinding<>(key, provider);
	}

	private final Key<T> key;
	private final Provider<T> provider;

	private ProxyBinding(Key<T> key, Provider<T> provider) {
		super();
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