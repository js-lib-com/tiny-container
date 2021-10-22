package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.tiny.container.spi.IFactory;

@RunWith(MockitoJUnitRunner.class)
public class FactoryTest {
	@Mock
	private IFactory factory;
	@Mock
	private File instance;
	
	/**
	 * Assert factory implementations storage is inheritable so that child threads to have access to parent factory
	 * implementation.
	 */
	@Test
	public void GivenFactoriesStorage_WhenGet_ThenInheritable() {
		// given

		// when
		ThreadLocal<IFactory> tls = Factory.tls();

		// then
		assertThat(tls, notNullValue());
		assertThat(tls, instanceOf(InheritableThreadLocal.class));
	}

	/** Factory implementation bound on main thread should be accessible on child thread. */
	public void GivenParentFactory_WhenGetFromChild_ThenTheSame() throws InterruptedException {
		class ChildFactory {
			IFactory factory;
		}

		// given
		Factory.bind(factory);

		// when
		final Object lock = new Object();
		final ChildFactory child = new ChildFactory();

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				child.factory = Factory.get();
				synchronized (lock) {
					lock.notify();
				}
			}
		});
		thread.start();

		synchronized (lock) {
			lock.wait();
		}

		// then
		assertThat(child.factory, equalTo(factory));
	}

	/** Missing factory implementation should throw exception. */
	@Test(expected = BugError.class)
	public void GivenFactoryNotBound_WhenGet_ThenException() {
		// given
		Factory.bind(null);

		// when
		Factory.get();

		// then
	}

	@Test
	public void GivenFactoryBound_WhenIsValid_ThenTrue() {
		// given
		Factory.bind(factory);
		
		// when
		boolean valid = Factory.isValid();
		
		// then
		assertThat(valid, is(true));
	}

	@Test
	public void GivenFactoryNotBound_WhenIsValid_ThenFalse() {
		// given
		Factory.bind(null);
		
		// when
		boolean valid = Factory.isValid();
		
		// then
		assertThat(valid, is(false));
	}

	@Test
	public void GivenFactoryBound_WhenGetInstance_ThenDelegateImplementation() {
		// given
		Factory.bind(factory);
		when(factory.getInstance(File.class)).thenReturn(instance);

		// when
		File file = Factory.getInstance(File.class);

		// then
		assertThat(file, equalTo(instance));
		verify(factory, times(1)).getInstance(File.class);
	}

	@Test
	public void GivenFactoryBound_WhenGetNamedInstance_ThenDelegateImplementation() {
		// given
		Factory.bind(factory);
		when(factory.getInstance(File.class, "file-manager")).thenReturn(instance);

		// when
		File file = Factory.getInstance(File.class, "file-manager");

		// then
		assertThat(file, equalTo(instance));
		verify(factory, times(1)).getInstance(File.class, "file-manager");
	}

	@Test
	public void GivenFactoryBound_WhenGetOptionalInstance_ThenDelegateImplementation() {
		// given
		Factory.bind(factory);
		when(factory.getOptionalInstance(File.class)).thenReturn(instance);

		// when
		File file = Factory.getOptionalInstance(File.class);

		// then
		assertThat(file, equalTo(instance));
		verify(factory, times(1)).getOptionalInstance(File.class);
	}
}
