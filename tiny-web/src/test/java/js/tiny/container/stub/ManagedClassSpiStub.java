package js.tiny.container.stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import js.lang.Config;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;

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

	@Override
	public <T extends IServiceMeta> T getServiceMeta(Class<T> type) {
		throw new UnsupportedOperationException("getServiceMeta(Class<T> type)");
	}

	@Override
	public Collection<IContainerService> getServices() {
		throw new UnsupportedOperationException("getServices()");
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> type) {
		throw new UnsupportedOperationException("getAnnotation(Class<T> type)");
	}
}
