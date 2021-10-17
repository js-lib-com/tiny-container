package js.tiny.container.stub;

import java.io.File;
import java.security.Principal;
import java.util.Locale;

import js.tiny.container.servlet.AppContext;

public class AppContextStub implements AppContext {
	@Override
	public <T> T getInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getInstance(String instanceName, Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getInstance(String instanceName, Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getRemoteInstance(URL implementationURL, Class<? super T> interfaceClass)");
	}

	@Override
	public <T> T loadService(Class<T> serviceInterface) {
		throw new UnsupportedOperationException("loadService(Class<T> serviceInterface)");
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
	public String getAppName() {
		throw new UnsupportedOperationException("getAppName()");
	}

	@Override
	public File getAppFile(String path) {
		throw new UnsupportedOperationException("getAppFile(String path)");
	}

	@Override
	public <T> T getProperty(String name, Class<T> type) {
		throw new UnsupportedOperationException("getProperty(String name, Class<T> type)");
	}

	@Override
	public Locale getRequestLocale() {
		throw new UnsupportedOperationException("getRequestLocale()");
	}

	@Override
	public String getRemoteAddr() {
		throw new UnsupportedOperationException("getRemoteAddr()");
	}
}
