package js.tiny.container;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;

public final class CronManager {
	private static final Log log = LogFactory.getLog(CronManager.class);

	private final ScheduledExecutorService scheduler;

	public CronManager() {
		log.trace("CronManager()");
		scheduler = Executors.newScheduledThreadPool(10);
	}

	public void register(Object instance, ManagedMethodSPI managedMethod) {
		log.debug("Register crom method |%s|.", managedMethod);
		scheduler.schedule(new CronTask(scheduler, instance, managedMethod), delay(managedMethod.getCronExpression()), TimeUnit.MINUTES);
	}

	private static long delay(String expression) {
		CronExpression cronExpression = new CronExpression(expression);

		final Calendar now = Calendar.getInstance();
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		
		Calendar nextExecutionDate = Calendar.getInstance();
		nextExecutionDate.set(Calendar.SECOND, 0);
		nextExecutionDate.set(Calendar.MILLISECOND, 0);

		int calendarField = CronExpression.UNSET_FIELD;
		if (cronExpression.hasMinute()) {
			nextExecutionDate.set(Calendar.MINUTE, cronExpression.getMinute());
		} else {
			calendarField = Calendar.MINUTE;
		}

		if (cronExpression.hasHour()) {
			nextExecutionDate.set(Calendar.HOUR_OF_DAY, cronExpression.getHour());
		} else {
			if (calendarField == CronExpression.UNSET_FIELD) {
				calendarField = Calendar.HOUR_OF_DAY;
			}
		}

		if (cronExpression.hasDay()) {
			nextExecutionDate.set(Calendar.DATE, cronExpression.getDay());
		} else {
			if (calendarField == CronExpression.UNSET_FIELD) {
				calendarField = Calendar.DATE;
			}
		}

		if (cronExpression.hasMonth()) {
			nextExecutionDate.set(Calendar.MONTH, cronExpression.getMonth());
		} else {
			if (calendarField == CronExpression.UNSET_FIELD) {
				calendarField = Calendar.MONTH;
			}
		}

		if (calendarField == CronExpression.UNSET_FIELD) {
			if (calendarField == CronExpression.UNSET_FIELD) {
				calendarField = Calendar.YEAR;
			}
		}

		if (nextExecutionDate.getTimeInMillis() <= now.getTimeInMillis()) {
			nextExecutionDate.add(calendarField, 1);
		}

		long delay = (nextExecutionDate.getTimeInMillis() - now.getTimeInMillis()) / 60000;
		log.debug("Next execution date |%s|. Delay is |%d|", nextExecutionDate.getTime(), delay);
		return delay;
	}

	public void destroy() {
		log.trace("destroy()");
		scheduler.shutdownNow();
	}

	private static final class CronTask implements Runnable {
		private final ScheduledExecutorService scheduler;
		private final Object instance;
		private final ManagedMethodSPI managedMethod;

		public CronTask(ScheduledExecutorService scheduler, Object instance, ManagedMethodSPI managedMethod) {
			this.scheduler = scheduler;
			this.instance = instance;
			this.managedMethod = managedMethod;
		}

		@Override
		public void run() {
			log.debug("Execute cron method |%s|.", managedMethod);
			try {
				managedMethod.invoke(instance);
				scheduler.schedule(this, delay(managedMethod.getCronExpression()), TimeUnit.MINUTES);
			} catch (IllegalArgumentException | InvocationException | AuthorizationException e) {
				log.error(e);
			}
		}
	}

	private static final class CronExpression {
		public static final int UNSET_FIELD = -1;

		private int minute = UNSET_FIELD;
		private int hour = UNSET_FIELD;
		private int day = UNSET_FIELD;
		private int month = UNSET_FIELD;
		private int weekday = UNSET_FIELD;

		public CronExpression(String expression) {
			String[] parts = expression.split(" ");
			if (!parts[0].equals("*")) {
				minute = Integer.parseInt(parts[0]);
			}
			if (!parts[1].equals("*")) {
				hour = Integer.parseInt(parts[1]);
			}
			if (!parts[2].equals("*")) {
				day = Integer.parseInt(parts[2]);
			}
			if (!parts[3].equals("*")) {
				month = Integer.parseInt(parts[3]);
			}
			if (!parts[4].equals("*")) {
				weekday = Integer.parseInt(parts[4]);
			}
		}

		public boolean hasMinute() {
			return minute != UNSET_FIELD;
		}

		public int getMinute() {
			return minute;
		}

		public boolean hasHour() {
			return hour != UNSET_FIELD;
		}

		public int getHour() {
			return hour;
		}

		public boolean hasDay() {
			return day != UNSET_FIELD;
		}

		public int getDay() {
			return day;
		}

		public boolean hasMonth() {
			return month != UNSET_FIELD;
		}

		public int getMonth() {
			return month;
		}

		public boolean hasWeekday() {
			return weekday != UNSET_FIELD;
		}

		public int getWeekday() {
			return weekday;
		}
	}
}
