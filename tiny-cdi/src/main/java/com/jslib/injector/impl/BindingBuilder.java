package com.jslib.injector.impl;

import java.lang.annotation.Annotation;
import java.net.URI;

import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.jslib.injector.IBindingBuilder;
import com.jslib.injector.IScope;
import com.jslib.injector.Names;

import js.tiny.container.cdi.CDI;

class BindingBuilder<T> implements IBindingBuilder<T> {
	private final CDI cdi;
	private final Binding<T> binding;

	public BindingBuilder(CDI cdi, Binding<T> binding) {
		this.cdi = cdi;
		this.binding = binding;
	}

	@Override
	public IBindingBuilder<T> to(Class<? extends T> type) {
		binding.setProvider(new ClassProvider<>(cdi, type));
		return this;
	}

	@Override
	public IBindingBuilder<T> annotatedWith(Annotation qualifier) {
		return with(qualifier);
	}

	@Override
	public IBindingBuilder<T> annotatedWith(Class<? extends Annotation> qualifierType) {
		return with(qualifierType);
	}

	@Override
	public IBindingBuilder<T> with(Annotation qualifier) {
		if (!qualifier.annotationType().isAnnotationPresent(Qualifier.class)) {
			throw new IllegalArgumentException("Not a qualifier annotation: " + qualifier);
		}
		binding.key().setQualifier(qualifier);
		return this;
	}

	@Override
	public IBindingBuilder<T> with(Class<? extends Annotation> qualifierType) {
		if (!qualifierType.isAnnotationPresent(Qualifier.class)) {
			throw new IllegalArgumentException("Not a qualifier annotation: " + qualifierType);
		}
		binding.key().setQualifier(qualifierType);
		return this;
	}

	@Override
	public IBindingBuilder<T> named(String name) {
		return with(Names.named(name));
	}

	@Override
	public IBindingBuilder<T> in(Class<? extends Annotation> annotation) {
		if (!annotation.isAnnotationPresent(Scope.class)) {
			throw new IllegalArgumentException("Not a scope annotation: " + annotation);
		}

		IScope scope = cdi.getScope(annotation);
		if (scope == null) {
			throw new IllegalStateException("No scope for annotation " + annotation);
		}
		
		binding.setProvider(scope.scope(binding.provider()));
		return this;
	}

	@Override
	public IBindingBuilder<T> toProvider(Provider<T> provider) {
		return provider(provider);
	}

	@Override
	public IBindingBuilder<T> provider(Provider<T> provider) {
		binding.setProvider(provider);
		return this;
	}

	@Override
	public IBindingBuilder<T> on(URI implementationURL) {
		return on(implementationURL.toString());
	}

	@Override
	public IBindingBuilder<T> on(String implementationURL) {
		binding.setProvider(new RemoteProvider<>(binding.key().type(), implementationURL));
		return this;
	}
}