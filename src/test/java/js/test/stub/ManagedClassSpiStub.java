package js.test.stub;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import js.container.ContainerSPI;
import js.container.InstanceScope;
import js.container.InstanceType;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.lang.Config;

public class ManagedClassSpiStub implements ManagedClassSPI {
	@Override
	public String getImplementationURL() {
		throw new UnsupportedOperationException("getImplementationURL()");
	}

	@Override
	public String getKey() {
		throw new UnsupportedOperationException("getKey()");
	}

	@Override
	public Constructor<?> getConstructor() {
		throw new UnsupportedOperationException("getConstructor()");
	}

	@Override
	public Class<?>[] getInterfaceClasses() {
		throw new UnsupportedOperationException("getInterfaceClasses()");
	}

	@Override
	public Class<?> getInterfaceClass() {
		throw new UnsupportedOperationException("getInterfaceClass()");
	}

	@Override
	public Iterable<ManagedMethodSPI> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
	}

	@Override
	public Iterable<ManagedMethodSPI> getCronManagedMethods() {
		throw new UnsupportedOperationException("getCronManagedMethods()");
	}

	@Override
	public String getRequestPath() {
		throw new UnsupportedOperationException("getRequestPath()");
	}
	
	@Override
	public ManagedMethodSPI getNetMethod(String methodName) {
		throw new UnsupportedOperationException("getNetMethod(String methodName)");
	}
	
	@Override
	public ManagedMethodSPI getManagedMethod(Method method) throws NoSuchMethodException {
		throw new UnsupportedOperationException("getManagedMethod(Method method)");
	}

	@Override
	public InstanceScope getInstanceScope() {
		throw new UnsupportedOperationException("getInstanceScope()");
	}

	@Override
	public InstanceType getInstanceType() {
		throw new UnsupportedOperationException("getInstanceType()");
	}

	@Override
	public Class<?> getImplementationClass() {
		throw new UnsupportedOperationException("getImplementationClass()");
	}

	@Override
	public boolean isRemotelyAccessible() {
		throw new UnsupportedOperationException("isRemotelyAccessible()");
	}

	@Override
	public ContainerSPI getContainer() {
		throw new UnsupportedOperationException("getContainer()");
	}

	@Override
	public Iterable<Field> getDependencies() {
		throw new UnsupportedOperationException("getDependencies()");
	}

	@Override
	public boolean isTransactional() {
		throw new UnsupportedOperationException("isTransactional()");
	}

	@Override
	public Config getConfig() {
		throw new UnsupportedOperationException("getConfig()");
	}

	@Override
	public boolean isAutoInstanceCreation() {
		throw new UnsupportedOperationException("isAutoInstanceCreation()");
	}

	@Override
	public Map<String, Field> getContextParamFields() {
		throw new UnsupportedOperationException("getContextParamFields()");
	}
}
