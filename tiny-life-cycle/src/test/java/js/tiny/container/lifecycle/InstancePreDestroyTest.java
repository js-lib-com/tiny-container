package js.tiny.container.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import javax.annotation.PreDestroy;

import org.junit.Before;
import org.junit.Test;

import js.tiny.container.spi.IInstancePreDestroyProcessor.Priority;

public class InstancePreDestroyTest {
	private InstancePreDestroyProcessor processor;

	@Before
	public void beforeTest() {
		processor = new InstancePreDestroyProcessor();
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
		Service instance = new Service();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
		assertThat(instance.invocationProbe, equalTo(1));
	}

	@Test
	public void GivenMethodFail_WhenOnInstancePreDestroy_ThenLogDump() {
		// given
		Object instance = new ServiceExcecutionException();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenStaticMethod_WhenOnInstancePreDestroy_ThenException() {
		// given
		Object instance = new ServiceStatic();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMethodWithParameter_WhenOnInstancePreDestroy_ThenException() {
		// given
		Object instance = new ServiceParameter();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivennonVoidMethod_WhenOnInstancePreDestroy_ThenException() {
		// given
		Object instance = new ServiceNotVoid();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMethodWithException_WhenOnInstancePreDestroy_ThenException() {
		// given
		Object instance = new ServiceException();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenDuplicatedMethod_WhenOnInstancePreDestroy_ThenException() {
		// given
		Object instance = new ServiceDuplicated();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test
	public void GivenMissingManagedMethod_WhenOnInstancePreDestroy_ThenNothing() {
		// given
		Object instance = new Object();

		// when
		processor.onInstancePreDestroy(null, instance);

		// then
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenNullInstance_WhenOnInstancePreDestroy_ThenException() {
		// given

		// when
		processor.onInstancePreDestroy(null, null);

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
