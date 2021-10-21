package js.tiny.container.cdi;

import javax.inject.Provider;

public interface IBinding<T> {

	Key<T> key();

	Provider<? extends T> provider();

}
