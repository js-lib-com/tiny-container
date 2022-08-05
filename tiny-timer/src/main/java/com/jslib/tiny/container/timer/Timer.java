package com.jslib.tiny.container.timer;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.annotation.PreDestroy;
import com.jslib.lang.AsyncExceptionListener;
import com.jslib.tiny.container.spi.IContainer;

/**
 * Scheduler for periodic and timeout tasks. This scheduler is a managed class and its instance if retrieved from factory, see
 * sample code. If task logic throws exception this scheduler takes care to invoke {@link AsyncExceptionListener}, of course if
 * application registers one.
 * <p>
 * This class supports two kinds of tasks: executed periodic with a given <code>period</code> and execute once, after a certain
 * <code>timeout</code> expires. Any pending task can be canceled, see <code>purge</code> methods.
 * 
 * <pre>
 * Timer timer = Factory.getInstance(Timer.class);
 * timer.period(this, period);
 * . . .
 * timer.purge(this);
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public final class Timer {
	private static final Log log = LogFactory.getLog(Timer.class);

	private final IContainer container;

	/** Java {@link java.util.Timer} instance. */
	private final java.util.Timer timer;

	/** Pending user defined tasks mapped to Java {@link TimerTask}. */
	private final Map<Object, TimerTask> tasks;

	public Timer(IContainer container) {
		log.trace("Timer(IContainer)");
		this.container = container;
		this.timer = new java.util.Timer();
		this.tasks = new HashMap<>();
	}

	@PreDestroy
	public synchronized void preDestroy() {
		log.trace("preDestroy()");
		tasks.values().forEach(task -> task.cancel());
		tasks.clear();
		timer.cancel();
	}

	/**
	 * Schedule periodic task execution.
	 * 
	 * @param periodicTask periodic task instance,
	 * @param period requested execution period, milliseconds.
	 */
	public synchronized void period(final PeriodicTask periodicTask, long period) {
		TimerTask task = new PeriodicTaskImpl(periodicTask);
		tasks.put(periodicTask, task);
		timer.schedule(task, 0L, period);
	}

	/**
	 * Schedule timeout task, reseting timeout period if given timeout task is already pending.
	 * 
	 * @param timeoutTask timeout task instance,
	 * @param timeout timeout value, milliseconds.
	 */
	public synchronized void timeout(final TimeoutTask timeoutTask, long timeout) {
		TimerTask task = tasks.get(timeoutTask);
		if (task != null) {
			task.cancel();
			tasks.values().remove(task);
		}
		task = new TimeoutTaskImpl(timeoutTask);
		tasks.put(timeoutTask, task);
		timer.schedule(task, timeout);
	}

	/**
	 * Purge periodic task. If given periodic task is not scheduled this method is NOP.
	 * 
	 * @param periodicTask periodic task instance.
	 */
	public synchronized void purge(PeriodicTask periodicTask) {
		purgeTask(periodicTask);
	}

	/**
	 * Purge timeout task. If timeout task is not scheduled this method is NOP.
	 * 
	 * @param timeoutTask timeout task instance.
	 */
	public synchronized void purge(TimeoutTask timeoutTask) {
		purgeTask(timeoutTask);
	}

	/**
	 * Purge task helper method. If given <code>task</code> is not scheduled this method does nothing.
	 * 
	 * @param task pending user defined task.
	 */
	private void purgeTask(Object task) {
		TimerTask timerTask = tasks.get(task);
		if (timerTask != null) {
			timerTask.cancel();
			tasks.values().remove(timerTask);
		}
	}

	// ----------------------------------------------------
	// TASK CLASS IMPLEMENTATIONS

	/**
	 * Periodic task implementation.
	 * 
	 * @author Iulian Rotaru
	 */
	private class PeriodicTaskImpl extends TimerTask {
		private PeriodicTask periodicTask;

		public PeriodicTaskImpl(PeriodicTask periodicTask) {
			super();
			this.periodicTask = periodicTask;
		}

		@Override
		public void run() {
			try {
				this.periodicTask.onPeriod();
			} catch (Throwable throwable) {
				Timer.log.error("%s: %s: %s", this.periodicTask.getClass(), throwable.getClass(), throwable.getMessage());
				AsyncExceptionListener listener = container.getOptionalInstance(AsyncExceptionListener.class);
				if (listener != null) {
					listener.onAsyncException(throwable);
				}
			}
		}
	}

	/**
	 * Timeout task implementation.
	 * 
	 * @author Iulian Rotaru
	 */
	private class TimeoutTaskImpl extends TimerTask {
		private TimeoutTask timeoutTask;

		public TimeoutTaskImpl(TimeoutTask timeoutTask) {
			super();
			this.timeoutTask = timeoutTask;
		}

		@Override
		public void run() {
			try {
				this.timeoutTask.onTimeout();
			} catch (Throwable throwable) {
				Timer.log.error("%s: %s: %s", this.timeoutTask.getClass(), throwable.getClass(), throwable.getMessage());
				AsyncExceptionListener listener = container.getOptionalInstance(AsyncExceptionListener.class);
				if (listener != null) {
					listener.onAsyncException(throwable);
				}
			}
		}
	}
}
