package js.tiny.container.stub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

import js.lang.InvocationException;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

public class ManagedMethodSpiStub implements IManagedMethod {
	@Override
	public String getName() {
		throw new UnsupportedOperationException("getName()");
	}

	@Override
	public String getSignature() {
		throw new UnsupportedOperationException("getSignature()");
	}

	@Override
	public Method getMethod() {
		throw new UnsupportedOperationException("getMethod()");
	}

	@Override
	public Type[] getParameterTypes() {
		throw new UnsupportedOperationException("getParameterTypes()");
	}

	@Override
	public boolean isPublic() {
		return true;
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
	public IManagedClass<?> getDeclaringClass() {
		throw new UnsupportedOperationException("getDeclaringClass()");
	}

	@Override
	public <T extends Annotation> T scanAnnotation(Class<T> type) {
		throw new UnsupportedOperationException("getAnnotation(Class<T> type)");
	}

	@Override
	public void addAnnotation(Annotation serviceMeta) {
		throw new UnsupportedOperationException("addServiceMeta(IServiceMeta serviceMeta)");
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> type) {
		throw new UnsupportedOperationException("getServiceMeta(Class<T> type)");
	}

	@Override
	public void setAttribute(Object context, String name, Object value) {
		throw new UnsupportedOperationException("setAttribute(Object context, String name, Object value)");
	}

	@Override
	public <T> T getAttribute(Object context, String name, Class<T> type) {
		throw new UnsupportedOperationException("getAttribute(Object context, String name, Class<T> type)");
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		throw new UnsupportedOperationException("onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation)");
	}

	@Override
	public Priority getPriority() {
		throw new UnsupportedOperationException("getPriority()");
	}

	@Override
	public void scanServices(Collection<IContainerService> services) {
	}
}
