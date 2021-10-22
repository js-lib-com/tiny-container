package js.tiny.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Schedule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IInstancePostConstructionProcessor.Priority;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class CalendarTimerServiceTest {
	@Mock
	private ScheduledExecutorService scheduler;
	@Mock
	private Object instance;
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private Schedule schedule;

	private CalendarTimerService service;

	@Before
	public void beforeTest() {
		when(managedClass.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.getAnnotation(Schedule.class)).thenReturn(schedule);

		when(schedule.second()).thenReturn("0");
		when(schedule.minute()).thenReturn("0");
		when(schedule.hour()).thenReturn("0");
		when(schedule.dayOfMonth()).thenReturn("*");
		when(schedule.dayOfWeek()).thenReturn("*");
		when(schedule.month()).thenReturn("*");
		when(schedule.year()).thenReturn("*");

		service = new CalendarTimerService(scheduler);
	}

	@Test
	public void GivenDefaults_WhenPostConstructInstance_ThenInvokeScheduler() {
		// given

		// when
		service.onInstancePostConstruction(managedClass, instance);

		// then
		ArgumentCaptor<Long> delayArg = ArgumentCaptor.forClass(Long.class);
		verify(scheduler, times(1)).schedule(any(TimerTask.class), delayArg.capture(), eq(TimeUnit.MILLISECONDS));
		assertThat(delayArg.getValue(), greaterThan(0L));
	}

	@Test
	public void GivenDefaults_WhenDestroy_ThenShutdownScheduler() {
		// given

		// when
		service.destroy();

		// then
		verify(scheduler, times(1)).shutdownNow();
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenTIMER() {
		// given

		// when
		Priority priority = service.getPriority();

		// then
		assertThat(priority, equalTo(Priority.TIMER));

	}
}
