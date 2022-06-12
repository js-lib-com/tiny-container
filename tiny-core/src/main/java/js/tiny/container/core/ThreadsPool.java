package js.tiny.container.core;

import static java.lang.String.format;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IThreadsPool;

@ApplicationScoped
public class ThreadsPool implements IThreadsPool {
	private static final Log log = LogFactory.getLog(ThreadsPool.class);

	private static final int THREAD_POOL_SIZE = 2;
	private static int DESTROY_TIMEOUT = 4000;

	private final ExecutorService executor;

	@Inject
	public ThreadsPool() {
		log.trace("ThreadsPool()");
		log.debug("Create threads pool for asynchronous commands and tasks. Pool size |%d|.", THREAD_POOL_SIZE);
		this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}

	public ThreadsPool(ExecutorService executor) {
		log.trace("ThreadsPool(ExecutorService)");
		this.executor = executor;
		ThreadsPool.DESTROY_TIMEOUT = 500;
	}

	@PreDestroy
	public void preDestroy() {
		log.trace("preDestroy()");
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
	public void execute(String commandName, Command command) {
		executor.execute(() -> {
			try (Watch watch = new Watch(commandName)) {
				command.call();
			} catch (Throwable throwable) {
				log.dump(format("Fail on asynchronous command: %s:", commandName), throwable);
			}
		});
	}

	@Override
	public <T> Future<T> submit(String taskName, Callable<T> task) {
		return executor.submit(() -> {
			try (Watch watch = new Watch(taskName)) {
				return task.call();
			} catch (Throwable throwable) {
				log.dump(format("Fail on asynchronous task: %s:", taskName), throwable);
				throw throwable;
			}
		});
	}

	private static class Watch implements AutoCloseable {
		private final long start = System.nanoTime();
		private final String callableName;

		public Watch(String callableName) {
			this.callableName = callableName;
		}

		@Override
		public void close() throws Exception {
			log.trace("Asynchronous %s processed in %.2f msec.", callableName, (System.nanoTime() - start) / 1000000.0);
		}
	}
}
