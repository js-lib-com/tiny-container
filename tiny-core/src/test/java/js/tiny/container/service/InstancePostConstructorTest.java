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

	private Object instance;

	private InstancePostConstructor processor;

	@Before
	public void beforeTest() {
		instance = new Service();
		doReturn(Service.class).when(managedClass).getImplementationClass();

		processor = new InstancePostConstructor();
		processor.resetCache();
	}

	@Test
	public void GivenSuccessfulBind_WhenInstancePostConstruct_Then() {
		// given
		processor.bind(managedClass);

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNoBind_WhenInstancePostConstruct_TheException() {
		// given

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenMethodException_WhenInstancePostConstruct_Then() {
		// given
		instance = new ExceptionalService();
		doReturn(ExceptionalService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenStaticMethod_WhenInstancePostConstruct_Then() {
		// given
		instance = new StaticService();
		doReturn(StaticService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenParameterMethod_WhenInstancePostConstruct_Then() {
		// given
		instance = new ParameterService();
		doReturn(ParameterService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivennonVoidMethod_WhenInstancePostConstruct_Then() {
		// given
		instance = new NonVoidService();
		doReturn(NonVoidService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenThrowsMethod_WhenInstancePostConstruct_Then() {
		// given
		instance = new ThrowsService();
		doReturn(ThrowsService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

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

	// --------------------------------------------------------------------------------------------

	private static class Service {
		@PostConstruct
		private void postConstruct() {
		}
	}

	private static class ExceptionalService {
		@PostConstruct
		private void postConstruct() {
			throw new RuntimeException();
		}
	}

	private static class StaticService {
		@PostConstruct
		private static void postConstruct() {
		}
	}

	private static class ParameterService {
		@PostConstruct
		private void postConstruct(String name) {
		}
	}

	private static class NonVoidService {
		@PostConstruct
		private String postConstruct() {
			return "name";
		}
	}

	private static class ThrowsService {
		@PostConstruct
		private void postConstruct() throws Exception {
		}
	}
}
