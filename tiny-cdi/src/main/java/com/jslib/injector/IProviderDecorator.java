package com.jslib.injector;

import javax.inject.Provider;

@Deprecated
public interface IProviderDecorator<T> {

	Provider<T> decorate(Provider<T> provider);

}
