package com.jslib.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThreadsPoolTest {
	private ThreadsPool threadsPool;
	private boolean running;

	@Before
	public void beforeTest() {
		threadsPool = new ThreadsPool(Executors.newSingleThreadExecutor());
		running = true;
	}

	@After
	public void afterTest() {
		if (running) {
			threadsPool.preDestroy();
		}
	}

	@Test
	public void GivenCommandLockNotify_WhenExecute_ThenLockWait() {
		// given
		Object lock = new Object();

		// when
		threadsPool.execute("command", () -> {
			synchronized (lock) {
				lock.notify();
			}
		});

		// then
		synchronized (lock) {
			long start = System.currentTimeMillis();
			long stop = 0;
			try {
				lock.wait(4000L);
				stop = System.currentTimeMillis();
			} catch (InterruptedException e) {
			}
			assertThat(stop - start, not(greaterThan(4000L)));
		}
	}

	@Test
	public void GivenCommandRuntimeException_WhenExecute_ThenExceptionNotPropagated() {
		// given

		// when
		threadsPool.execute("command", () -> {
			throw new RuntimeException("exception");
		});
		
		// then
	}

	@Test
	public void GivenBlockedCommand_WhenExecute_ThenForceShutdown() {
		// given

		// when
		threadsPool.execute("command", () -> {
			for (;;) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		});

		// then
		threadsPool.preDestroy();
		running = false;
	}

	@Test
	public void GivenTaskStringValue_WhenSubmit_ThenFutureGet() throws InterruptedException, ExecutionException {
		// given

		// when
		Future<String> future = threadsPool.submit("task", () -> "promise");

		// then
		assertThat(future, notNullValue());
		assertThat(future.get(), equalTo("promise"));
	}

	@Test
	public void GivenTaskException_WhenSubmit_ThenFutureGetException() throws InterruptedException, ExecutionException {
		// given

		// when
		Future<String> future = threadsPool.submit("task", () -> {
			throw new Exception("exception");
		});

		// then
		assertThat(future, notNullValue());

		String exception = null;
		try {
			future.get();
		} catch (Exception e) {
			exception = e.getMessage();
		}
		assertThat(exception, containsString("exception"));
	}
}
