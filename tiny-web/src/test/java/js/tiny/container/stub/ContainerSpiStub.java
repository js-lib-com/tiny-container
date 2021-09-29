package js.tiny.container.stub;

import java.security.Principal;
import java.util.List;

import js.tiny.container.ContainerSPI;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.core.IContainerService;

public class ContainerSpiStub implements ContainerSPI {
	@Override
	public List<IContainerService> getServices() {
		throw new UnsupportedOperationException("getPlugins()");
	}

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
	public Iterable<ManagedMethodSPI> getNetMethods() {
		throw new UnsupportedOperationException("getNetMethods()");
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
