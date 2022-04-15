package js.tiny.container.timer;

import java.util.SortedSet;

import jakarta.ejb.Schedule;

interface IScheduleExpressionParser {

	/**
	 * 
	 * @param schedule
	 * @param calendar
	 * @return
	 * @throws IllegalArgumentException if schedule contains invalid expression(s).
	 */
	SortedSet<Integer> parse(Schedule schedule, CalendarEx calendar);

	/**
	 * 
	 * @return
	 * @throws IllegalStateException if invoke this method before expression parse.
	 */
	int getMinimumValue();

	/**
	 * 
	 * @param currentValue
	 * @return
	 * @throws IllegalStateException if invoke this method before expression parse.
	 */
	NextValue getNextValue(int currentValue);

}
