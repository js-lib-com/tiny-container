package com.jslib.container.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IMethodInvocationProcessor;
import com.jslib.lang.BugError;

class InvocationProcessorsChain implements IInvocationProcessorsChain {
	/**
	 * List of method invocation processors in the proper order for execution. Managed method is a method invocation processor
	 * too and is the last item on this list.
	 */
	private final List<IMethodInvocationProcessor> processors;

	private final Iterator<IMethodInvocationProcessor> iterator;

	public InvocationProcessorsChain(FlowProcessorsSet<IMethodInvocationProcessor> processors, IMethodInvocationProcessor managedMethod) {
		this.processors = new ArrayList<>();
		processors.forEach(processor -> this.processors.add(processor));
		// managed method is a method invocation processor too
		// its default priority ensures that it is executed at the end, after all other invocation processors
		processors.add(managedMethod);
		iterator = processors.iterator();
	}

	public IInvocation createInvocation(IManagedMethod managedMethod, Object instance, Object[] arguments) {
		return new MethodInvocation(managedMethod, instance, arguments);
	}

	@Override
	public Object invokeNextProcessor(IInvocation invocation) throws Throwable {
		if (!iterator.hasNext()) {
			throw new BugError("Invocation processors chain was not properly ended. See ManagedMethod#onMethodInvocation().");
		}
		return iterator.next().onMethodInvocation(this, invocation);
	}

	private static final class MethodInvocation implements IInvocation {
		private final IManagedMethod method;
		private final Object instance;
		private final Object[] arguments;

		public MethodInvocation(IManagedMethod method, Object instance, Object[] arguments) {
			this.method = method;
			this.instance = instance;
			this.arguments = arguments;
		}

		@Override
		public IManagedMethod method() {
			return method;
		}

		@Override
		public Object instance() {
			return instance;
		}

		@Override
		public Object[] arguments() {
			return arguments;
		}
	}
}
