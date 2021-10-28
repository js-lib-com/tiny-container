package com.jslib.injector.impl;

import javax.inject.Provider;

import com.jslib.injector.IScope;

class ThreadScope implements IScope {

	@Override
	public <T> Provider<T> scope(Provider<T> provider) {
		return new ThreadScopeProvider<>(provider);
	}

}
