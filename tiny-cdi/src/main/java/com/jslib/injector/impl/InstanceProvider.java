package com.jslib.injector.impl;

import javax.inject.Provider;

class InstanceProvider<T> implements Provider<T> {
	private final T instance;

	public InstanceProvider(T instance) {
		this.instance = instance;
	}

	@Override
	public T get() {
		return instance;
	}
}
