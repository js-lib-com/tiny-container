package js.tiny.container.servlet;

import javax.inject.Provider;

import com.jslib.injector.ScopedProvider;

public class SessionScopeProvider<T> extends ScopedProvider<T> {

	protected SessionScopeProvider(Provider<T> provider) {
		super(provider);
	}

	@Override
	public T getScopeInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T get() {
		return super.get();
	}

}
