package js.tiny.container.stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collection;

import js.lang.Config;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

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
	public IManagedMethod getManagedMethod(String methodName) {
		throw new UnsupportedOperationException("getManagedMethod(String methodName)");
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
	public IContainer getContainer() {
		throw new UnsupportedOperationException("getContainer()");
	}

	@Override
	public Config getConfig() {
		throw new UnsupportedOperationException("getConfig()");
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

	@Override
	public void setAttribute(Object context, String name, Object value) {
		throw new UnsupportedOperationException("setAttribute(Object context, String name, Object value)");
	}

	@Override
	public <T> T getAttribute(Object context, String name, Class<T> type) {
		throw new UnsupportedOperationException("getAttribute(Object context, String name, Class<T> type)");
	}
}
