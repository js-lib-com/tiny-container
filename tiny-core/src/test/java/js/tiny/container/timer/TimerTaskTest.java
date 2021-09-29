package js.tiny.container.timer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.InvocationException;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class TimerTaskTest {
	@Mock
	private CalendarTimerService service;
	@Mock
	private Object instance;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private ScheduleMeta schedule;

	private TimerTask task;

	@Before
	public void beforeTest() {
		when(managedMethod.getServiceMeta(ScheduleMeta.class)).thenReturn(schedule);

		task = new TimerTask(service, instance, managedMethod);
	}

	@Test
	public void GivenPositiveDelay_WhenTaskRun_ThenInvokeServiceSchedule() {
		// given
		when(service.computeDelay(schedule)).thenReturn(100L);

		// when
		task.run();

		// then
		verify(service, times(1)).schedule(task, 100L);
	}

	@Test
	public void GivenZeroDelay_WhenTaskRun_ThenDoNotInvokeServiceSchedule() {
		// given
		when(service.computeDelay(schedule)).thenReturn(0L);

		// when
		task.run();

		// then
		verify(service, times(0)).schedule(any(), anyLong());
	}

	@Test
	public void GivenMethodInvokationFail_WhenTaskRun_ThenLog() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.invoke(instance)).thenThrow(InvocationException.class);

		// when
		task.run();

		// then
	}
}
