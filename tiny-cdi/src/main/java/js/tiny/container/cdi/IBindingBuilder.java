package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

public interface IBindingBuilder<T> {

	IBindingBuilder<T> to(Class<? extends T> type);

	IBindingBuilder<T> annotatedWith(Annotation qualifier);

	IBindingBuilder<T> annotatedWith(Class<? extends Annotation> qualifierType);

	IBindingBuilder<T> with(Annotation qualifier);

	IBindingBuilder<T> with(Class<? extends Annotation> qualifierType);

	IBindingBuilder<T> named(String name);

	IBindingBuilder<T> in(Class<? extends Annotation> scopeType);

	IBindingBuilder<T> toProvider(Provider<T> provider);

	IBindingBuilder<T> provider(Provider<T> provider);

	IBindingBuilder<T> on(String url);

}
