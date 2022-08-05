package com.jslib.container.timer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.IInstancePostConstructProcessor.Priority;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;

import jakarta.ejb.Schedule;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CalendarTimerServiceTest {
	@Mock
	private ScheduledExecutorService scheduler;
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private Schedule schedule;

	private Object instance;
	private CalendarTimerService service;

	@Before
	public void beforeTest() {
		instance = new Object();

		doReturn(instance.getClass()).when(managedClass).getImplementationClass();
		doReturn(managedClass).when(managedMethod).getDeclaringClass();
		when(managedMethod.scanAnnotation(Schedule.class)).thenReturn(schedule);

		when(schedule.second()).thenReturn("0");
		when(schedule.minute()).thenReturn("0");
		when(schedule.hour()).thenReturn("0");
		when(schedule.dayOfMonth()).thenReturn("*");
		when(schedule.dayOfWeek()).thenReturn("*");
		when(schedule.month()).thenReturn("*");
		when(schedule.year()).thenReturn("*");

		service = new CalendarTimerService(scheduler);
	}

	@Ignore
	@Test
	public void GivenTimerMethod_WhenPostConstructInstance_ThenInvokeScheduler() {
		// given
		//service.scanMethodAnnotations(managedMethod);

		// when
		service.onInstancePostConstruct(instance);

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
