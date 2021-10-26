package js.tiny.container.cdi;

import javax.inject.Provider;

public interface IScope {

	<T> Provider<T> scope(Provider<T> provider);

}
