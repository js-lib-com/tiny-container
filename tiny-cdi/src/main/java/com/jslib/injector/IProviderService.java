package com.jslib.injector;

import java.lang.annotation.Annotation;

@Deprecated
public interface IProviderService<T> {

	Class<? extends Annotation> getScope();

	IProviderDecorator<T> getProviderFactory();

}
