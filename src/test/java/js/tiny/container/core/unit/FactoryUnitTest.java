package js.tiny.container.core.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;

import js.lang.BugError;
import js.tiny.container.core.AppFactory;
import js.tiny.container.core.Factory;
import js.util.Classes;

import org.junit.Test;

public class FactoryUnitTest {
	@Test
	public void privateConstructor() {
		Classes.newInstance("js.tiny.container.core.Factory");
	}

	/** Assert application factories storage is inheritable so that child threads to have access to parent application factory. */
	@Test
	public void storageType() {
		ThreadLocal<AppFactory> tls = Classes.getFieldValue(Factory.class, "tls");
		assertNotNull(tls);
		assertTrue(tls instanceof InheritableThreadLocal);
	}

	/** Application factory bound on main thread should be accessible on child thread. */
	public void storageInheritance() throws InterruptedException {
		final MockAppFactory appFactory = new MockAppFactory();
		Factory.bind(appFactory);

		final Object lock = new Object();
		final AtomicBoolean probe = new AtomicBoolean();
		probe.set(false);

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					probe.set(appFactory == Factory.getAppFactory());
				} catch (Throwable t) {
				}
				synchronized (lock) {
					lock.notify();
				}
			}
		});
		thread.start();

		synchronized (lock) {
			lock.wait();
		}
		assertTrue("Application factory is not inherited into thread.", probe.get());
	}

	/** Missing application factory should throw exception. */
	@Test(expected = BugError.class)
	public void getAppFactoryException() {
		Factory.bind(null);
		Factory.getAppFactory();
	}

	@Test
	public void isValid() {
		Factory.bind(null);
		assertFalse(Factory.isValid());
		Factory.bind(new MockAppFactory());
		assertTrue(Factory.isValid());
	}

	@Test
	public void getInstance() {
		MockAppFactory appFactory = new MockAppFactory();
		Factory.bind(appFactory);

		assertEquals(1964, Factory.getInstance(File.class, "/var/www/vhost", 1964));

		assertEquals(2, appFactory.args.length);
		assertEquals(File.class, appFactory.args[0]);
		assertEquals(2, ((Object[]) appFactory.args[1]).length);
		assertEquals("/var/www/vhost", ((Object[]) appFactory.args[1])[0]);
		assertEquals(1964, ((Object[]) appFactory.args[1])[1]);
	}

	@Test
	public void getNamedInstance() {
		MockAppFactory appFactory = new MockAppFactory();
		Factory.bind(appFactory);

		assertEquals(1965, Factory.getInstance("file-manager", File.class, "/var/www/vhost", 1964));

		assertEquals(3, appFactory.args.length);
		assertEquals("file-manager", appFactory.args[0]);
		assertEquals(File.class, appFactory.args[1]);
		assertEquals(2, ((Object[]) appFactory.args[2]).length);
		assertEquals("/var/www/vhost", ((Object[]) appFactory.args[2])[0]);
		assertEquals(1964, ((Object[]) appFactory.args[2])[1]);
	}

	@Test
	public void getOptionalInstance() {
		MockAppFactory appFactory = new MockAppFactory();
		Factory.bind(appFactory);

		assertEquals(1966, Factory.getOptionalInstance(File.class, "/var/www/vhost", 1964));

		assertEquals(2, appFactory.args.length);
		assertEquals(File.class, appFactory.args[0]);
		assertEquals(2, ((Object[]) appFactory.args[1]).length);
		assertEquals("/var/www/vhost", ((Object[]) appFactory.args[1])[0]);
		assertEquals(1964, ((Object[]) appFactory.args[1])[1]);
	}

	@Test
	public void getRemoteInstance() throws MalformedURLException {
		MockAppFactory appFactory = new MockAppFactory();
		Factory.bind(appFactory);

		assertEquals(1967, Factory.getRemoteInstance("http://server.com/app", File.class));

		assertEquals(2, appFactory.args.length);
		assertEquals("http://server.com/app", appFactory.args[0]);
		assertEquals(File.class, appFactory.args[1]);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@SuppressWarnings("unchecked")
	private static class MockAppFactory implements AppFactory {
		private Object[] args;

		@Override
		public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
			this.args = new Object[] { interfaceClass, args };
			return (T) new Integer(1964);
		}

		@Override
		public <T> T getInstance(String instanceName, Class<? super T> interfaceClass, Object... args) {
			this.args = new Object[] { instanceName, interfaceClass, args };
			return (T) new Integer(1965);
		}

		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
			this.args = new Object[] { interfaceClass, args };
			return (T) new Integer(1966);
		}

		@Override
		public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
			this.args = new Object[] { implementationURL, interfaceClass };
			return (T) new Integer(1967);
		}
	}
}
