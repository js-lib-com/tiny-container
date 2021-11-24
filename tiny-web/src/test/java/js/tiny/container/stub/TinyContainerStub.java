package js.tiny.container.stub;

import java.security.Principal;
import java.util.List;

import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.spi.IBindingBuilder;
import js.tiny.container.spi.IManagedClass;

public class TinyContainerStub implements ITinyContainer {
	@Override
	public <T> IBindingBuilder<T> bind(Class<T> interfaceClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getInstance(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getOptionalInstance(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public boolean login(String username, String password) {
		throw new UnsupportedOperationException("login(String username, String password)");
	}

	@Override
	public void login(Principal principal) {
		throw new UnsupportedOperationException("login(Principal principal)");
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
	public boolean isAuthenticated() {
		throw new UnsupportedOperationException("isAuthenticated()");
	}

	@Override
	public boolean isAuthorized(String... roles) {
		throw new UnsupportedOperationException("isAuthorized(String... roles)");
	}

	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public List<IManagedClass<?>> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}

	@Override
	public String getAppName() {
		throw new UnsupportedOperationException("getAppName()");
	}

	@Override
	public String getLoginPage() {
		throw new UnsupportedOperationException("getLoginPage()");
	}
}
