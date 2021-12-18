package js.tiny.container.timer;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>ejb.Schedule</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface ISchedule {

	String second();

	String minute();

	String hour();

	String dayOfMonth();

	String month();

	String dayOfWeek();

	String year();

	static ISchedule scan(IManagedMethod managedMethod) {
		jakarta.ejb.Schedule jakartaSchedule = managedMethod.scanAnnotation(jakarta.ejb.Schedule.class);
		if (jakartaSchedule != null) {
			return new JakartaSchedule(jakartaSchedule);
		}

		javax.ejb.Schedule javaxSchedule = managedMethod.scanAnnotation(javax.ejb.Schedule.class);
		if (javaxSchedule != null) {
			return new JavaxSchedule(javaxSchedule);
		}

		return null;
	}

	static class JakartaSchedule implements ISchedule {
		private final jakarta.ejb.Schedule schedule;

		public JakartaSchedule(jakarta.ejb.Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public String second() {
			return schedule.second();
		}

		@Override
		public String minute() {
			return schedule.minute();
		}

		@Override
		public String hour() {
			return schedule.hour();
		}

		@Override
		public String dayOfMonth() {
			return schedule.dayOfMonth();
		}

		@Override
		public String month() {
			return schedule.month();
		}

		@Override
		public String dayOfWeek() {
			return schedule.dayOfWeek();
		}

		@Override
		public String year() {
			return schedule.year();
		}
	}

	static class JavaxSchedule implements ISchedule {
		private final javax.ejb.Schedule schedule;

		public JavaxSchedule(javax.ejb.Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public String second() {
			return schedule.second();
		}

		@Override
		public String minute() {
			return schedule.minute();
		}

		@Override
		public String hour() {
			return schedule.hour();
		}

		@Override
		public String dayOfMonth() {
			return schedule.dayOfMonth();
		}

		@Override
		public String month() {
			return schedule.month();
		}

		@Override
		public String dayOfWeek() {
			return schedule.dayOfWeek();
		}

		@Override
		public String year() {
			return schedule.year();
		}
	}
}
