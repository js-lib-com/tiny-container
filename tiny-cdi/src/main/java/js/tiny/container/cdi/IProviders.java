package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

public interface IProviders {

	<T> Provider<T> getProvider(Class<T> type);

	<T> Provider<T> getProvider(Class<T> type, String uri);

	<T> IProviderDecorator<T> getScopedProviderDecorator(Class<? extends Annotation> scopeType);

}
