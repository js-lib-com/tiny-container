package js.tiny.container.cdi;

import javax.inject.Provider;

import com.jslib.injector.IBinding;
import com.jslib.injector.Key;
import com.jslib.injector.ScopedProvider;

class ProxyBinding<T> implements IBinding<T> {
	public static <T> IBinding<T> create(IManagedLoader managedLoader, IBinding<T> binding) {
		Key<T> key = binding.key();
		Class<T> interfaceClass = key.type();

		Provider<T> provider = binding.provider();
		if (provider instanceof ScopedProvider) {
			ScopedProvider<T> scopedProvider = (ScopedProvider<T>) provider;
			scopedProvider.setProvider(new ProxyProvider<>(interfaceClass, managedLoader, scopedProvider.getProvisioningProvider()));
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