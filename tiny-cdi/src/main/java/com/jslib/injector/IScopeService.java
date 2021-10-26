package com.jslib.injector;

import java.lang.annotation.Annotation;

@Deprecated
public interface IScopeService {

	Class<? extends Annotation> getAnnotation();

	IScope getScope();

}
