package js.tiny.container.timer;

import java.util.SortedSet;

import javax.ejb.Schedule;

import js.log.Log;
import js.log.LogFactory;
import js.util.Params;

class NumericExpressionParser extends BaseExpressionParser {
	private static final Log log = LogFactory.getLog(NumericExpressionParser.class);

	private final CalendarUnit calendarUnit;

	public NumericExpressionParser(CalendarUnit calendarUnit) {
		super();
		log.trace("NumericExpressionParser(calendarUnit)");
		this.calendarUnit = calendarUnit;
	}

	@Override
	public SortedSet<Integer> parse(Schedule schedule, CalendarEx calendar) {
		log.trace("parse(schedule, calendar)");
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
		return values;
	}
}
