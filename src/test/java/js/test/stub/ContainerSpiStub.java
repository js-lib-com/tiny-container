package js.test.stub;

import java.security.Principal;

import js.container.ContainerSPI;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;

public class ContainerSpiStub implements ContainerSPI {
	@Override
	public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
		throw new UnsupportedOperationException("getInstance(Class<? super T> interfaceClass, Object... args)");
	}

	@Override
	public <T> T getInstance(String instanceName, Class<? super T> interfaceClass, Object... args) {
		throw new UnsupportedOperationException("getInstance(String instanceName, Class<? super T> interfaceClass, Object... args)");
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
		throw new UnsupportedOperationException("getOptionalInstance(Class<? super T> interfaceClass, Object... args)");
	}

	@Override
	public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
		throw new UnsupportedOperationException("getRemoteInstance(URL implementationURL, Class<? super T> interfaceClass)");
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
	public <T extends Principal> T getUserPrincipal() {
		throw new UnsupportedOperationException("getUserPrincipal()");
	}

	@Override
	public boolean isAuthenticated() {
		throw new UnsupportedOperationException("isAuthenticated()");
	}

	@Override
	public <T> T getInstance(ManagedClassSPI managedClass, Object... args) {
		throw new UnsupportedOperationException("getInstance(ManagedClassSPI managedClass, Object... args)");
	}

	@Override
	public boolean isManagedClass(Class<?> interfaceClass) {
		throw new UnsupportedOperationException("isManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public ManagedClassSPI getManagedClass(Class<?> interfaceClass) {
		throw new UnsupportedOperationException("getManagedClass(Class<?> interfaceClass)");
	}

	@Override
	public Iterable<ManagedClassSPI> getManagedClasses() {
		throw new UnsupportedOperationException("getManagedClasses()");
	}

	@Override
	public Iterable<ManagedMethodSPI> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
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
