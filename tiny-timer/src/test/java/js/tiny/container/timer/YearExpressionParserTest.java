package js.tiny.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YearExpressionParserTest {
	@Mock
	private ScheduleMeta schedule;

	private CalendarEx now;

	private YearExpressionParser parser;

	@Before
	public void beforeTest() {
		when(schedule.year()).thenReturn("2020");

		now = new CalendarEx(2020, 1, 1, 0, 0, 0);

		parser = new YearExpressionParser();
	}

	@Test
	public void GivenYear2020_WhenNextValue2021_ThenNull() {
		// given
		parser.parse(schedule, now);

		// when
		NextValue value = parser.getNextValue(2021);

		// then
		assertThat(value, nullValue());
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNoParse_WhenNextValue_ThenException() {
		// given

		// when
		parser.getNextValue(2021);

		// then
	}
}
