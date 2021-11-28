package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.net.URI;

import javax.inject.Provider;

import js.injector.IBinding;
import js.injector.IBindingBuilder;
import js.injector.ITypedProvider;

/**
 * Container internal bindings builder. Created injector bindings are for container internal use only. This chained builder
 * collects binding parameters into {@link ContainerBindingParameters} and send them to
 * {@link CDI#bind(ContainerBindingParameters)} when {@link #build()} is invoked.
 * 
 * @author Iulian Rotaru
 */
public class ContainerBindingBuilder<T> implements IBindingBuilder<T> {
	private final CDI cdi;
	private final ContainerBindingParameters<T> binding;

	public ContainerBindingBuilder(CDI cdi, Class<T> interfaceClass) {
		this.cdi = cdi;
		this.binding = new ContainerBindingParameters<>(interfaceClass);
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
