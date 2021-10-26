package js.tiny.container.cdi;

import javax.inject.Provider;

@Deprecated
public interface IProviderDecorator<T> {

	Provider<T> decorate(Provider<T> provider);

}
