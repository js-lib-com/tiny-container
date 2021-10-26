package js.tiny.container.cdi.impl;

import javax.inject.Provider;

import js.tiny.container.cdi.IScope;

public class SingletonScope implements IScope {

	@Override
	public <T> Provider<T> scope(Provider<T> provider) {
		return new SingletonScopeProvider<>(provider);
	}

}
