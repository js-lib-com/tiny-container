package js.tiny.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.injector.ProvisionException;
import js.lang.Config;

@RunWith(MockitoJUnitRunner.class)
public class CDIConformityTest {
	@Mock
	private Config config;

	private CDI cdi;

	@Before
	public void beforeTest() {
		cdi = CDI.create();
	}

	@Test
	public void GivenTaskService_WhenGetInstance_ThenTaskInjected() {
		// given
		cdi.bind(new ContainerBindingParameters<>(Task.class));
		cdi.bind(new ContainerBindingParameters<>(TaskService.class));
		cdi.configure(config);

		// when
		TaskService service = cdi.getInstance(TaskService.class);

		// then
		assertThat(service.task, notNullValue());
	}

	@Test
	public void GivenTwoTasksService_WhenGetInstance_ThenBothTasksInjected() {
		// given
		cdi.bind(new ContainerBindingParameters<>(Task.class));
		cdi.bind(new ContainerBindingParameters<>(TwoTasksService.class));
		cdi.configure(config);

		// when
		TwoTasksService service = cdi.getInstance(TwoTasksService.class);

		// then
		assertThat(service.task1, notNullValue());
		assertThat(service.task2, notNullValue());
	}

	@Test(expected = ProvisionException.class)
	public void GivenMissingBinding_WhenGetInstance_ThenException() {
		// given
		cdi.configure(config);

		// when
		cdi.getInstance(Object.class);

		// then
	}

	@Test(expected = ProvisionException.class)
	public void GivenMissingService_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new ContainerBindingParameters<>(IService.class).setService(true));
		cdi.configure(config);

		// when
		cdi.getInstance(IService.class);

		// then
	}

	@Test(expected = ProvisionException.class)
	public void GivenConstructorFail_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new ContainerBindingParameters<>(ExceptionalTask.class));
		cdi.configure(config);

		// when
		cdi.getInstance(ExceptionalTask.class);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class TaskService {
		@Inject
		private Task task;
	}

	private static class TwoTasksService {
		@Inject
		private Task task1;

		@Inject
		private Task task2;
	}

	private static class Task {

	}

	private static class ExceptionalTask {
		@SuppressWarnings("unused")
		public ExceptionalTask() {
			throw new RuntimeException();
		}
	}

	private interface IService {
	}
}