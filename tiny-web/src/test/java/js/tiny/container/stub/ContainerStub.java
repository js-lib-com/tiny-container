package js.tiny.container.stub;

import java.security.Principal;

import js.tiny.container.Container;

public class ContainerStub extends Container {
	@Override
	public boolean login(String username, String password) {
		throw new UnsupportedOperationException("login(String, String)");
	}

	@Override
	public void login(Principal principal) {
		throw new UnsupportedOperationException("login(Principal)");
	}

	@Override
	public void logout() {
		throw new UnsupportedOperationException("logout()");
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UnsupportedOperationException("getUserPrincipal()");
	}

	@Override
	public <T> T getProperty(String name, Class<T> type) {
		throw new UnsupportedOperationException("getProperty(String name, Class<T> type)");
	}
}
