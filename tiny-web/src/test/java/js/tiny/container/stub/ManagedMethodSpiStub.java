package js.tiny.container.stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import js.lang.InvocationException;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;

public class ManagedMethodSpiStub implements IManagedMethod {
	@Override
	public Method getMethod() {
		throw new UnsupportedOperationException("getMethod()");
	}

	@Override
	public Type[] getParameterTypes() {
		throw new UnsupportedOperationException("getParameterTypes()");
	}

	@Override
	public <T> T invoke(Object object, Object... arguments) throws IllegalArgumentException, InvocationException, AuthorizationException {
		throw new UnsupportedOperationException("invoke(Object object, Object... arguments)");
	}

	@Override
	public Type getReturnType() {
		throw new UnsupportedOperationException("getReturnType()");
	}

	@Override
	public String getReturnContentType() {
		throw new UnsupportedOperationException("getReturnContentType()");
	}

	@Override
	public String getRequestPath() {
		throw new UnsupportedOperationException("getRequestPath()");
	}

	@Override
	public boolean isVoid() {
		throw new UnsupportedOperationException("isVoid()");
	}

	@Override
	public boolean isRemotelyAccessible() {
		throw new UnsupportedOperationException("isRemotelyAccessible()");
	}

	@Override
	public boolean isUnchecked() {
		throw new UnsupportedOperationException("isUnchecked()");
	}

	@Override
	public boolean isTransactional() {
		throw new UnsupportedOperationException("isTransactional()");
	}

	@Override
	public boolean isImmutable() {
		throw new UnsupportedOperationException("isImmutable()");
	}

	@Override
	public boolean isAsynchronous() {
		throw new UnsupportedOperationException("isAsynchronous()");
	}

	@Override
	public IManagedClass getDeclaringClass() {
		throw new UnsupportedOperationException("getDeclaringClass()");
	}

	@Override
	public <T extends IServiceMeta> T getServiceMeta(Class<T> type) {
		throw new UnsupportedOperationException("getServiceMeta(Class<T> type)");
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> type) {
		throw new UnsupportedOperationException("getAnnotation(Class<T> type)");
	}
}
