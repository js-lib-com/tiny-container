package js.tiny.container.lifecycle;

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
public class InstancePreDestroyTest {
	@Mock
	private IManagedClass<?> managedClass;

	private InstancePreDestructor processor;

	@Before
	public void beforeTest() {
		processor = new InstancePreDestructor();
		processor.resetCache();
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenDESTRUCTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.DESTRUCTOR));
	}

	@Test
	public void GivenPostConstructMethod_WhenOnInstancePreDestroy_ThenInvoke() {
		// given
		doReturn(Service.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Service instance = new Service();

		// when
		processor.onInstancePreDestroy(instance);

		// then
		assertThat(instance.invocationProbe, equalTo(1));
	}

	@Test
	public void GivenMethodFail_WhenOnInstancePreDestroy_ThenLogDump() {
		// given
		doReturn(ServiceExcecutionException.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Object instance = new ServiceExcecutionException();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenStaticMethod_WhenOnInstancePreDestroy_ThenException() {
		// given
		doReturn(ServiceStatic.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Object instance = new ServiceStatic();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMethodWithParameter_WhenOnInstancePreDestroy_ThenException() {
		// given
		doReturn(ServiceParameter.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Object instance = new ServiceParameter();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivennonVoidMethod_WhenOnInstancePreDestroy_ThenException() {
		// given
		doReturn(ServiceNotVoid.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Object instance = new ServiceNotVoid();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMethodWithException_WhenOnInstancePreDestroy_ThenException() {
		// given
		doReturn(ServiceException.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Object instance = new ServiceException();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test
	public void GivenDuplicatedMethod_WhenOnInstancePreDestroy_ThenGetOne() {
		// given
		doReturn(ServiceDuplicated.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);
		Object instance = new ServiceDuplicated();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingManagedMethod_WhenOnInstancePreDestroy_ThenException() {
		// given
		Object instance = new Object();

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenNullInstance_WhenOnInstancePreDestroy_ThenException() {
		// given

		// when
		processor.onInstancePreDestroy(null);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class Service {
		int invocationProbe;

		@PreDestroy
		public void preDestroy() {
			++invocationProbe;
		}
	}

	private static class ServiceStatic {
		@PreDestroy
		public static void preDestroy() {
		}
	}

	private static class ServiceParameter {
		@PreDestroy
		public void preDestroy(String parameter) {
		}
	}

	private static class ServiceNotVoid {
		@PreDestroy
		public String preDestroy() {
			return null;
		}
	}

	private static class ServiceException {
		@PreDestroy
		public void preDestroy() throws Exception {
		}
	}

	private static class ServiceDuplicated {
		@PreDestroy
		public void preDestroy1() {
		}

		@PreDestroy
		public void preDestroy2() {
		}
	}

	private static class ServiceExcecutionException {
		@PreDestroy
		public void preDestroy() {
			throw new IllegalStateException();
		}
	}
}
