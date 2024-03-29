package com.jslib.container.timer;

import com.jslib.util.Params;

import jakarta.ejb.Schedule;

class NumericExpressionParser extends BaseExpressionParser {
	private final CalendarUnit calendarUnit;

	public NumericExpressionParser(CalendarUnit calendarUnit) {
		super();
		this.calendarUnit = calendarUnit;
	}

	@Override
	public void parse(Schedule schedule, CalendarEx calendar) {
		Params.notNull(schedule, "Schedule");
		Params.notNull(calendar, "Calendar");

		String expression = null;
		switch (calendarUnit) {
		case SECOND:
			expression = schedule.second();
			break;

		case MINUTE:
			expression = schedule.minute();
			break;

		case HOUR:
			expression = schedule.hour();
			break;

		case YEAR:
			expression = schedule.year();
			break;

		default:
			throw new IllegalStateException("Not supported calendar unit: " + calendarUnit);
		}

		int minimum = calendar.getActualMinimum(calendarUnit);
		int maximum = calendar.getActualMaximum(calendarUnit);

		ExpressionParser expressionParser = new ExpressionParser();
		values = expressionParser.parseExpression(expression, minimum, maximum);
	}
}
