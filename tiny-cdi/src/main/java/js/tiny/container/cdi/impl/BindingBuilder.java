package js.tiny.container.cdi.impl;

import java.lang.annotation.Annotation;

import javax.inject.Provider;
import javax.inject.Scope;

import js.tiny.container.cdi.IBindingBuilder;
import js.tiny.container.cdi.IProviderDecorator;
import js.tiny.container.cdi.IProviders;
import js.tiny.container.cdi.Names;

class BindingBuilder<T> implements IBindingBuilder<T> {
	private final IProviders providers;
	private final Binding<T> binding;

	public BindingBuilder(IProviders providers, Binding<T> binding) {
		this.providers = providers;
		this.binding = binding;
	}

	@Override
	public IBindingBuilder<T> to(Class<? extends T> type) {
		binding.setProvider(providers.getProvider(type));
		return this;
	}

	@Override
	public IBindingBuilder<T> annotatedWith(Annotation annotation) {
		return with(annotation);
	}

	@Override
	public IBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType) {
		return with(annotationType);
	}

	@Override
	public IBindingBuilder<T> with(Annotation annotation) {
		binding.key().setQualifier(annotation);
		return this;
	}

	@Override
	public IBindingBuilder<T> with(Class<? extends Annotation> annotationType) {
		binding.key().setQualifier(annotationType);
		return this;
	}

	@Override
	public IBindingBuilder<T> named(String name) {
		return with(Names.named(name));
	}

	@SuppressWarnings("unchecked")
	@Override
	public IBindingBuilder<T> in(Class<? extends Annotation> scope) {
		if (!scope.isAnnotationPresent(Scope.class)) {
			throw new IllegalArgumentException("Not a scope annotation: " + scope);
		}
		
		@SuppressWarnings("rawtypes")
		IProviderDecorator decorator = providers.getScopedProviderDecorator(scope);
		if (decorator == null) {
			throw new IllegalStateException("No provider for scope " + scope);
		}
		
		binding.setProvider(decorator.decorate(binding.provider()));
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
	public IBindingBuilder<T> on(String url) {
		binding.setProvider(providers.getProvider(binding.key().type(), url));
		return this;
	}
}