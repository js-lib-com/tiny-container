package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import js.lang.BugError;
import js.lang.Config;
import js.lang.InvocationException;
import js.tiny.container.TransactionalResource;
import js.tiny.container.core.AppFactory;
import js.tiny.container.stub.AppFactoryStub;
import js.tiny.container.stub.TransactionManagerStub;
import js.tiny.container.stub.TransactionStub;
import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.util.Classes;

/**
 * Managed proxy handler unit test. This class focuses on general behavior and leave transactions life cycle for
 * {@link ManagedProxyHandlerTransactionUnitTest}.
 * 
 * @author Iulian Rotaru
 */
public class TransactionalResourceUnitTest {
	private MockAppFactory factory;

	@Before
	public void beforeTest() throws Exception {
		factory = new MockAppFactory();
	}

	@Test
	public void constructor() {
		factory.transactionManager = new TransactionManagerStub();
		TransactionalResource transactionalResource = createTransactionalResource(factory);
		assertEquals(factory.transactionManager, transactionalResource.getTransactionManager());
	}

	@Test(expected = BugError.class)
	public void constructorWithMissingTransactionManager() {
		createTransactionalResource(factory);
	}

	/**
	 * Ensure session storage from {@link TransactionalResource} implementation is inheritable thread local, so that child
	 * threads can still access session object.
	 */
	@Test
	public void inheritableSessionStorage() {
		factory.transactionManager = new TransactionManagerStub();
		Object transactionalResource = createTransactionalResource(factory);
		assertTrue(getSessionStorage(transactionalResource) instanceof InheritableThreadLocal);
	}

	/**
	 * Create a {@link TransactionalResource} in main thread and store a session object. Create a child thread and get the
	 * session object from thread. Session object from main thread and from child thread should be the same.
	 */
	@Test
	public void threadSessionObject() throws InterruptedException {
		factory.transactionManager = new TransactionManagerStub();
		final TransactionalResource transactionalResource = createTransactionalResource(factory);
		Object session = new Object();
		transactionalResource.storeSession(session);

		class ThreadSession {
			Object session;
		}

		final ThreadSession threadSession = new ThreadSession();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
			}
		});
		thread.start();
		thread.join();

		//assertEquals(session, threadSession.session);
	}

	@Test
	public void storeSession() {
		factory.transactionManager = new TransactionManagerStub();
		TransactionalResource transactionalResource = createTransactionalResource(factory);
		Object session = new Object();
		transactionalResource.storeSession(session);
		assertEquals(session, getSessionStorage(transactionalResource).get());
	}

	@Test
	public void releaseSession() {
		factory.transactionManager = new TransactionManagerStub();
		TransactionalResource transactionalResource = createTransactionalResource(factory);
		ThreadLocal<Object> sessionStorage = getSessionStorage(transactionalResource);
		sessionStorage.set(new Object());

		transactionalResource.releaseSession();
		assertNull(sessionStorage.get());
	}

	@Test
	public void getSession() {
		factory.transactionManager = new TransactionManagerStub();
		TransactionalResource transactionalResource = createTransactionalResource(factory);

		Object session = new Object();
		getSessionStorage(transactionalResource).set(session);

	}

	/** Configuration object should be passed to transaction manager. */
	@Test
	public void config() {
		class MockTransactionManager extends TransactionManagerStub {
			Config config;

			@Override
			public void config(Config config) {
				this.config = config;
			}
		}
		MockTransactionManager transactionManager = new MockTransactionManager();

		factory.transactionManager = transactionManager;
		Object transactionalResource = createTransactionalResource(factory);
		invokeMethod(transactionalResource, "config", new Config("test"));

		assertEquals("test", transactionManager.config.getName());
	}

	/** Pre-destroy hook should delegate transaction manager destroy. */
	@Test
	public void preDestroy() {
		class MockTransactionManager extends TransactionManagerStub {
			int destroyProbe;

			@Override
			public void destroy() {
				++destroyProbe;
			}
		}
		MockTransactionManager transactionManager = new MockTransactionManager();

		factory.transactionManager = transactionManager;
		Object transactionalResource = createTransactionalResource(factory);
		invokeMethod(transactionalResource, "preDestroy");

		assertEquals(1, transactionManager.destroyProbe);
	}

	/** Create transaction should delegate transaction manager. */
	@Test
	public void createTransaction() {
		class MockTransactionManager extends TransactionManagerStub {
			Transaction transaction = new TransactionStub();

			@Override
			public Transaction createTransaction(String schema) {
				return transaction;
			}
		}
		MockTransactionManager transactionManager = new MockTransactionManager();

		factory.transactionManager = transactionManager;
		Object transactionalResource = createTransactionalResource(factory);
		Object transaction = invokeMethod(transactionalResource, "createTransaction", (String)null);

		assertEquals(transaction, transactionManager.transaction);
	}

	/** Create read-only transaction should delegate transaction manager. */
	@Test
	public void createReadOnlyTransaction() {
		class MockTransactionManager extends TransactionManagerStub {
			Transaction transaction = new TransactionStub();

			@Override
			public Transaction createReadOnlyTransaction(String schema) {
				return transaction;
			}
		}
		MockTransactionManager transactionManager = new MockTransactionManager();

		factory.transactionManager = transactionManager;
		Object transactionalResource = createTransactionalResource(factory);
		Object transaction = invokeMethod(transactionalResource, "createReadOnlyTransaction", (String)null);

		assertEquals(transaction, transactionManager.transaction);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static TransactionalResource createTransactionalResource(AppFactory factory) {
		try {
			return Classes.newInstance("js.tiny.container.TransactionalResourceImpl", factory);
		} catch (InvocationException e) {
			if (e.getCause() instanceof BugError) {
				throw (BugError) e.getCause();
			}
			throw e;
		}
	}

	private static Object invokeMethod(Object object, String method, Object... args) {
		try {
			return Classes.invoke(object, method, args);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}

	private static ThreadLocal<Object> getSessionStorage(Object transactionalResource) {
		return Classes.getFieldValue(transactionalResource, "sessionStorage");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockAppFactory extends AppFactoryStub {
		private TransactionManager transactionManager;

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
			assertEquals(TransactionManager.class, interfaceClass);
			return (T) transactionManager;
		}
	}
}
