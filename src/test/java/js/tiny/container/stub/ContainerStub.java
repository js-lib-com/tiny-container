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
	public String getLoginRealm() {
		throw new UnsupportedOperationException("getLoginRealm()");
	}

	@Override
	public String getLoginPage() {
		throw new UnsupportedOperationException("getLoginPage()");
	}

	@Override
	public void setProperty(String name, Object value) {
		throw new UnsupportedOperationException("setProperty(String name, Object value)");
	}
}
