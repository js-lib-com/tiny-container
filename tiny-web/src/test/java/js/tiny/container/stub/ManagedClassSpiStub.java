package js.tiny.container.stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import js.lang.Config;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

public class ManagedClassSpiStub<T> implements IManagedClass<T> {
	@Override
	public String getImplementationURL() {
		throw new UnsupportedOperationException("getImplementationURL()");
	}

	@Override
	public Integer getKey() {
		throw new UnsupportedOperationException("getKey()");
	}

	@Override
	public Constructor<? extends T> getConstructor() {
		throw new UnsupportedOperationException("getConstructor()");
	}

	@Override
	public Class<T> getInterfaceClass() {
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
	public Class<? extends T> getImplementationClass() {
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
	public <S extends IServiceMeta> S getServiceMeta(Class<S> type) {
		throw new UnsupportedOperationException("getServiceMeta(Class<T> type)");
	}

	@Override
	public Set<IContainerService> getServices() {
		throw new UnsupportedOperationException("getServices()");
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		throw new UnsupportedOperationException("getAnnotation(Class<T> type)");
	}

	@Override
	public void setAttribute(Object context, String name, Object value) {
		throw new UnsupportedOperationException("setAttribute(Object context, String name, Object value)");
	}

	@Override
	public <A> A getAttribute(Object context, String name, Class<A> type) {
		throw new UnsupportedOperationException("getAttribute(Object context, String name, Class<T> type)");
	}
}
