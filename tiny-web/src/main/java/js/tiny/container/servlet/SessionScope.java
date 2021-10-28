package js.tiny.container.servlet;

import javax.inject.Provider;

import com.jslib.injector.IScope;

public class SessionScope implements IScope {
	@Override
	public <T> Provider<T> scope(Provider<T> provider) {
		return new SessionScopeProvider<>(provider);
	}
}
