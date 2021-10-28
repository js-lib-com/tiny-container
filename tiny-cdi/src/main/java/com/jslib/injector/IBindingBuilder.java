package com.jslib.injector;

import java.lang.annotation.Annotation;
import java.net.URI;

import javax.inject.Provider;

/**
 * Chained builder used by module configuration to collect binding parameters.
 * 
 * @author Iulian Rotaru
 */
public interface IBindingBuilder<T> {

	IBindingBuilder<T> annotatedWith(Annotation qualifier);

	IBindingBuilder<T> annotatedWith(Class<? extends Annotation> qualifierType);

	IBindingBuilder<T> with(Annotation qualifier);

	IBindingBuilder<T> with(Class<? extends Annotation> qualifierType);

	IBindingBuilder<T> named(String name);

	IBindingBuilder<T> to(Class<? extends T> type);

	IBindingBuilder<T> toInstance(T instance);

	IBindingBuilder<T> instance(T instance);

	IBindingBuilder<T> toProvider(Provider<T> provider);

	IBindingBuilder<T> provider(Provider<T> provider);

	IBindingBuilder<T> on(URI implementationURL);

	IBindingBuilder<T> on(String implementationURL);

	IBindingBuilder<T> in(Class<? extends Annotation> scopeType);

	Provider<T> getProvider();

	IBinding<T> getBinding();
}
