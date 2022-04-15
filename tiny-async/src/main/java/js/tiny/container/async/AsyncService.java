package js.tiny.container.async;

import static java.lang.String.format;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.ejb.Asynchronous;
import jakarta.inject.Inject;
import js.lang.AsyncTask;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.ServiceConfigurationException;
import js.util.Types;

/**
 * Container service for method asynchronous execution.
 * 
 * @author Iulian Rotaru
 */
public class AsyncService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(AsyncService.class);

	private static final int THREAD_POOL_SIZE = 2;
	private static final int DESTROY_TIMEOUT = 4000;

	private final ExecutorService executor;

	@Inject
	public AsyncService() {
		log.trace("AsyncService()");
		log.debug("Create threads pool for asynchronous methods. Pool size |%d|.", THREAD_POOL_SIZE);
		this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}

	public AsyncService(ExecutorService executor) {
		log.trace("AsyncService(ExecutorService)");
		this.executor = executor;
	}

	@Override
	public void destroy() {
		log.trace("destroy()");
		try {
			log.debug("Initiate graceful threads pool shutdown.");
			executor.shutdown();
			if (!executor.awaitTermination(DESTROY_TIMEOUT, TimeUnit.MILLISECONDS)) {
				log.warn("Timeout waiting for threads pool termination. Force shutdown now.");
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	@Override
	public Priority getPriority() {
		return Priority.ASYNCHRONOUS;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES) == null) {
			return false;
		}
		if (managedMethod.isStatic()) {
			throw new ServiceConfigurationException("Asynchronous method |%s| cannot be static.", managedMethod);
		}
		if (managedMethod.isVoid()) {
			if (managedMethod.getExceptionTypes().length != 0) {
				throw new ServiceConfigurationException("Void asynchronous method |%s| cannot have checked exception(s).", managedMethod);
			}
			return true;
		}
		if (!Types.isKindOf(managedMethod.getReturnType(), Future.class)) {
			throw new ServiceConfigurationException("Invalid asynchronous method |%s| return type. Should be void or Future.", managedMethod);
		}
		return true;
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

		if (invocation.method().isVoid()) {
			executor.execute(() -> {
				try (Watch watch = new Watch(invocation)) {
					chain.invokeNextProcessor(invocation);
				} catch (Throwable throwable) {
					log.dump(format("Fail on asynchronous method |%s|:", invocation.method()), throwable);
				}
			});
			return null;
		}

		// at this point we know that invocation method returns a future
		return executor.submit(() -> {
			try (Watch watch = new Watch(invocation)) {
				Future<?> future = (Future<?>) chain.invokeNextProcessor(invocation);
				return future.get();
			} catch (Throwable throwable) {
				log.dump(format("Fail on asynchronous method |%s|:", invocation.method()), throwable);
				throw throwable;
			}
		});
	}

	private static class Watch implements AutoCloseable {
		private final long start = System.nanoTime();
		private final IManagedMethod method;

		public Watch(IInvocation invocation) {
			this.method = invocation.method();
		}

		@Override
		public void close() throws Exception {
			log.trace("Asynchronous method |%s| processed in %.2f msec.", method, (System.nanoTime() - start) / 1000000.0);
		}
	}
}
