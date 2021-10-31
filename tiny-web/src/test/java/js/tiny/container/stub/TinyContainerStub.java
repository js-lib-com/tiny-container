package js.tiny.container.stub;

import java.io.File;
import java.security.Principal;
import java.util.Locale;

import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

public class TinyContainerStub implements ITinyContainer {
	@Override
	public <T> T getInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
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
	public <T> T getInstance(IManagedClass<T> managedClass) {
		throw new UnsupportedOperationException("getInstance(ManagedClassSPI managedClass)");
	}

	@Override
	public boolean isManagedClass(Class<?> interfaceClass) {
		throw new UnsupportedOperationException("isManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		throw new UnsupportedOperationException("getManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public Iterable<IManagedClass<?>> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
	}

	@Override
	public <T> T getProperty(String name, Class<T> type) {
		throw new UnsupportedOperationException("getProperty(String name, Class<T> type)");
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
	public String getAppName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getAppFile(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale getRequestLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		return null;
	}
}
