package js.tiny.container.stub;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import js.lang.Config;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

public class ManagedClassSpiStub implements IManagedClass {
	@Override
	public String getImplementationURL() {
		throw new UnsupportedOperationException("getImplementationURL()");
	}

	@Override
	public Integer getKey() {
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
	public Iterable<IManagedMethod> getManagedMethods() {
		throw new UnsupportedOperationException("getManagedMethods()");
	}

	@Override
	public Iterable<IManagedMethod> getNetMethods() {
		throw new UnsupportedOperationException("getNetMethods()");
	}

	@Override
	public IManagedMethod getPostConstructMethod() {
		throw new UnsupportedOperationException("getPostConstructMethod()");
	}

	@Override
	public IManagedMethod getPreDestroyMethod() {
		throw new UnsupportedOperationException("getPreDestroyMethod()");
	}

	@Override
	public String getRequestPath() {
		throw new UnsupportedOperationException("getRequestPath()");
	}

	@Override
	public IManagedMethod getNetMethod(String methodName) {
		throw new UnsupportedOperationException("getNetMethod(String methodName)");
	}

	@Override
	public IManagedMethod getManagedMethod(Method method) throws NoSuchMethodException {
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
	public IContainer getContainer() {
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
	public String getTransactionalSchema() {
		throw new UnsupportedOperationException("getTransactionalSchema()");
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