package js.tiny.container;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;

final class MethodInvocationProcessorsChain implements IMethodInvocationProcessorsChain {
	private static final Log log = LogFactory.getLog(MethodInvocationProcessorsChain.class);

	/** Set of method invocation processors in the proper order for execution. */
	private final SortedSet<IMethodInvocationProcessor> processors;

	private Iterator<IMethodInvocationProcessor> iterator;

	public MethodInvocationProcessorsChain() {
		log.trace("MethodInvocationProcessorsChain()");
		this.processors = new TreeSet<>((p1, p2) -> p1.getPriority().compareTo(p2.getPriority()));
	}

	public void addProcessor(IMethodInvocationProcessor processor) {
		processors.add(processor);
	}

	public void addProcessors(Collection<IMethodInvocationProcessor> processors) {
		this.processors.addAll(processors);
	}

	public void createIterator() {
		iterator = processors.iterator();
	}

	public IMethodInvocation createMethodInvocation(IManagedMethod managedMethod, Object instance, Object[] arguments) {
		return new MethodInvocation(managedMethod, instance, arguments);
	}

	@Override
	public Object invokeNextProcessor(IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException {
		if (!iterator.hasNext()) {
			// TODO: end of chain logic
			return null;
		}
		return iterator.next().invoke(this, methodInvocation);
	}

	private static final class MethodInvocation implements IMethodInvocation {
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
