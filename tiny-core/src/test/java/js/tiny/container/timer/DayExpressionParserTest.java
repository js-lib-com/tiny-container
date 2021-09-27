package js.tiny.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.SortedSet;

import javax.ejb.Schedule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DayExpressionParserTest {
	@Mock
	private Schedule schedule;

	private CalendarEx now;
	private DayExpressionParser parser;

	@Before
	public void beforeTest() {
		when(schedule.dayOfMonth()).thenReturn("*");
		when(schedule.dayOfWeek()).thenReturn("*");

		now = new CalendarEx(2020, 1, 1, 0, 0, 0);
		parser = new DayExpressionParser();
	}

	@Test
	@Schedule()
	public void GivenDefaultSchedule_WhenParseDay_ThenUnitsValues() {
		// given

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(31));
		assertThat(values, hasItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31));
	}

	@Test
	@Schedule(dayOfMonth = "15")
	public void GivenSingleDayOfMonth_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("15");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, contains(15));
	}

	@Test
	@Schedule(dayOfMonth = "15, 20, 25")
	public void GivenDaysOfMonthList_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("15, 20, 25");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(15, 20, 25));
	}

	@Test
	@Schedule(dayOfMonth = "15-20")
	public void GivenDaysOfMonthRange_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("15-20");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(6));
		assertThat(values, hasItems(15, 16, 17, 18, 19, 20));
	}

	@Test
	@Schedule(dayOfMonth = "30-2")
	public void GivenDaysOfMonthRangeOverflow_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("30-2");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(1, 2, 30, 31));
	}

	@Test
	@Schedule(dayOfMonth = "15/5")
	public void GivenDaysOfMonthIncrement_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("15/5");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(15, 20, 25, 30));
	}

	/** Day of month increment is not supported by JEE. */
	@Test
	@Schedule(dayOfMonth = "*/5")
	public void GivenDaysOfMonthWildcardIncrement_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("*/5");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(7));
		assertThat(values, hasItems(1, 6, 11, 16, 21, 26, 31));
	}

	/** List with increment is not supported by JEE. */
	@Test(expected = IllegalArgumentException.class)
	@Schedule(dayOfMonth = "1, 2, */10")
	public void GivenDaysOfMonthListWithIncrement_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("1, 2, */10");

		// when
		parser.parse(schedule, now);

		// then
	}

	@Test
	@Schedule(dayOfMonth = "Last")
	public void GivenLastDayOfMonth_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("Last");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(31));
	}

	@Test
	@Schedule(dayOfMonth = "last")
	public void GivenLastDayOfMonthLowerCase_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("last");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(31));
	}

	@Test
	@Schedule(dayOfMonth = "-1")
	public void GivenBeforeLastDayOfMonth_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("-1");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(30));
	}

	@Test
	@Schedule(dayOfMonth = "-7")
	public void GivenSevenDaysBeforeLastDayOfMonth_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("-7");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(24));
	}

	@Test
	@Schedule(dayOfMonth = "-3 - -1")
	public void GivenRangeOfLastMontDays_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("-3 - -1");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(28, 29, 30));
	}

	@Test
	@Schedule(dayOfMonth = "-3--1")
	public void GivenRangeOfLastMontDaysNoSpaces_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("-3--1");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(28, 29, 30));
	}

	@Test
	@Schedule(dayOfMonth = "28--1")
	public void GivenMixedRangeOfLastMontDays_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("28--1");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(28, 29, 30));
	}

	@Test
	@Schedule(dayOfMonth = "1-3, 15, -1, last")
	public void GivenLastDayOfMonthComplexList_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("1-3, 15, -1, last");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(6));
		assertThat(values, hasItems(1, 2, 3, 15, 30, 31));
	}

	@Test
	@Schedule(dayOfMonth = "Last")
	public void GivenLastDayOfFebruary_WhenParseDay_ThenUnitsValues() {
		// given
		now.set(CalendarUnit.MONTH, 2);
		when(schedule.dayOfMonth()).thenReturn("Last");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(29));
	}

	@Test
	@Schedule(dayOfMonth = "2nd Fri")
	public void GivenSecondFriday_WhenParseDay_ThenSingleDay() {
		// given
		when(schedule.dayOfMonth()).thenReturn("2nd Fri");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(10));
	}

	@Test(expected = IllegalArgumentException.class)
	@Schedule(dayOfMonth = "2rd Fri")
	public void GivenInvalidOrdinal_WhenParseDayOfWeekOnMonth_ThenException() {
		// given
		when(schedule.dayOfMonth()).thenReturn("2rd Fri");

		// when
		parser.parse(schedule, now);

		// then
	}

	@Test(expected = IllegalArgumentException.class)
	@Schedule(dayOfMonth = "2nd Fry")
	public void GivenInvalidWeekDay_WhenParseDayOfWeekOnMonth_ThenException() {
		// given
		when(schedule.dayOfMonth()).thenReturn("2nd Fry");

		// when
		parser.parse(schedule, now);

		// then
	}

	@Test
	@Schedule(dayOfMonth = "2nd Fri, 4th Fri")
	public void GivenListOfMonthWeekDays_WhenParseDay_ThenSingleDay() {
		// given
		when(schedule.dayOfMonth()).thenReturn("2nd Fri, 4th Fri");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(2));
		assertThat(values, hasItems(10, 24));
	}

	@Test
	@Schedule(dayOfMonth = "1st Fri - 1st Sun")
	public void GivenRangeOfMonthWeekDays_WhenParseDay_ThenDaysSequence() {
		// given
		when(schedule.dayOfMonth()).thenReturn("1st Fri - 1st Sun");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(3, 4, 5));
	}

	@Test
	@Schedule(dayOfMonth = "1st Fri-1st Sun")
	public void GivenRangeOfMonthWeekDaysNoSpaces_WhenParseDay_ThenDaysSequence() {
		// given
		when(schedule.dayOfMonth()).thenReturn("1st Fri-1st Sun");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(3, 4, 5));
	}

	@Test
	@Schedule(dayOfWeek = "Fri")
	public void GivenSingleDayOfWeek_WhenParseDay_ThenAllWeeksDay() {
		// given
		when(schedule.dayOfWeek()).thenReturn("Fri");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(5));
		assertThat(values, hasItems(3, 10, 17, 24, 31));
	}

	@Test
	@Schedule(dayOfWeek = "Mon-Fri")
	public void GivenDayOfWeekRange_WhenParseDay_ThenAllWeeksDays() {
		// given
		when(schedule.dayOfWeek()).thenReturn("Mon-Fri");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(23));
		assertThat(values, hasItems(1, 2, 3, 6, 7, 8, 9, 10, 13, 14, 15, 16, 17, 20, 21, 22, 23, 24, 27, 28, 29, 30, 31));
	}

	@Test
	@Schedule(dayOfWeek = "Fri-Mon")
	public void GivenDayOfWeekRangeOverflow_WhenParseDay_ThenAllSortedWeeksDays() {
		// given
		when(schedule.dayOfWeek()).thenReturn("Fri-Mon");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(17));
		assertThat(values, hasItems(3, 4, 5, 6, 10, 11, 12, 13, 17, 18, 19, 20, 24, 25, 26, 27, 31));
	}

	@Test
	@Schedule(dayOfMonth = "1", dayOfWeek = "Fri")
	public void GivenSingleDayOfMonthAndDayOfWeek_WhenParseDay_ThenUnitsValues() {
		// given
		when(schedule.dayOfMonth()).thenReturn("1");
		when(schedule.dayOfWeek()).thenReturn("Fri");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(6));
		assertThat(values, hasItems(1, 3, 10, 17, 24, 31));
	}

	@Test
	public void GivenInvalidDayOfMonth_WhenParse_ThenIllegalArgument() {
		String[] invalidExpressions = { null, "", " ", "0.1", "1d", "1.0", "?", "%", "$", "!", "&", "-", "/", ",", ".", "1-", "1-2-3", "1+2", "**", "*-", "*,1", "1,*", "5/*", "1, 2/2", "---", "-", "--", " -2 -3 -4", "-31", "1--" };
		for (String expression : invalidExpressions) {
			try {
				when(schedule.dayOfMonth()).thenReturn(expression);
				parser.parse(schedule, now);
				Assert.fail("Invalid expression should throw IllegalArgumentException: " + expression);
			} catch (IllegalArgumentException expected) {
			}
		}
	}
}
