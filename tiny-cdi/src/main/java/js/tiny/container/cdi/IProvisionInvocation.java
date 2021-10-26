package js.tiny.container.cdi;

import javax.inject.Provider;

public interface IProvisionInvocation<T> {

	static <T> IProvisionInvocation<T> event(final Provider<T> provider, final T instance) {
		return new IProvisionInvocation<T>() {
			@Override
			public Provider<T> provider() {
				return provider;
			}

			@Override
			public T instance() {
				return instance;
			}
		};
	}

	Provider<T> provider();

	T instance();
}
