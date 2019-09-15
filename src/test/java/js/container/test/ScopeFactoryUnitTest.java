package js.container.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import js.container.Container;
import js.container.InstanceKey;
import js.container.InstanceScope;
import js.container.ManagedClassSPI;
import js.container.ScopeFactory;
import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.test.stub.ManagedClassSpiStub;
import js.unit.TestContext;
import js.util.Classes;

import org.junit.BeforeClass;
import org.junit.Test;

public class ScopeFactoryUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@Test
	public void containerRegistration() throws Exception {
		Object container = TestContext.start();
		Map<InstanceScope, ScopeFactory> scopeFactories = Classes.getFieldValue(container, Container.class, "scopeFactories");

		assertNotNull(scopeFactories);
		assertNotNull(scopeFactories.get(InstanceScope.APPLICATION));
		assertNotNull(scopeFactories.get(InstanceScope.THREAD));
		assertNull(scopeFactories.get(InstanceScope.LOCAL));

		assertTrue(scopeFactories.get(InstanceScope.APPLICATION) instanceof ScopeFactory);
		assertTrue(scopeFactories.get(InstanceScope.THREAD) instanceof ScopeFactory);
	}

	@Test
	public void instanceScopeValue() {
		assertEquals("APPLICATION", InstanceScope.APPLICATION.getValue());
		assertEquals("THREAD", InstanceScope.THREAD.getValue());
		assertEquals("SESSION", InstanceScope.SESSION.getValue());
		assertEquals("LOCAL", InstanceScope.LOCAL.getValue());
	}

	@Test
	public void instanceScopeHashCode() {
		assertEquals(31 + "SCOPE".hashCode(), new InstanceScope("SCOPE").hashCode());
		assertEquals(31, new InstanceScope().hashCode());
	}

	@Test
	public void instanceScopeEquality() {
		InstanceScope scope1 = new InstanceScope("SCOPE");
		assertTrue(scope1.equals(scope1));
		InstanceScope scope2 = new InstanceScope("SCOPE");
		assertTrue(scope1.equals(scope2));

		scope1 = new InstanceScope("SCOPE1");
		scope2 = new InstanceScope("SCOPE2");
		assertFalse(scope1.equals(scope2));

		assertFalse(scope1.equals(null));
		assertFalse(scope1.equals(new Object()));
		assertFalse(scope1.equals(new InstanceScope()));
		assertFalse(new InstanceScope().equals(scope1));
		assertTrue(new InstanceScope().equals(new InstanceScope()));
	}

	@Test
	public void instanceScopeConverter() {
		Converter converter = ConverterRegistry.getConverter();
		assertEquals("APPLICATION", converter.asString(InstanceScope.APPLICATION));
		assertEquals(InstanceScope.APPLICATION, converter.asObject("APPLICATION", InstanceScope.class));
	}

	// --------------------------------------------------------------------------------------------
	// APPLICATION SCOPE FACTORY

	@Test
	public void applicationScopeFactory() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		InstanceKey instanceKey = new InstanceKey(managedClass.getKey());
		ScopeFactory factory = getApplicationScopeFactory();

		assertNull(factory.getInstance(instanceKey));
		factory.persistInstance(instanceKey, new Person());

		Person p1 = (Person) factory.getInstance(instanceKey);
		Person p2 = (Person) factory.getInstance(instanceKey);

		assertNotNull(p1);
		assertNotNull(p2);
		assertEquals(p1, p2);
	}

	/** Two instances of the same managed class with APPLICATION scope should be equal even if created from different threads. */
	@Test
	public void applicationScopeFactory_CrossThreadApplicationScope() throws InterruptedException {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		final InstanceKey instanceKey = new InstanceKey(managedClass.getKey());
		final ScopeFactory factory = getApplicationScopeFactory();

		assertNull(factory.getInstance(instanceKey));
		factory.persistInstance(instanceKey, new Person());

		final Object lock = new Object();

		class TestRunnable implements Runnable {
			private Person person;

			@Override
			public void run() {
				person = (Person) factory.getInstance(instanceKey);
				synchronized (lock) {
					lock.notify();
				}
			}
		}

		TestRunnable runnable = new TestRunnable();
		Thread thread = new Thread(runnable);
		thread.start();

		synchronized (lock) {
			lock.wait(2000);
		}

		// instance created from main thread is the same as the one created from separated thread
		assertEquals(runnable.person, factory.getInstance(instanceKey));
	}

	/**
	 * Create a large number of thread and every thread invokes factory method a random number of times. All created instances
	 * are stored in a common synchronized set. After all threads finished, instances set should contain only one item. This
	 * proves that at application level instance is reused even if retrieved from different threads.
	 * 
	 * @throws Exception any exception is bubbled up.
	 */
	@Test
	public void applicationScopeFactory_StressedConcurentCreation() throws Exception {
		final ManagedClassSPI managedClass = new MockManagedClassSPI();
		final InstanceKey instanceKey = new InstanceKey(managedClass.getKey());
		final ScopeFactory factory = getApplicationScopeFactory();

		assertNull(factory.getInstance(instanceKey));
		factory.persistInstance(instanceKey, new Person());

		final int TESTS_COUNT = 1000;
		final Set<Object> instances = Collections.synchronizedSet(new HashSet<>());

		Thread[] threads = new Thread[TESTS_COUNT];
		for (int i = 0; i < TESTS_COUNT; ++i) {
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					Random random = new Random();
					int count = random.nextInt(TESTS_COUNT) + 1;
					for (int i = 0; i < count; ++i) {
						instances.add(factory.getInstance(instanceKey));
					}
				}
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join(2000);
		}

		assertEquals(1, instances.size());
	}

	private static ScopeFactory getApplicationScopeFactory() {
		return Classes.newInstance("js.container.ApplicationScopeFactory");
	}

	// --------------------------------------------------------------------------------------------
	// THREAD SCOPE FACTORY

	@Test
	public void threadScopeFactory() {
		InstanceKey instanceKey = new InstanceKey("1");
		ScopeFactory factory = getThreadScopeFactory();

		assertNull(factory.getInstance(instanceKey));
		factory.persistInstance(instanceKey, new Person());

		Person p1 = (Person) factory.getInstance(instanceKey);
		Person p2 = (Person) factory.getInstance(instanceKey);

		assertNotNull(p1);
		assertNotNull(p2);
		assertEquals(p1, p2);
	}

	@Test
	public void threadScopeFactory_clear() {
		ScopeFactory factory = getThreadScopeFactory();
		InstanceKey instanceKey = new InstanceKey("1");
		assertNull(factory.getInstance(instanceKey));

		factory.persistInstance(instanceKey, new Person());
		assertNotNull(factory.getInstance(instanceKey));
		
		factory.clear();
		assertNull(factory.getInstance(instanceKey));
	}
	
	/** Ensure thread scope factory does not use inheritable thread local. */
	@Test
	public void threadScopeFactory_NotInheriableThreadLocal() {
		// get an instance to force instances pool initialization that is lazy
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		InstanceKey instanceKey = new InstanceKey(managedClass.getKey());
		ScopeFactory factory = getThreadScopeFactory();

		assertNull(factory.getInstance(instanceKey));
		factory.persistInstance(instanceKey, new Person());
		assertNotNull(factory.getInstance(instanceKey));

		Map<ManagedClassSPI, Object> instancesPool = Classes.getFieldValue(factory, "instancesPool");
		assertFalse(instancesPool.get(managedClass) instanceof InheritableThreadLocal);
	}

	/**
	 * Create a large number of thread and every thread invokes factory method couple times, a random number. All created
	 * instances are stored in a common synchronized set. After all threads finished, instances set should contain a number of
	 * items equal with the number of created threads. This hopefully proves that every thread has its own instance.
	 * 
	 * @throws Exception any exception is bubbled up.
	 */
	@Test
	public void threadScopeFactory_StressedMultipleThreads() throws Exception {
		final ManagedClassSPI managedClass = new MockManagedClassSPI();
		final InstanceKey instanceKey = new InstanceKey(managedClass.getKey());
		final ScopeFactory factory = getThreadScopeFactory();

		final int THREADS_COUNT = 1000;
		final Set<Object> instances = Collections.synchronizedSet(new HashSet<>());

		Thread[] threads = new Thread[THREADS_COUNT];
		for (int i = 0; i < THREADS_COUNT; ++i) {
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					assertNull(factory.getInstance(instanceKey));
					factory.persistInstance(instanceKey, new Person());

					Random random = new Random();
					int count = random.nextInt(THREADS_COUNT) + 1;
					for (int i = 0; i < count; ++i) {
						instances.add(factory.getInstance(instanceKey));
					}
				}
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join(2000);
		}

		assertEquals(THREADS_COUNT, instances.size());
	}

	/**
	 * Create multiple threads, everyone with a different key. Thread factory should create a thread local object for every key,
	 * that is, internal instances pool size should have the size equals with threads count.
	 */
	@Test
	public void threadScopedFactory_MultipleKeys() throws InterruptedException {
		final ScopeFactory factory = getThreadScopeFactory();

		final int THREADS_COUNT = 1000;

		Thread[] threads = new Thread[THREADS_COUNT];
		for (int i = 0; i < THREADS_COUNT; ++i) {
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					InstanceKey instanceKey = new InstanceKey(UUID.randomUUID().toString());
					assertNull(factory.getInstance(instanceKey));
				}
			});
		}

		for (int i = 0; i < THREADS_COUNT; ++i) {
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join(2000);
		}

		assertEquals(THREADS_COUNT, ((Map<?, ?>) Classes.getFieldValue(factory, "instancesPool")).size());
	}

	@Test(expected = BugError.class)
	public void threadScopeFactory_BadPersist() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		InstanceKey instanceKey = new InstanceKey(managedClass.getKey());
		ScopeFactory factory = getThreadScopeFactory();
		factory.persistInstance(instanceKey, new Person());
	}

	private static ScopeFactory getThreadScopeFactory() {
		return Classes.newInstance("js.container.ThreadScopeFactory");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	public static class Person {
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		@Override
		public String getKey() {
			return "1";
		}
	}
}
