package com.jslib.container.timer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimerTaskTest {
	@Mock
	private CalendarTimerService service;
	@Mock
	private Object instance;

	private Method method;
	private TimerTask task;

	@Before
	public void beforeTest() throws NoSuchMethodException, SecurityException {
		method = TimerTaskTest.class.getDeclaredMethod("timer");
		method.setAccessible(true);
		exception = false;
		task = new TimerTask(service, instance, method);
	}

	@Test
	public void GivenPositiveDelay_WhenTaskRun_ThenInvokeServiceSchedule() {
		// given
		when(service.computeDelay(any())).thenReturn(100L);

		// when
		task.run();

		// then
		verify(service, times(1)).schedule(task, 100L);
	}

	@Test
	public void GivenZeroDelay_WhenTaskRun_ThenDoNotInvokeServiceSchedule() {
		// given
		when(service.computeDelay(any())).thenReturn(0L);

		// when
		task.run();

		// then
		verify(service, times(0)).schedule(any(), anyLong());
	}

	@Test
	public void GivenMethodInvokationFail_WhenTaskRun_ThenNoException() throws Exception {
		// given
		exception = true;

		// when
		task.run();

		// then
	}

	private static boolean exception;

	@SuppressWarnings("unused")
	private static void timer() {
		if (exception) {
			throw new RuntimeException();
		}
	}
}
