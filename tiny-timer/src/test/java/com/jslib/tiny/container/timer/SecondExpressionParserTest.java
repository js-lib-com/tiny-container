package com.jslib.tiny.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ejb.Schedule;

@RunWith(MockitoJUnitRunner.class)
public class SecondExpressionParserTest {
	@Mock
	private Schedule schedule;

	private CalendarEx now;
	private NumericExpressionParser parser;

	@Before
	public void beforeTest() {
		when(schedule.second()).thenReturn("0");

		now = new CalendarEx(2020, 1, 1, 0, 0, 0);
		parser = new NumericExpressionParser(CalendarUnit.SECOND);
	}

	@Test
	@Schedule()
	public void GivenDefaultSchedule_WhenParse_ThenDefaultValue() {
		// given

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(0));
	}

	@Test
	@Schedule(second = "*")
	public void GivenWidlcardValue_WhenParse_ThenAll60Seconds() {
		// given
		when(schedule.second()).thenReturn("*");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(60));
		assertThat(values, hasItems(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 28, 29, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 3, 8, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59));
	}

	@Test
	@Schedule(second = "*")
	public void GivenWidlcardValue_WhenGetMinimumValue_ThenZero() {
		// given
		when(schedule.second()).thenReturn("*");
		parser.parse(schedule, now);

		// when
		int minimum = parser.getMinimumValue();

		// then
		assertThat(minimum, equalTo(0));
	}

	@Test(expected = IllegalStateException.class)
	@Schedule()
	public void GivenDefaultSchedule_WhenGetMinimumValueWithoutParse_ThenException() {
		// given

		// when
		parser.getMinimumValue();

		// then
	}

	@Test
	@Schedule(second = "*")
	public void GivenWidlcardValue_WhenGetNextValue_ThenSameSecondWithoutOverflow() {
		// given
		when(schedule.second()).thenReturn("*");
		parser.parse(schedule, now);

		// when
		NextValue next = parser.getNextValue(10);

		// then
		assertThat(next, notNullValue());
		assertThat(next.getValue(), equalTo(10));
		assertThat(next.isOverflow(), is(false));
	}

	@Test(expected = IllegalStateException.class)
	@Schedule()
	public void GivenDefaultSchedule_WhenGetNextValueWithoutParse_ThenException() {
		// given

		// when
		parser.getNextValue(10);

		// then
	}

	@Test
	@Schedule(second = "10")
	public void GivenSingleValue_WhenGetNextValue_ThenSameSecondWithoutOverflow() {
		// given
		when(schedule.second()).thenReturn("10");
		parser.parse(schedule, now);

		// when
		NextValue next = parser.getNextValue(10);

		// then
		assertThat(next, notNullValue());
		assertThat(next.getValue(), equalTo(10));
		assertThat(next.isOverflow(), is(false));
	}

	@Test
	@Schedule(second = "10")
	public void GivenSingleValue_WhenGetNextValueAfter_ThenOverflow() {
		// given
		when(schedule.second()).thenReturn("10");
		parser.parse(schedule, now);

		// when
		NextValue next = parser.getNextValue(11);

		// then
		assertThat(next, notNullValue());
		assertThat(next.getValue(), equalTo(10));
		assertThat(next.isOverflow(), is(true));
	}

	@Test
	@Schedule(second = "30")
	public void GivenSingleValue_WhenParse_ThenSetIt() {
		// given
		when(schedule.second()).thenReturn("30");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(30));
	}

	@Test
	@Schedule(second = "30-32")
	public void GivenRangeValues_WhenParse_ThenIncludeEnds() {
		// given
		when(schedule.second()).thenReturn("30-32");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(30, 31, 32));
	}

	@Test
	@Schedule(second = "58-02")
	public void GivenRangeValuesOverflow_WhenParse_ThenBothSubRanges() {
		// given
		when(schedule.second()).thenReturn("58-02");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(5));
		assertThat(values, hasItems(0, 1, 2, 58, 59));
	}

	@Test
	@Schedule(second = "30-30")
	public void GivenIdenticalRangeValues_WhenParse_ThenSetSingle() {
		// given
		when(schedule.second()).thenReturn("30-30");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(1));
		assertThat(values, hasItems(30));
	}

	@Test
	@Schedule(second = "10/20")
	public void GivenIncrementValues_WhenParse_ThenAllIncrements() {
		// given
		when(schedule.second()).thenReturn("10/20");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(10, 30, 50));
	}

	@Test
	@Schedule(second = "*/20")
	public void GivenWildcardIncrementValues_WhenParse_ThenStartWithZero() {
		// given
		when(schedule.second()).thenReturn("*/20");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(0, 20, 40));
	}

	@Test
	@Schedule(second = "10, 25, 40")
	public void GivenListValues_WhenParse_ThenAllItems() {
		// given
		when(schedule.second()).thenReturn("10, 25, 40");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(3));
		assertThat(values, hasItems(10, 25, 40));
	}

	@Test
	@Schedule(second = "10-15, 25, 40")
	public void GivenListWithRange_WhenParse_ThenIncludeRange() {
		// given
		when(schedule.second()).thenReturn("10-15, 25, 40");

		// when
		SortedSet<Integer> values = parser.parse(schedule, now);

		// then
		assertThat(values, hasSize(8));
		assertThat(values, hasItems(10, 11, 12, 13, 14, 15, 25, 40));
	}

	@Test(expected = IllegalArgumentException.class)
	@Schedule(second = "60")
	public void GivenTooLargeSingleValue_WhenParse_ThenException() {
		// given
		when(schedule.second()).thenReturn("60");

		// when
		parser.parse(schedule, now);

		// then
	}
}
