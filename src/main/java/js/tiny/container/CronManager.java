package js.tiny.container;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Schedule;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.util.Strings;

public final class CronManager {
	private static final Log log = LogFactory.getLog(CronManager.class);

	private final ScheduledExecutorService scheduler;

	public CronManager() {
		log.trace("CronManager()");
		scheduler = Executors.newScheduledThreadPool(10);
	}

	public void register(Object instance, ManagedMethodSPI managedMethod) {
		log.debug("Register crom method |%s|.", managedMethod);
		// computed remaining time can be zero in which case managed method is executed instantly
		scheduler.schedule(new CronTask(scheduler, instance, managedMethod), getTimeRemaining(managedMethod.getSchedule()), TimeUnit.MILLISECONDS);
	}

	/**
	 * Return remaining time, in milliseconds, to the next scheduler timeout or 0 if given schedule is passed. This method
	 * creates evaluation date to <code>now</code> and delegates {@link #getNextTimeout(CalendarEx, Schedule)}.
	 * 
	 * @param schedule method configured schedule.
	 * @return delay to the next scheduler timeout, in milliseconds, or zero if no schedule passed.
	 */
	private static long getTimeRemaining(Schedule schedule) {
		final CalendarEx now = new CalendarEx();

		Date next = getNextTimeout(now, schedule);
		if (next == null) {
			// schedule is passed and there are no more future events
			return 0;
		}

		long delay = (next.getTime() - now.getTimeInMillis());
		log.debug("Next execution date |%s|. Delay is |%d|", next.getTime(), delay);
		return delay;
	}

	/**
	 * Return next scheduler timeout or null if given schedule instance is passed. This method is designed to be invoked by
	 * {@link #getTimeRemaining(Schedule)} and does alter given <code>now</code> parameter.
	 * 
	 * @param now next timeout evaluation moment,
	 * @param schedule method configured schedule.
	 * @return next scheduler event or null if configured schedule is passed.
	 */
	static Date getNextTimeout(CalendarEx now, Schedule schedule) {
		// next scheduler event should be at least one second after the current one
		now.increment(CalendarEx.Unit.SECOND);

		CalendarEx next = now.clone();
		log.debug("Next timeout evaluation date: %s.", next.getTime());

		ScheduleExpressionParser parser = new ScheduleExpressionParser(schedule);
		// calendar units array is ordered from years to seconds, top down
		for (int i = 0; i < CalendarEx.UNITS.length; ++i) {
			CalendarEx.Unit unit = CalendarEx.UNITS[i];
			parser.parse(next, unit);
			if (next.after(now) && !next.isUpdated(unit)) {
				// reset to minimal acceptable value for specific unit but only if unit was not already set
				// keep in mind that this loop can be executed multiple times because of unit value overflow
				next.set(unit, parser.getMinimum(unit));
				continue;
			}
			ScheduleExpressionParser.NextValue nextValue = parser.nextValue(unit, next.get(unit));
			if (nextValue == null) {
				return null;
			}
			if (nextValue.overflow) {
				if (i == 0) {
					return null;
				}
				// increment upper unit value, reset current overflowing unit and restart evaluation loop back from years
				next.increment(CalendarEx.UNITS[i - 1]);
				next.set(unit, next.getActualMinimum(unit));
				i = 0;
				continue;
			}
			next.set(unit, nextValue.value);
		}

		Date date = next.getTime();
		log.debug("Computed next timeout: %s.", date);
		return date;
	}

	public void destroy() {
		log.trace("destroy()");
		scheduler.shutdownNow();
	}

	// --------------------------------------------------------------------------------------------

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
				long delay = getTimeRemaining(managedMethod.getSchedule());
				if (delay > 0) {
					scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
				}
			} catch (IllegalArgumentException | InvocationException | AuthorizationException e) {
				log.error(e);
			}
		}
	}

	/**
	 * Schedule expression parser. Schedule has expressions for each date unit, from years to seconds - see {@link Schedule}
	 * class description for expressions syntax. This parser converts schedule expressions into lists of values; values are
	 * stored in a map with date unit as key.
	 * <p>
	 * Once expression parsed for a given date unit we can obtain the next value - see {@link #nextValue(CalendarEx.Unit, int)},
	 * or get the minimum allowable value for a given date unit.
	 * <p>
	 * expression := wildcard | value | list | range | increment wildcard := '*' list := value (',' value)? range := value '-'
	 * value increment := start '/' increment value := number | label
	 * 
	 * 
	 * increment := start '/' amount start := wildcard | number amount := number wildcard := '0'
	 * 
	 * 
	 * day-of-month :=
	 * 
	 * @author Iulian Rotaru
	 */
	static class ScheduleExpressionParser {
		private final Map<Key, List<Integer>> cache = new HashMap<>();

		/** Lists of values parsed from schedule expression mapped by date unit. */
		private final Map<CalendarEx.Unit, List<Integer>> unitsValues = new HashMap<>();

		private final Schedule schedule;

		public ScheduleExpressionParser(Schedule schedule) {
			this.schedule = schedule;
		}

		/**
		 * Parse internal schedule expression and store results into units values field. Schedule expression to evaluate is
		 * identified by given calendar unit. This method delegates extract expression from {@link #schedule} and
		 * {@link #parseExpression(String, int, int)}. Minimum and maximum allowed values are loaded from calendar.
		 * 
		 * @Parma calendar current scheduler event calendar,
		 * @param unit calendar date unit.
		 * @return parsed values.
		 */
		public List<Integer> parse(CalendarEx calendar, CalendarEx.Unit unit) {
			int minimum = calendar.getActualMinimum(unit);
			int maximum = calendar.getActualMaximum(unit);

			Key key = new Key(unit, minimum, maximum);
			List<Integer> values = cache.get(key);

			if (values == null) {
				switch (unit) {
				case YEAR:
					values = parseExpression(schedule.year(), minimum, maximum);
					break;

				case MONTH:
					values = parseExpression(schedule.month(), minimum, maximum);
					break;

				case DAY:
					if (schedule.dayOfMonth().equals("*")) {
						if (schedule.dayOfWeek().equals("*")) {
							// if dayOfMonth == "*" and dayOfWeek == "*" uses month days
							values = parseExpression(schedule.dayOfMonth(), minimum, maximum);
						} else {
							// if dayOfMonth == "*" and dayOfWeek is concrete uses week days
							values = new ArrayList<>();
							for (Integer weekDay : parseExpression(schedule.dayOfWeek(), 0, 6)) {
								for (Integer monthDay : calendar.weekDayToMonthDays(weekDay)) {
									values.add(monthDay);
								}
							}
							Collections.sort(values);
						}
					} else {
						// if dayOfMonth is concrete uses month days
						values = parseExpression(schedule.dayOfMonth(), minimum, maximum);

						// if dayOfWeek is concrete merge week days to month days
						if (!schedule.dayOfWeek().equals("*")) {
							for (Integer weekDay : parseExpression(schedule.dayOfWeek(), 0, 6)) {
								for (Integer monthDay : calendar.weekDayToMonthDays(weekDay)) {
									if (!values.contains(monthDay)) {
										values.add(monthDay);
									}
								}
							}
							Collections.sort(values);
						}
					}
					break;

				case HOUR:
					values = parseExpression(schedule.hour(), minimum, maximum);
					break;

				case MINUTE:
					values = parseExpression(schedule.minute(), minimum, maximum);
					break;

				case SECOND:
					values = parseExpression(schedule.second(), minimum, maximum);
					break;

				default:
					throw new BugError("Unsupported calendar unit |%d|.", unit);
				}

				cache.put(key, values);
			}

			unitsValues.put(unit, values);
			return values;
		}

		/**
		 * Search unit values for first value greater that given current value. If none found return first value from list with
		 * overflow flag set.
		 * 
		 * @param unit calendar unit.
		 * @param currentValue current value.
		 * @return next value from unit values list.
		 */
		public NextValue nextValue(CalendarEx.Unit unit, int currentValue) {
			List<Integer> values = unitsValues.get(unit);
			for (int value : values) {
				if (value >= currentValue) {
					return new NextValue(value, false);
				}
			}

			return new NextValue(values.get(0), true);
		}

		/**
		 * Gets minimum acceptable value for given unit. This method should be called after unit values initialization performed
		 * by {@link #parse(CalendarEx, CalendarEx.Unit)}.
		 * 
		 * @param unit calendar unit.
		 * @return first item from unit values list.
		 * @throws IllegalStateException if unit values are not properly initialized.
		 */
		public int getMinimum(CalendarEx.Unit unit) {
			List<Integer> values = unitsValues.get(unit);
			if (values.isEmpty()) {
				throw new IllegalStateException("Empty values for calendar unit " + unit);
			}
			return values.get(0);
		}

		// ----------------------------------------------------------------------------------------

		static List<Integer> parseExpression(String expression, int minimum, int maximum) {
			expression = expression.trim();
			List<Integer> values = new ArrayList<>();

			if (expression.equals("*")) {
				for (int i = minimum; i <= maximum; ++i) {
					values.add(i);
				}
				return values;
			}

			if (expression.contains(",")) {
				for (String itemValue : expression.split(",")) {
					parseNumbers(values, itemValue.trim(), minimum, maximum);
				}
				return values;
			}

			int slashPosition = expression.indexOf('/');
			if (slashPosition > 0) {
				int start = parseNumber(expression.substring(0, slashPosition).trim(), minimum, maximum);
				int increment = parseNumber(expression.substring(slashPosition + 1).trim(), minimum, maximum);
				for (int value = start; value <= maximum; value += increment) {
					values.add(value);
				}
				return values;
			}

			parseNumbers(values, expression, minimum, maximum);
			return values;
		}

		private static void parseNumbers(List<Integer> values, String expression, int minimum, int maximum) {
			int dashPosition = expression.indexOf('-');
			switch (dashPosition) {
			case -1:
				values.add(parseNumber(expression, minimum, maximum));
				break;

			case 0:
				values.add(maximum + parseNumber(expression, minimum, maximum));
				break;

			default:
				int start = parseNumber(expression.substring(0, dashPosition), minimum, maximum);
				int end = parseNumber(expression.substring(dashPosition + 1), minimum, maximum);

				if (start < end) {
					for (int i = start; i <= end; ++i) {
						values.add(i);
					}
				} else if (start > end) {
					// add lower sub-range first
					for (int i = minimum; i <= end; ++i) {
						values.add(i);
					}
					for (int i = start; i <= maximum; ++i) {
						values.add(i);
					}
				} else {
					values.add(start);
				}
			}
		}

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
			ALIASES.put("sun", 0);
			ALIASES.put("mon", 1);
			ALIASES.put("tue", 2);
			ALIASES.put("wed", 3);
			ALIASES.put("thu", 4);
			ALIASES.put("fri", 5);
			ALIASES.put("sat", 6);
		}

		private static String[] ORDINALS = new String[] { "1st", "2nd", "3rd", "4th", "5th" };

		static int parseNumber(String expression, int minimum, int maximum) {
			if (expression.equals("*")) {
				return minimum;
			}
			if (expression.equalsIgnoreCase("Last")) {
				return maximum;
			}

			int factor = 0;
			for (int i = 0; i < ORDINALS.length; ++i) {
				String ordinal = ORDINALS[i];
				if (expression.contains(ordinal)) {
					factor = (i + 1) * 10;
					expression = expression.replace(ordinal, "").trim();
				}
			}

			Integer number = ALIASES.get(expression.toLowerCase());
			if (number == null) {
				number = Integer.parseInt(expression);
			}
			if (factor > 0) {
				number *= factor;
			}
			return number;
		}

		// ----------------------------------------------------------------------------------------

		/**
		 * Next value from unit values returned by {@link #nextValue(CalendarEx.Unit, int)}. Beside the actual value this class
		 * holds a flag about value overflowing. Value overflow occurs when next value is greater maximum allowed value in which
		 * case returned value is the minimal one.
		 * 
		 * @author Iulian Rotaru
		 */
		static class NextValue {
			int value;
			boolean overflow;

			public NextValue(int value, boolean overflow) {
				this.value = value;
				this.overflow = overflow;
			}

			@Override
			public String toString() {
				return Strings.toString(value, overflow);
			}
		}

		private static class Key {
			private final CalendarEx.Unit unit;
			private final int minimum;
			private final int maximum;

			public Key(CalendarEx.Unit unit, int minimum, int maximum) {
				this.unit = unit;
				this.minimum = minimum;
				this.maximum = maximum;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + maximum;
				result = prime * result + minimum;
				result = prime * result + ((unit == null) ? 0 : unit.hashCode());
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Key other = (Key) obj;
				if (maximum != other.maximum)
					return false;
				if (minimum != other.minimum)
					return false;
				if (unit != other.unit)
					return false;
				return true;
			}

			@Override
			public String toString() {
				return Strings.toString(unit, minimum, maximum);
			}
		}
	}

	public static class CalendarEx {

		public enum Unit {
			YEAR(Calendar.YEAR), MONTH(Calendar.MONTH), DAY(Calendar.DAY_OF_MONTH), HOUR(Calendar.HOUR_OF_DAY), MINUTE(Calendar.MINUTE), SECOND(Calendar.SECOND);

			private int value;

			private Unit(int value) {
				this.value = value;
			}
		}

		public static final Unit[] UNITS = new Unit[] { Unit.YEAR, Unit.MONTH, Unit.DAY, Unit.HOUR, Unit.MINUTE, Unit.SECOND };
		public static final int[] WEEK_DAYS = new int[] { Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY };

		private final Map<Unit, Boolean> updateState = new HashMap<>();
		{
			updateState.put(Unit.YEAR, false);
			updateState.put(Unit.MONTH, false);
			updateState.put(Unit.DAY, false);
			updateState.put(Unit.HOUR, false);
			updateState.put(Unit.MINUTE, false);
			updateState.put(Unit.SECOND, false);
		}

		private final Calendar calendar;

		public CalendarEx() {
			this.calendar = Calendar.getInstance();
			this.calendar.set(Calendar.MILLISECOND, 0);
		}

		public CalendarEx(int year, int month, int day, int hour, int minute, int second) {
			this.calendar = Calendar.getInstance();
			this.calendar.set(Calendar.MILLISECOND, 0);

			set(Unit.YEAR, year);
			set(Unit.MONTH, month);
			set(Unit.DAY, day);
			set(Unit.HOUR, hour);
			set(Unit.MINUTE, minute);
			set(Unit.SECOND, second);
		}

		public CalendarEx(Date date) {
			this.calendar = Calendar.getInstance();
			this.calendar.setTime(date);
			this.calendar.set(Calendar.MILLISECOND, 0);
		}

		public int get(Unit unit) {
			int value = calendar.get(unit.value);
			if (unit == Unit.MONTH) {
				// scheduler first month is 1 whereas on internal calendar is 0
				++value;
			}
			return value;
		}

		public void set(Unit unit, int value) {
			updateState.put(unit, true);
			if (unit == Unit.MONTH) {
				// scheduler first month is 1 whereas on internal calendar is 0
				--value;
			}
			calendar.set(unit.value, value);
		}

		public boolean isUpdated(Unit unit) {
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

		public int getActualMinimum(Unit unit) {
			switch (unit) {
			case YEAR:
				return calendar.get(Calendar.YEAR);

			case MONTH:
				// scheduler first month is 1 whereas on internal calendar is 0
				return calendar.getActualMinimum(unit.value) + 1;

			default:
				return calendar.getActualMinimum(unit.value);
			}
		}

		public int getActualMaximum(Unit unit) {
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

		public void increment(Unit unit) {
			calendar.add(unit.value, 1);
		}

		/**
		 * Convert week day to current month days. When this method is invoked internal calendar year and month should be
		 * properly initialized.
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

			int ordinal = weekDay / 10;
			System.out.println(ordinal);
			return monthDays;
		}

		@Override
		public String toString() {
			Object[] args = new Object[UNITS.length];
			for (int i = 0; i < UNITS.length; ++i) {
				args[i] = get(UNITS[i]);
			}
			return String.format("%04d-%02d-%02d %02d:%02d:%02d", args);
		}

		public boolean equals(int... values) {
			if (UNITS.length != values.length) {
				return false;
			}
			for (int i = 0; i < UNITS.length; ++i) {
				if (get(UNITS[i]) != values[i]) {
					return false;
				}
			}
			return true;
		}
	}
}
