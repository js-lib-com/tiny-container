package com.jslib.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ejb.Schedule;

@RunWith(MockitoJUnitRunner.class)
public class CalendarTimerServiceNextTimeoutTest {
	@Mock
	private Schedule schedule;

	private CalendarEx now;
	private CalendarTimerService service;

	@Before
	public void beforeTest() {
		when(schedule.second()).thenReturn("0");
		when(schedule.minute()).thenReturn("0");
		when(schedule.hour()).thenReturn("0");
		when(schedule.dayOfMonth()).thenReturn("*");
		when(schedule.dayOfWeek()).thenReturn("*");
		when(schedule.month()).thenReturn("*");
		when(schedule.year()).thenReturn("*");

		now = new CalendarEx(2020, 1, 1, 0, 0, 0);
		service = new CalendarTimerService();
	}

	/**
	 * Schedule is set to 00:00:00 on every day. Next timeout evaluation is performed on 00:00:30. Since the only acceptable
	 * value for seconds is zero it will overflow, triggering overflowing to minute and hour. As a result day will be moved to
	 * the next one.
	 */
	@Test
	@Schedule()
	public void GivenDefaultSchedule_WhenSecond30_ThenNextDay() {
		// given
		now.set(CalendarUnit.SECOND, 30);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 2, 0, 0, 0));
	}

	/**
	 * Schedule is set to 00:00:00 on every day from 2021. Next timeout evaluation is performed on first second from 2020. On
	 * returned timeout only year should be updated to 2021.
	 */
	@Test
	@Schedule(year = "2021")
	public void GivenYear2021_WhenYear2020_ThenSetYear() {
		// given
		when(schedule.year()).thenReturn("2021");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2021, 1, 1, 0, 0, 0));
	}

	/**
	 * Schedule is set to 2020-01-01 00:00:00. Next timeout evaluation is performed on 2020-01-01 00:00:30. Since all
	 * expressions are fixed and scheduler is passed at the evaluation moment, next timeout value is null.
	 */
	@Test
	@Schedule(dayOfMonth = "1", month = "Jan", year = "2020")
	public void GivenFirstSecondFrom2020_WhenSecond30Year2020_ThenNull() {
		// given
		when(schedule.dayOfMonth()).thenReturn("1");
		when(schedule.month()).thenReturn("Jan");
		when(schedule.year()).thenReturn("2020");

		now.set(CalendarUnit.SECOND, 30);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, nullValue());
	}

	@Test
	@Schedule(second = "20-40")
	public void GivenSecondRange_WhenBeforeRangeStart_ThenNextSecond() {
		// given
		when(schedule.second()).thenReturn("20-40");
		now.set(CalendarUnit.SECOND, 10);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 1, 0, 0, 20));
	}

	@Test
	@Schedule(second = "20-40")
	public void GivenSecondRange_WhenOnRangeStart_ThenNextSecond() {
		// given
		when(schedule.second()).thenReturn("20-40");
		now.set(CalendarUnit.SECOND, 20);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 1, 0, 0, 21));
	}

	@Test
	@Schedule(second = "20-40")
	public void GivenSecondRange_WhenInsideRangeStart_ThenKeepIt() {
		// given
		now.set(CalendarUnit.SECOND, 30);
		when(schedule.second()).thenReturn("20-40");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 1, 0, 0, 31));
	}

	/**
	 * Schedule is set to 00:00:[50-10]. Delay evaluation is performed on 00:00:59. Returned event calendar has seconds set to 0
	 * with overflow, minutes is increment because of seconds overflow. Since allowed minutes is only 0 it is reset with
	 * overflow to hours; the same for hours with overflow to days so that timeout event should be on timeout day.
	 */
	@Test
	@Schedule(second = "50-10")
	public void GivenSecondRangeOverflow_WhenDelay_ThenMinimum() {
		// given
		now.set(CalendarUnit.SECOND, 59);
		when(schedule.second()).thenReturn("50-10");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 2, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfWeek = "Fri")
	public void GivenDayOfWeek_WhenDelay_ThenSetDay() {
		// given
		when(schedule.dayOfWeek()).thenReturn("Fri");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 3, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "2nd Sun")
	public void GivenWeekDayOfMonth_WhenDelay_ThenSetDay() {
		// given
		when(schedule.dayOfWeek()).thenReturn("Fri");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 3, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfWeek = "Fri-Sun")
	public void GivenDayOfWeeksRange_WhenDelay_ThenSetFirstDay() {
		// given
		when(schedule.dayOfWeek()).thenReturn("Fri-Sun");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 3, 0, 0, 0));
	}

	@Test
	@Schedule(second = "0", minute = "*", hour = "10")
	public void GivenWildcardMinuteAndFixHour_WhenDelay_ThenMinuteZero() {
		// given
		when(schedule.second()).thenReturn("0");
		when(schedule.minute()).thenReturn("*");
		when(schedule.hour()).thenReturn("10");
		now.set(CalendarUnit.MINUTE, 30);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 1, 10, 0, 0));
	}

	@Test
	@Schedule(minute = "30", hour = "4")
	public void GivenHourMinute_WhenDelay_ThenSet() {
		// given
		when(schedule.minute()).thenReturn("30");
		when(schedule.hour()).thenReturn("4");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 1, 4, 30, 0));
	}

	@Test
	@Schedule(minute = "30", hour = "4", dayOfMonth = "10")
	public void GivenDayHourMinute_WhenDelay_ThenSet() {
		// given
		when(schedule.minute()).thenReturn("30");
		when(schedule.hour()).thenReturn("4");
		when(schedule.dayOfMonth()).thenReturn("10");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 10, 4, 30, 0));
	}

	@Test
	@Schedule(minute = "30", hour = "4", dayOfMonth = "10", month = "2")
	public void GivenMonthDayHourMinute_WhenDelay_ThenSet() {
		// given
		when(schedule.minute()).thenReturn("30");
		when(schedule.hour()).thenReturn("4");
		when(schedule.dayOfMonth()).thenReturn("10");
		when(schedule.month()).thenReturn("2");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 2, 10, 4, 30, 0));
	}

	@Test
	@Schedule(minute = "30", hour = "4", dayOfMonth = "10", month = "2", year = "2021")
	public void GivenYearMonthDayHourMinute_WhenDelay_ThenSet() {
		// given
		when(schedule.minute()).thenReturn("30");
		when(schedule.hour()).thenReturn("4");
		when(schedule.dayOfMonth()).thenReturn("10");
		when(schedule.month()).thenReturn("2");
		when(schedule.year()).thenReturn("2021");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2021, 2, 10, 4, 30, 0));
	}

	@Test
	@Schedule()
	public void GivenDefault_WhenLast30SecondsFromYear_ThenNextYear() {
		// given
		now.set(CalendarUnit.YEAR, 2021);
		now.set(CalendarUnit.MONTH, 12);
		now.set(CalendarUnit.DAY, 31);
		// TODO: HACK: 21 EET = 23 UTC; fix CalendarEx time zone handling
		now.set(CalendarUnit.HOUR, 21);
		now.set(CalendarUnit.MINUTE, 59);
		now.set(CalendarUnit.SECOND, 30);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2022, 1, 1, 0, 0, 0));
	}

	@Test
	@Schedule()
	public void GivenDefault_WhenLastSecondFromYear_ThenNextYear() {
		// given
		now.set(CalendarUnit.YEAR, 2021);
		now.set(CalendarUnit.MONTH, 12);
		now.set(CalendarUnit.DAY, 31);
		// TODO: HACK: 21 EET = 23 UTC; fix CalendarEx time zone handling
		now.set(CalendarUnit.HOUR, 21);
		now.set(CalendarUnit.MINUTE, 59);
		now.set(CalendarUnit.SECOND, 59);

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2022, 1, 1, 0, 0, 0));
	}

	@Test
	@Schedule(month = "Mar")
	public void GivenMonthName_WhenDayInsideMonth_ThenSetMonthAndResetDay() {
		// given
		now.set(CalendarUnit.DAY, 10);
		when(schedule.month()).thenReturn("Mar");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 3, 1, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "Last")
	public void GivenLastDayOfJanuary_WhenDelay_ThenSet31() {
		// given
		when(schedule.dayOfMonth()).thenReturn("Last");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 31, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "-1")
	public void GivenBeforeLastDayOfJanuary_WhenDelay_ThenSet30() {
		// given
		when(schedule.dayOfMonth()).thenReturn("-1");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 1, 30, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "Last", month = "Feb", year = "2021")
	public void GivenLastDayOfFebruary_WhenDelay_ThenSet28() {
		// given
		when(schedule.dayOfMonth()).thenReturn("Last");
		when(schedule.month()).thenReturn("Feb");
		when(schedule.year()).thenReturn("2021");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2021, 2, 28, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "-1", month = "Feb", year = "2021")
	public void GivenBeforeLastDayOfFebruary_WhenDelay_ThenSet27() {
		// given
		when(schedule.dayOfMonth()).thenReturn("-1");
		when(schedule.month()).thenReturn("Feb");
		when(schedule.year()).thenReturn("2021");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2021, 2, 27, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "Last", month = "Feb", year = "2020")
	public void GivenLastDayOfLeapFebruary_WhenDelay_ThenSet29() {
		// given
		now.set(CalendarUnit.YEAR, 2020);
		now.set(CalendarUnit.MONTH, 1);

		when(schedule.dayOfMonth()).thenReturn("Last");
		when(schedule.month()).thenReturn("Feb");
		when(schedule.year()).thenReturn("2020");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 2, 29, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "last", month = "beb", year = "2020")
	public void GivenLastDayOfLeapFebruaryLowerCase_WhenDelay_ThenSet29() {
		// given
		now.set(CalendarUnit.YEAR, 2020);
		now.set(CalendarUnit.MONTH, 1);

		when(schedule.dayOfMonth()).thenReturn("last");
		when(schedule.month()).thenReturn("feb");
		when(schedule.year()).thenReturn("2020");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 2, 29, 0, 0, 0));
	}

	@Test
	@Schedule(dayOfMonth = "-1", month = "Feb", year = "2020")
	public void GivenBeforeLastDayOfLeapFebruary_WhenDelay_ThenSet28() {
		// given
		now.set(CalendarUnit.YEAR, 2020);
		now.set(CalendarUnit.MONTH, 1);

		when(schedule.dayOfMonth()).thenReturn("-1");
		when(schedule.month()).thenReturn("Feb");
		when(schedule.year()).thenReturn("2020");

		// when
		Date timeout = service.getNextTimeout(now.getTime(), schedule);

		// then
		assertThat(timeout, equals(2020, 2, 28, 0, 0, 0));
	}

	private Matcher<Date> equals(int year, int month, int day, int hour, int minute, int second) {
		class CalendarExMatcher extends BaseMatcher<Date> {
			@Override
			public boolean matches(Object item) {
				CalendarEx calendar = new CalendarEx((Date) item);
				return calendar.equals(year, month, day, hour, minute, second);
			}

			@Override
			public void describeTo(Description description) {
				CalendarEx calendar = new CalendarEx(year, month, day, hour, minute, second);
				description.appendText(calendar.getTime().toString());
			}
		}
		return new CalendarExMatcher();
	}
}
