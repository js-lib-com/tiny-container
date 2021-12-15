package js.tiny.container.async;

import javax.ejb.Asynchronous;

import js.lang.AsyncTask;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;

public class AsyncService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(AsyncService.class);

	@Override
	public Priority getPriority() {
		return Priority.ASYNCHRONOUS;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(Asynchronous.class) != null) {
			return true;
		}
		return managedMethod.getDeclaringClass().scanAnnotation(Asynchronous.class) != null;
	}

	/**
	 * Perform method invocation in a separated thread of execution.
	 * 
	 * Current implementation is based on {@link AsyncTask} and has no means to <code>join</code> after starting asynchronous
	 * tasks. If invoker executed asynchronously fails the only option to be notified is application logger.
	 * 
	 */
	@Override
	public Object onMethodInvocation(final IInvocationProcessorsChain chain, final IInvocation invocation) throws Exception {
		log.debug("Execute asynchronous |%s|.", invocation.method());
		AsyncTask<Void> asyncTask = new AsyncTask<Void>() {
			@Override
			protected Void execute() throws Throwable {
				long start = System.nanoTime();
				try {
					chain.invokeNextProcessor(invocation);
				} finally {
					log.trace("Asynchronous method |%s| processed in %.2f msec.", invocation.method(), (System.nanoTime() - start) / 1000000.0);
				}
				return null;
			}

			@Override
			protected void onThrowable(Throwable throwable) {
				log.dump(String.format("Fail on asynchronous method |%s|:", invocation.method()), throwable);
			}
		};
		asyncTask.start();

		return null;
	}
}
