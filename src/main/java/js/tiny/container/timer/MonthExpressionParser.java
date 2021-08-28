package js.tiny.container.timer;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import javax.ejb.Schedule;

import js.log.Log;
import js.log.LogFactory;
import js.util.Params;

class MonthExpressionParser extends BaseExpressionParser {
	private static final Log log = LogFactory.getLog(MonthExpressionParser.class);

	private static final Map<String, Integer> ALIASES = new HashMap<>();
	static {
		ALIASES.put("jan", 1);
		ALIASES.put("feb", 2);
		ALIASES.put("mar", 3);
		ALIASES.put("apr", 4);
		ALIASES.put("may", 5);
		ALIASES.put("jun", 6);
		ALIASES.put("jul", 7);
		ALIASES.put("aug", 8);
		ALIASES.put("sep", 9);
		ALIASES.put("oct", 10);
		ALIASES.put("nov", 11);
		ALIASES.put("dec", 12);
	}

	public MonthExpressionParser() {
		super();
		log.trace("MonthExpressionParser()");
	}

	@Override
	public SortedSet<Integer> parse(Schedule schedule, CalendarEx calendar) {
		log.trace("parse(schedule, calendar)");
		Params.notNull(schedule, "Schedule");
		Params.notNull(calendar, "Calendar");

		int minimum = calendar.getActualMinimum(CalendarUnit.MONTH);
		int maximum = calendar.getActualMaximum(CalendarUnit.MONTH);

		ITextualParser textualParser = value -> ALIASES.get(value.toLowerCase());
		ExpressionParser expressionParser = new ExpressionParser(textualParser);
		values = expressionParser.parseExpression(schedule.month(), minimum, maximum);
		return values;
	}
}
