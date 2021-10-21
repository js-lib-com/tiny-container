package js.tiny.container.cdi;

import javax.inject.Provider;

public interface IProviderDecorator<T> {

	Provider<T> decorate(Provider<T> provider);

}
