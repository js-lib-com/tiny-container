package js.tiny.container;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessor;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;

final class InvocationProcessorsChain implements IInvocationProcessorsChain {
	private static final Log log = LogFactory.getLog(InvocationProcessorsChain.class);

	/** Set of method invocation processors in the proper order for execution. */
	private final SortedSet<IInvocationProcessor> processors;

	private Iterator<IInvocationProcessor> iterator;

	public InvocationProcessorsChain() {
		log.trace("MethodInvocationProcessorsChain()");
		this.processors = new TreeSet<>((p1, p2) -> p1.getPriority().compareTo(p2.getPriority()));
	}

	public void addProcessor(IInvocationProcessor processor) {
		processors.add(processor);
	}

	public void addProcessors(Collection<IInvocationProcessor> processors) {
		this.processors.addAll(processors);
	}

	public void createIterator() {
		iterator = processors.iterator();
	}

	public IInvocation createMethodInvocation(IManagedMethod managedMethod, Object instance, Object[] arguments) {
		return new MethodInvocation(managedMethod, instance, arguments);
	}

	@Override
	public Object invokeNextProcessor(IInvocation methodInvocation) throws Exception {
		if (!iterator.hasNext()) {
			// TODO: end of processing chain
			return null;
		}
		return iterator.next().execute(this, methodInvocation);
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
