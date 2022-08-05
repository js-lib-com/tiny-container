package com.jslib.container.timer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import jakarta.ejb.Schedule;
import com.jslib.util.Params;

class DayExpressionParser extends BaseExpressionParser {
	private static final Map<String, Integer> WEEK_DAYS = new HashMap<>();
	static {
		WEEK_DAYS.put("sun", 0);
		WEEK_DAYS.put("mon", 1);
		WEEK_DAYS.put("tue", 2);
		WEEK_DAYS.put("wed", 3);
		WEEK_DAYS.put("thu", 4);
		WEEK_DAYS.put("fri", 5);
		WEEK_DAYS.put("sat", 6);
	}

	private static final Map<String, Integer> ORDINALS = new HashMap<>();
	static {
		ORDINALS.put("1st", 1);
		ORDINALS.put("2nd", 2);
		ORDINALS.put("3rd", 3);
		ORDINALS.put("4th", 4);
		ORDINALS.put("5th", 5);
	}

	@Override
	public SortedSet<Integer> parse(Schedule schedule, CalendarEx calendar) {
		Params.notNull(calendar, "Calendar");

		String dayOfMonth = schedule.dayOfMonth();
		Params.notNullOrEmpty(dayOfMonth, "Day of month");

		String dayOfWeek = schedule.dayOfWeek();
		Params.notNullOrEmpty(dayOfWeek, "Day of week");

		values = new TreeSet<>();
		if (schedule.dayOfMonth().equals("*")) {
			if (schedule.dayOfWeek().equals("*")) {
				// if dayOfMonth == "*" and dayOfWeek == "*" uses month days
				parseDayOfMonth(schedule.dayOfMonth(), calendar);
			} else {
				// if dayOfMonth == "*" and dayOfWeek is concrete uses week days
				parseDayOfWeek(schedule.dayOfWeek(), calendar);
			}
		} else {
			// if dayOfMonth is concrete uses month days
			parseDayOfMonth(schedule.dayOfMonth(), calendar);

			// if dayOfWeek is concrete merge week days to month days
			if (!schedule.dayOfWeek().equals("*")) {
				parseDayOfWeek(schedule.dayOfWeek(), calendar);
			}
		}

		return values;
	}

	private void parseDayOfMonth(String expression, CalendarEx calendar) {
		int minimum = calendar.getActualMinimum(CalendarUnit.DAY);
		int maximum = calendar.getActualMaximum(CalendarUnit.DAY);

		ITextualParser textualParser = new ITextualParser() {
			@Override
			public Integer parseText(String value) {
				// -1
				if (value.startsWith("-")) {
					return maximum + Integer.parseInt(value);
				}

				// last
				if (value.equalsIgnoreCase("last")) {
					return maximum;
				}

				// 1st Mon
				Pattern pattern = Pattern.compile("\\s+");
				String[] parts = pattern.split(value);
				if (parts.length != 2) {
					return null;
				}

				Integer ordinal = ORDINALS.get(parts[0].toLowerCase(Locale.ENGLISH));
				if (ordinal == null) {
					return null;
				}
				Integer weekDay = WEEK_DAYS.get(parts[1].toLowerCase(Locale.ENGLISH));
				if (weekDay == null) {
					return null;
				}
				return calendar.weekDayToMonthDay(ordinal, weekDay);
			}
		};

		ExpressionParser expressionParser = new ExpressionParser(textualParser);
		values.addAll(expressionParser.parseExpression(expression, minimum, maximum));
	}

	private void parseDayOfWeek(String expression, CalendarEx calendar) {
		ITextualParser textualParser = value -> WEEK_DAYS.get(value.toLowerCase(Locale.ENGLISH));
		ExpressionParser expressionParser = new ExpressionParser(textualParser);
		for (Integer weekDay : expressionParser.parseExpression(expression, 0, 6)) {
			values.addAll(calendar.weekDayToMonthDays(weekDay));
		}
	}
}
