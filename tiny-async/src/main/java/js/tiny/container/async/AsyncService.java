package js.tiny.container.async;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Asynchronous;

import js.lang.AsyncTask;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessor;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;
import js.tiny.container.spi.Priority;

class AsyncService implements IInvocationProcessor, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(AsyncService.class);

	public AsyncService() {
		log.trace("AsyncService()");
	}

	@Override
	public int getPriority() {
		return Priority.HIGH.value(1);
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedClass managedClass) {
		List<IServiceMeta> servicesMeta = new ArrayList<>();

		Asynchronous immutable = managedClass.getAnnotation(Asynchronous.class);
		if (immutable != null) {
			servicesMeta.add(new AsynchronousMeta(this));
		}

		return servicesMeta;
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		List<IServiceMeta> servicesMeta = new ArrayList<>();

		Asynchronous immutable = managedMethod.getAnnotation(Asynchronous.class);
		if (immutable != null) {
			servicesMeta.add(new AsynchronousMeta(this));
		}

		return servicesMeta;
	}

	/**
	 * Perform method invocation in a separated thread of execution.
	 * 
	 * Current implementation is based on {@link AsyncTask} and has no means to <code>join</code> after starting asynchronous
	 * tasks. If invoker executed asynchronously fails the only option to be notified is application logger.
	 * 
	 */
	@Override
	public Object executeService(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		if (!isAsynchronous(invocation.method())) {
			return chain.invokeNextProcessor(invocation);
		}

		log.debug("Execute asynchronous |%s|.", invocation.method());
		AsyncTask<Void> asyncTask = new AsyncTask<Void>() {
			@Override
			protected Void execute() throws Throwable {
				chain.invokeNextProcessor(invocation);
				return null;
			}
		};
		asyncTask.start();

		return null;
	}

	private static boolean isAsynchronous(IManagedMethod managedMethod) {
		if (managedMethod.getServiceMeta(AsynchronousMeta.class) != null) {
			return true;
		}
		return managedMethod.getDeclaringClass().getServiceMeta(AsynchronousMeta.class) != null;
	}
}