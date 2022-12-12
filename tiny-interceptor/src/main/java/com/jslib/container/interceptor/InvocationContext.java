package com.jslib.container.interceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.lang.InvocationException;

public class InvocationContext implements jakarta.interceptor.InvocationContext {
	private final IInvocationProcessorsChain chain;
	private final Object target;
	private final IManagedMethod method;
	private final Object[] parameters;

	public InvocationContext(IInvocationProcessorsChain chain, Object target, IManagedMethod managedMethod, Object[] parameters) {
		this.chain = chain;
		this.target = target;
		this.method = managedMethod;
		this.parameters = parameters;
	}

	@Override
	public Object getTarget() {
		return target;
	}

	@Override
	public Object getTimer() {
		throw new UnsupportedOperationException("Current implementation of the interceptor service does not support timers.");
	}

	@Override
	public Method getMethod() {
		return method.getMethod();
	}

	@Override
	public Constructor<?> getConstructor() {
		throw new UnsupportedOperationException("Current implementation of the interceptor service does not support constructor.");
	}

	@Override
	public Object[] getParameters() {
		return parameters;
	}

	@Override
	public void setParameters(Object[] params) {
		throw new UnsupportedOperationException("Current implementation of the interceptor service does not support parameters setter.");
	}

	@Override
	public Map<String, Object> getContextData() {
		throw new UnsupportedOperationException("Current implementation of the interceptor service does not support context data.");
	}

	@Override
	public Object proceed() throws Exception {
		IInvocation invocation = new IInvocation() {
			@Override
			public IManagedMethod method() {
				return method;
			}

			@Override
			public Object instance() {
				return target;
			}

			@Override
			public Object[] arguments() {
				return parameters;
			}
		};
		try {
			return chain.invokeNextProcessor(invocation);
		} catch (Throwable e) {
			throw new InvocationException(e);
		}
	}
}
