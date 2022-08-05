package com.jslib.tiny.container.timer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CalendarEx {

	private static final int[] WEEK_DAYS = new int[] { Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY };

	private final Map<CalendarUnit, Boolean> updateState = new HashMap<>();
	{
		updateState.put(CalendarUnit.YEAR, false);
		updateState.put(CalendarUnit.MONTH, false);
		updateState.put(CalendarUnit.DAY, false);
		updateState.put(CalendarUnit.HOUR, false);
		updateState.put(CalendarUnit.MINUTE, false);
		updateState.put(CalendarUnit.SECOND, false);
	}

	private final Calendar calendar;

	public CalendarEx() {
		this.calendar = Calendar.getInstance();
		this.calendar.set(Calendar.MILLISECOND, 0);
	}

	public CalendarEx(int year, int month, int day, int hour, int minute, int second) {
		this.calendar = Calendar.getInstance();
		this.calendar.set(Calendar.MILLISECOND, 0);

		set(CalendarUnit.YEAR, year);
		set(CalendarUnit.MONTH, month);
		set(CalendarUnit.DAY, day);
		set(CalendarUnit.HOUR, hour);
		set(CalendarUnit.MINUTE, minute);
		set(CalendarUnit.SECOND, second);
	}

	public CalendarEx(Date date) {
		this.calendar = Calendar.getInstance();
		this.calendar.setTime(date);
		this.calendar.set(Calendar.MILLISECOND, 0);
	}

	public int get(CalendarUnit unit) {
		int value = calendar.get(unit.value);
		if (unit == CalendarUnit.MONTH) {
			// scheduler first month is 1 whereas on internal calendar is 0
			++value;
		}
		return value;
	}

	public void set(CalendarUnit unit, int value) {
		updateState.put(unit, true);
		if (unit == CalendarUnit.MONTH) {
			// scheduler first month is 1 whereas on internal calendar is 0
			--value;
		}
		calendar.set(unit.value, value);
	}

	public boolean isUpdated(CalendarUnit unit) {
		return updateState.get(unit);
	}

	public long getTimeInMillis() {
		return calendar.getTimeInMillis();
	}

	public Date getTime() {
		return calendar.getTime();
	}

	public CalendarEx clone() {
		CalendarEx calendarEx = new CalendarEx();
		calendarEx.calendar.setTimeInMillis(calendar.getTimeInMillis());
		return calendarEx;
	}

	public int getActualMinimum(CalendarUnit unit) {
		switch (unit) {
		case YEAR:
			return calendar.get(Calendar.YEAR) - 10;

		case MONTH:
			// scheduler first month is 1 whereas on internal calendar is 0
			return calendar.getActualMinimum(unit.value) + 1;

		default:
			return calendar.getActualMinimum(unit.value);
		}
	}

	public int getActualMaximum(CalendarUnit unit) {
		switch (unit) {
		case YEAR:
			return calendar.get(Calendar.YEAR) + 10;

		case MONTH:
			// scheduler last month is 12 whereas on internal calendar is 11
			return calendar.getActualMaximum(unit.value) + 1;

		default:
			return calendar.getActualMaximum(unit.value);
		}
	}

	public boolean after(CalendarEx other) {
		return this.calendar.after(other.calendar);
	}

	public void increment(CalendarUnit unit) {
		calendar.add(unit.value, 1);
	}

	/**
	 * Convert week day to current month days. When this method is invoked internal calendar year and month should be properly
	 * initialized.
	 * 
	 * @param weekDay week day number, Sunday 0.
	 * @return list of days from current month with requested week day name.
	 */
	public List<Integer> weekDayToMonthDays(Integer weekDay) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, this.calendar.get(Calendar.YEAR));
		calendar.set(Calendar.MONTH, this.calendar.get(Calendar.MONTH));

		List<Integer> monthDays = new ArrayList<>();

		calendar.set(Calendar.DAY_OF_WEEK, WEEK_DAYS[weekDay % 7]);
		// for a given week day calendar.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH) can be 5 or 4 depending on current
		// month week days spreading
		for (int i = 1; i <= calendar.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH); ++i) {
			calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, i);
			monthDays.add(calendar.get(Calendar.DAY_OF_MONTH));
		}
		return monthDays;
	}

	public Integer weekDayToMonthDay(Integer dayOfWeekInMonth, Integer dayOfWeek) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, this.calendar.get(Calendar.YEAR));
		calendar.set(Calendar.MONTH, this.calendar.get(Calendar.MONTH));
		calendar.set(Calendar.DAY_OF_WEEK, WEEK_DAYS[dayOfWeek % 7]);

		if (dayOfWeekInMonth < calendar.getActualMinimum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
			return null;
		}
		if (dayOfWeekInMonth > calendar.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)) {
			return null;
		}

		calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
		return calendar.get(Calendar.DAY_OF_MONTH);
	}

	@Override
	public String toString() {
		// this algorithm depends on calendar units order; it is assumed from years to seconds
		Object[] args = new Object[CalendarUnit.length()];
		for (int i = 0; i < args.length; ++i) {
			args[i] = get(CalendarUnit.get(i));
		}
		return String.format("%04d-%02d-%02d %02d:%02d:%02d", args);
	}

	public boolean equals(int... values) {
		if (CalendarUnit.length() != values.length) {
			return false;
		}
		// this algorithm depends on calendar units order; it is assumed from years to seconds
		for (int i = 0; i < CalendarUnit.length(); ++i) {
			if (get(CalendarUnit.get(i)) != values[i]) {
				return false;
			}
		}
		return true;
	}
}
