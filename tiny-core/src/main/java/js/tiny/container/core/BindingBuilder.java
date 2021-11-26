package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.net.URI;

import javax.inject.Provider;

import js.injector.IBinding;
import js.injector.IBindingBuilder;
import js.injector.ITypedProvider;
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.ContainerBinding;

class BindingBuilder<T> implements IBindingBuilder<T> {
	private final CDI cdi;
	private final ContainerBinding<T> binding;

	public BindingBuilder(CDI cdi, Class<T> interfaceClass) {
		this.cdi = cdi;
		this.binding = new ContainerBinding<>(interfaceClass);
	}

	@Override
	public IBindingBuilder<T> to(Class<? extends T> implementationClass) {
		binding.setImplementationClass(implementationClass);
		return this;
	}

	@Override
	public IBindingBuilder<T> instance(T instance) {
		binding.setInstance(instance);
		return this;
	}

	@Override
	public IBindingBuilder<T> on(URI implementationURL) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> with(Annotation qualifier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> with(Class<? extends Annotation> qualifierType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> named(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> provider(Provider<T> provider) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> provider(ITypedProvider<T> provider) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> service() {
		binding.setService(true);
		return this;
	}

	@Override
	public IBindingBuilder<T> on(String implementationURL) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> in(Class<? extends Annotation> scopeType) {
		binding.setScope(scopeType);
		return this;
	}

	@Override
	public Provider<T> getProvider() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBinding<T> getBinding() {
		throw new UnsupportedOperationException();
	}

	public IBindingBuilder<T> build() {
		cdi.bind(binding);
		return this;
	}
}
