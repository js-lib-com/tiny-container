package js.tiny.container.timer;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import js.lang.AsyncExceptionListener;
import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.Factory;

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
public final class Timer implements ManagedLifeCycle {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Timer.class);

	/** Java {@link java.util.Timer} instance. */
	private java.util.Timer timer;

	/** Pending user defined tasks mapped to Java {@link TimerTask}. */
	private Map<Object, TimerTask> tasks;

	@Override
	public synchronized void postConstruct() throws Exception {
		this.timer = new java.util.Timer();
		this.tasks = new HashMap<Object, TimerTask>();
	}

	@Override
	public synchronized void preDestroy() throws Exception {
		for (TimerTask task : this.tasks.values()) {
			task.cancel();
		}
		this.tasks.clear();
		this.timer.cancel();
	}

	/**
	 * Schedule periodic task execution.
	 * 
	 * @param periodicTask periodic task instance,
	 * @param period requested execution period, milliseconds.
	 */
	public synchronized void period(final PeriodicTask periodicTask, long period) {
		TimerTask task = new PeriodicTaskImpl(periodicTask);
		this.tasks.put(periodicTask, task);
		this.timer.schedule(task, 0L, period);
	}

	/**
	 * Schedule timeout task, reseting timeout period if given timeout task is already pending.
	 * 
	 * @param timeoutTask timeout task instance,
	 * @param timeout timeout value, milliseconds.
	 */
	public synchronized void timeout(final TimeoutTask timeoutTask, long timeout) {
		TimerTask task = this.tasks.get(timeoutTask);
		if (task != null) {
			task.cancel();
			this.tasks.values().remove(task);
		}
		task = new TimeoutTaskImpl(timeoutTask);
		this.tasks.put(timeoutTask, task);
		this.timer.schedule(task, timeout);
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
		TimerTask timerTask = this.tasks.get(task);
		if (timerTask != null) {
			timerTask.cancel();
			this.tasks.values().remove(timerTask);
		}
	}

	/**
	 * Prevent instantiation with new operator.
	 */
	private Timer() {
	}

	// ----------------------------------------------------
	// TASK CLASS IMPLEMENTATIONS

	/**
	 * Periodic task implementation.
	 * 
	 * @author Iulian Rotaru
	 */
	private static class PeriodicTaskImpl extends TimerTask {
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
				AsyncExceptionListener listener = Factory.getOptionalInstance(AsyncExceptionListener.class);
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
	private static class TimeoutTaskImpl extends TimerTask {
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
				AsyncExceptionListener listener = Factory.getOptionalInstance(AsyncExceptionListener.class);
				if (listener != null) {
					listener.onAsyncException(throwable);
				}
			}
		}
	}
}
