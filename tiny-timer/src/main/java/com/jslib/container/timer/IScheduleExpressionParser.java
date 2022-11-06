package com.jslib.container.timer;

import jakarta.ejb.Schedule;

/**
 * Expression parser for an element of the {@link Schedule} annotation. Schedule annotation has an element for every calendar
 * component: year, month, day and so on. These elements value are expressions that denotes one or more values; for example
 * expression can denote a specific hour or a range of hours.
 * 
 * There are different parsers for different calendar components - therefore schedule annotation elements, but all implements
 * this interface. The general usage pattern is:
 * <ul>
 * <li>get a parser implementation for a specific calendar component,
 * <li>parse expression and store values list; list can have only one value or can be empty,
 * <li>start with minimal value from list, then get next values till null.
 * </ul>
 * 
 * @author Iulian Rotaru
 */
interface IScheduleExpressionParser {

	/**
	 * Parse expression of the schedule element(s) specific to particular implementation and store resulting values list.
	 * Current calendar is used to constrain parsed expression; for example last day depends on current calendar month.
	 * 
	 * @param schedule schedule annotation,
	 * @param calendar current calendar value.
	 * @throws IllegalArgumentException if schedule contains invalid expression(s).
	 */
	void parse(Schedule schedule, CalendarEx calendar);

	/**
	 * Retrieve minimum from parsed values list. This method should be called after {@link #parse(Schedule, CalendarEx)}.
	 * 
	 * @return minimum value from values list.
	 * @throws IllegalStateException if invoke this method before expression parse.
	 */
	int getMinimumValue();

	/**
	 * Get next value from values list or null if no more values. This method should be called after
	 * {@link #parse(Schedule, CalendarEx)}. Given current value parameter should be obtained from a previous call to this
	 * method or from {@link #getMinimumValue()}.
	 * 
	 * @param currentValue current value.
	 * @return next value or null if end of list was reached.
	 * @throws IllegalStateException if invoke this method before expression parse.
	 */
	NextValue getNextValue(int currentValue);

}
