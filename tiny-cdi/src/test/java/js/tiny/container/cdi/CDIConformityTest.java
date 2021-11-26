package js.tiny.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
		cdi.bind(new ContainerBinding<>(Task.class));
		cdi.bind(new ContainerBinding<>(TaskService.class));
		cdi.configure(config);

		// when
		TaskService service = cdi.getInstance(TaskService.class);

		// then
		assertThat(service.task, notNullValue());
	}

	@Test
	public void GivenTwoTasksService_WhenGetInstance_ThenBothTasksInjected() {
		// given
		cdi.bind(new ContainerBinding<>(Task.class));
		cdi.bind(new ContainerBinding<>(TwoTasksService.class));
		cdi.configure(config);

		// when
		TwoTasksService service = cdi.getInstance(TwoTasksService.class);

		// then
		assertThat(service.task1, notNullValue());
		assertThat(service.task2, notNullValue());
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

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
}