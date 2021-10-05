package js.tiny.container.timer;

import javax.ejb.Schedule;

import js.tiny.container.spi.IContainerServiceMeta;

class ScheduleMeta implements IContainerServiceMeta {
	private final String second;
	private final String minute;
	private final String hour;
	private final String dayOfMonth;
	private final String dayOfWeek;
	private final String month;
	private final String year;

	public ScheduleMeta(Schedule schedule) {
		this.second = schedule.second();
		this.minute = schedule.minute();
		this.hour = schedule.hour();
		this.dayOfMonth = schedule.dayOfMonth();
		this.dayOfWeek = schedule.dayOfWeek();
		this.month = schedule.month();
		this.year = schedule.year();
	}

	public String second() {
		return second;
	}

	public String minute() {
		return minute;
	}

	public String hour() {
		return hour;
	}

	public String dayOfMonth() {
		return dayOfMonth;
	}

	public String dayOfWeek() {
		return dayOfWeek;
	}

	public String month() {
		return month;
	}

	public String year() {
		return year;
	}
}
