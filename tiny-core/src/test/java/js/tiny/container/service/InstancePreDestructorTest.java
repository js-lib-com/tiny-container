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

	private Object instance;

	private InstancePreDestructor processor;

	@Before
	public void beforeTest() {
		instance = new Service();
		doReturn(Service.class).when(managedClass).getImplementationClass();

		processor = new InstancePreDestructor();
		processor.resetCache();
	}

	@Test
	public void GivenSuccessfulBind_WhenInstancePreDestroy_Then() {
		// given
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNoBind_WhenInstancePreDestroy_ThenException() {
		// given

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test
	public void GivenExceptionalService_WhenInstancePreDestroy_Then() {
		// given
		instance = new ExceptionalService();
		doReturn(ExceptionalService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenStaticMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new StaticService();
		doReturn(StaticService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenParameterMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new ParameterService();
		doReturn(ParameterService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNonVoidMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new NonVoidService();
		doReturn(NonVoidService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenThrowsMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new ThrowsService();
		doReturn(ThrowsService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

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

	// --------------------------------------------------------------------------------------------

	private static class Service {
		@PreDestroy
		private void postConstruct() {
		}
	}

	private static class ExceptionalService {
		@PreDestroy
		private void postConstruct() {
			throw new RuntimeException();
		}
	}

	private static class StaticService {
		@PreDestroy
		private static void postConstruct() {
		}
	}

	private static class ParameterService {
		@PreDestroy
		private void postConstruct(String name) {
		}
	}

	private static class NonVoidService {
		@PreDestroy
		private String postConstruct() {
			return "name";
		}
	}

	private static class ThrowsService {
		@PreDestroy
		private void postConstruct() throws Exception {
		}
	}
}
