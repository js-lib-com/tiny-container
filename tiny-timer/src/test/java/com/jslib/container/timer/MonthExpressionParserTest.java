package com.jslib.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ejb.Schedule;

@RunWith(MockitoJUnitRunner.class)
public class MonthExpressionParserTest {
	@Mock
	private Schedule schedule;

	private CalendarEx now;
	private MonthExpressionParser parser;

	@Before
	public void beforeTest() {
		when(schedule.month()).thenReturn("*");

		now = new CalendarEx(2020, 1, 1, 0, 0, 0);
		parser = new MonthExpressionParser();
	}

	@Test
	@Schedule()
	public void GivenDefaultSchedule_WhenParseDay_ThenUnitsValues() {
		// given

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(12));
		assertThat(values, hasItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
	}

	@Test
	@Schedule(month = "3")
	public void GivenSingleMonth_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("3");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(3));
	}

	@Test
	@Schedule(month = "Mar")
	public void GivenSingleMonthName_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("Mar");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(3));
	}

	@Test
	@Schedule(month = "mar")
	public void GivenSingleMonthNameLowerCase_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("mar");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(3));
	}

	@Test
	@Schedule(month = "3, 6")
	public void GivenMonthsList_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("3, 6");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(2));
		assertThat(values, hasItems(3, 6));
	}

	@Test
	@Schedule(month = "Mar, Jun")
	public void GivenMonthNamesList_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("Mar, Jun");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(2));
		assertThat(values, hasItems(3, 6));
	}

	@Test
	@Schedule(month = "mar, jun")
	public void GivenMonthNamesLowerCaseList_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("mar, jun");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(2));
		assertThat(values, hasItems(3, 6));
	}

	@Test
	@Schedule(month = "3-6")
	public void GivenMonthsRange_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("3-6");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(3, 4, 5, 6));
	}

	@Test
	@Schedule(month = "Mar-Jun")
	public void GivenMonthNamesRange_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("Mar-Jun");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(3, 4, 5, 6));
	}

	@Test
	@Schedule(month = "mar-jun")
	public void GivenMonthNamesLowerCaseRange_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("mar-jun");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(3, 4, 5, 6));
	}

	@Test
	@Schedule(month = "3/3")
	public void GivenMonthsIncrement_WhenParseMonth_ThenUnitsValues() {
		// given
		when(schedule.month()).thenReturn("3/3");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(3, 6, 9, 12));
	}

	@Test
	@Schedule(month = "*/3")
	public void GivenMonthsWildcardIncrement_WhenParseMonth_ThenStartsWithOne() {
		// given
		when(schedule.month()).thenReturn("*/3");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(4));
		assertThat(values, hasItems(1, 4, 7, 10));
	}
}
