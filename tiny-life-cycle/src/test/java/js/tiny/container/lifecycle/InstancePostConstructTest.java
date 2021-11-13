package js.tiny.container.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import javax.annotation.PostConstruct;

import org.junit.Before;
import org.junit.Test;

import js.tiny.container.spi.IInstancePostConstructProcessor.Priority;

public class InstancePostConstructTest {
	private InstancePostConstructProcessor processor;

	@Before
	public void beforeTest() {
		processor = new InstancePostConstructProcessor();
		processor.resetCache();
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenCONSTRCUTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.CONSTRUCTOR));
	}

	@Test
	public void GivenPostConstructMethod_WhenOnInstancePostConstruct_ThenInvoke() {
		// given
		Service instance = new Service();

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.invocationProbe, equalTo(1));
	}

	@Test(expected = RuntimeException.class)
	public void GivenMethodFail_WhenOnInstancePostConstruct_ThenException() {
		// given
		Object instance = new ServiceExcecutionException();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenStaticMethod_WhenOnInstancePostConstruct_ThenException() {
		// given
		Object instance = new ServiceStatic();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMethodWithParameter_WhenOnInstancePostConstruct_ThenException() {
		// given
		Object instance = new ServiceParameter();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivennonVoidMethod_WhenOnInstancePostConstruct_ThenException() {
		// given
		Object instance = new ServiceNotVoid();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMethodWithException_WhenOnInstancePostConstruct_ThenException() {
		// given
		Object instance = new ServiceException();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenDuplicatedMethod_WhenOnInstancePostConstruct_ThenException() {
		// given
		Object instance = new ServiceDuplicated();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test
	public void GivenMissingManagedMethod_WhenOnInstancePostConstruct_ThenNothing() {
		// given
		Object instance = new Object();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenNullInstance_WhenOnInstancePostConstruct_ThenException() {
		// given

		// when
		processor.onInstancePostConstruct(null);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class Service {
		int invocationProbe;

		@PostConstruct
		public void postConstruct() {
			++invocationProbe;
		}
	}

	private static class ServiceStatic {
		@PostConstruct
		public static void postConstruct() {
		}
	}

	private static class ServiceParameter {
		@PostConstruct
		public void postConstruct(String parameter) {
		}
	}

	private static class ServiceNotVoid {
		@PostConstruct
		public String postConstruct() {
			return null;
		}
	}

	private static class ServiceException {
		@PostConstruct
		public void postConstruct() throws Exception {
		}
	}

	private static class ServiceDuplicated {
		@PostConstruct
		public void postConstruct1() {
		}

		@PostConstruct
		public void postConstruct2() {
		}
	}

	private static class ServiceExcecutionException {
		@PostConstruct
		public void postConstruct() {
			throw new IllegalStateException();
		}
	}
}
