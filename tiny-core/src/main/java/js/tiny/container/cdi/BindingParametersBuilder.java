package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.net.URI;

import jakarta.inject.Provider;
import js.injector.IBinding;
import js.injector.IBindingBuilder;
import js.injector.ITypedProvider;

/**
 * Container internal bindings builder. Created injector bindings are for container internal use only. This chained builder
 * collects binding parameters into {@link BindingParameters} and send them to {@link CDI#bind(BindingParameters)} when
 * {@link #build()} is invoked.
 * 
 * @author Iulian Rotaru
 */
public class BindingParametersBuilder<T> implements IBindingBuilder<T> {
	private final CDI cdi;
	private final BindingParameters<T> parameters;

	public BindingParametersBuilder(CDI cdi, Class<T> interfaceClass) {
		this.cdi = cdi;
		this.parameters = new BindingParameters<>(interfaceClass);
	}

	@Override
	public IBindingBuilder<T> to(Class<? extends T> implementationClass) {
		parameters.setImplementationClass(implementationClass);
		return this;
	}

	@Override
	public IBindingBuilder<T> instance(T instance) {
		parameters.setInstance(instance);
		return this;
	}

	@Override
	public IBindingBuilder<T> on(URI implementationURL) {
		parameters.setImplementationURL(implementationURL);
		return this;
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
		parameters.setProvider(provider);
		return this;
	}

	@Override
	public IBindingBuilder<T> provider(ITypedProvider<T> provider) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBindingBuilder<T> service() {
		parameters.setService(true);
		return this;
	}

	@Override
	public IBindingBuilder<T> on(String implementationURL) {
		parameters.setImplementationURL(URI.create(implementationURL));
		return this;
	}

	@Override
	public IBindingBuilder<T> in(Class<? extends Annotation> scopeType) {
		parameters.setScope(scopeType);
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
		cdi.bind(parameters);
		return this;
	}
}
