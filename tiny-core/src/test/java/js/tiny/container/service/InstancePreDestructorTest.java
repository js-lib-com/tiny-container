package js.tiny.container.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;

import javax.annotation.PreDestroy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IInstancePreDestroyProcessor.Priority;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class InstancePreDestructorTest {
	@Mock
	private IManagedClass<Service> managedClass;
	
	private Service instance;

	private InstancePreDestructor processor;

	@Before
	public void beforeTest() {
		instance = new Service();
		doReturn(Service.class).when(managedClass).getImplementationClass();

		processor = new InstancePreDestructor();
		processor.bind(managedClass);
	}

	@Test
	public void GivenProcessor_WhenInstancePreDestroy_Then() {
		// given

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test
	public void GivenProcessor_WhenGetPriority_ThenDESTRUCTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.DESTRUCTOR));
	}

	private static class Service {
		@PreDestroy
		private void postConstruct() {
		}
	}
}
