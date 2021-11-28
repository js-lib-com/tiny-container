package js.tiny.container.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;

import javax.annotation.PostConstruct;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IInstancePostConstructProcessor.Priority;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class InstancePostConstructorTest {
	@Mock
	private IManagedClass<Service> managedClass;

	private Service instance;

	private InstancePostConstructor processor;

	@Before
	public void beforeTest() {
		instance = new Service();
		doReturn(Service.class).when(managedClass).getImplementationClass();

		processor = new InstancePostConstructor();
		processor.bind(managedClass);
	}

	@Test
	public void GivenProcessor_WhenInstancePostConstruct_Then() {
		// given

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test
	public void GivenProcessor_WhenGetPriority_ThenCONSTRUCTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.CONSTRUCTOR));
	}

	private static class Service {
		@PostConstruct
		private void postConstruct() {
		}
	}
}
