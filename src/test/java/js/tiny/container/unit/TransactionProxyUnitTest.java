package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import js.lang.Config;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.AppFactory;
import js.tiny.container.stub.TransactionManagerStub;
import js.transaction.Immutable;
import js.transaction.Transaction;
import js.transaction.TransactionContext;
import js.transaction.TransactionManager;
import js.transaction.Transactional;

/**
 * Access transactional implementation through Java Proxy. This test is similar to
 * {@link ManagedProxyHandlerTransactionUnitTest} but instead of directly invoking managed proxy handler let factory creating
 * Java Proxy instance. Also uses mock transaction manager instead of mock transactional resource.
 */
public class TransactionProxyUnitTest {
	private static final String DESCRIPTOR = "<?xml version='1.0' encoding='UTF-8'?>" + //
			"<test-config>" + //
			"	<managed-classes>" + //
			"		<transaction-manager interface='js.transaction.TransactionManager' class='js.tiny.container.unit.TransactionProxyUnitTest$MockTransactionManager' />" + //
			"		<dao interface='js.tiny.container.unit.TransactionProxyUnitTest$Dao' class='js.tiny.container.unit.TransactionProxyUnitTest$DaoImpl' type='PROXY' />" + //
			"	</managed-classes>" + //
			"</test-config>";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private MockTransactionManager manager;
	private Dao dao;

	@Before
	public void beforeTest() throws Exception {
		AppFactory factory = TestContext.start(DESCRIPTOR);
		manager = (MockTransactionManager) factory.getInstance(TransactionManager.class);
		dao = factory.getInstance(Dao.class);
	}

	@Test
	public void testCommitMutableTransaction() throws Throwable {
		dao.commitMutableMethod();

		MockTransaction transaction = manager.transaction;
		assertEquals(1, transaction.commitCounter);
		assertEquals(0, transaction.rollbackCounter);
		assertEquals(1, transaction.closeCounter);
	}

	@Test
	public void testRollbackMutableTransaction() throws Throwable {
		try {
			dao.rollbackMutableMethod();
			fail("Expected exception.");
		} catch (Exception expected) {
		}

		MockTransaction transaction = manager.transaction;
		assertEquals(0, transaction.commitCounter);
		assertEquals(1, transaction.rollbackCounter);
		assertEquals(1, transaction.closeCounter);
	}

	@Test
	public void testCommitImmutableTransaction() throws Throwable {
		dao.commitImmutableMethod();

		MockTransaction transaction = manager.transaction;
		assertEquals(0, transaction.commitCounter);
		assertEquals(0, transaction.rollbackCounter);
		assertEquals(1, transaction.closeCounter);
	}

	@Test
	public void testRollbackImmutableTransaction() throws Throwable {
		try {
			dao.rollbackImmutableMethod();
			fail("Expected exception.");
		} catch (Exception expected) {
		}

		MockTransaction transaction = manager.transaction;
		assertEquals(0, transaction.commitCounter);
		assertEquals(0, transaction.rollbackCounter);
		assertEquals(1, transaction.closeCounter);
	}

	/** Session object should be accessible from child threads. */
	@Test
	public void testThreadSessionObject() throws Throwable {
		// test assertion is performed on dao method implementation
		dao.threadSessionObject();
	}

	// ----------------------------------------------------
	// FIXTURE

	@Transactional
	private interface Dao {
		void commitMutableMethod();

		void rollbackMutableMethod() throws Exception;

		@Immutable
		void commitImmutableMethod();

		@Immutable
		void rollbackImmutableMethod() throws Exception;

		void threadSessionObject() throws InterruptedException;
	}

	@SuppressWarnings("unused")
	private static class DaoImpl implements Dao {
		private static final Log log = LogFactory.getLog(DaoImpl.class);

		@Inject
		private TransactionContext database;

		@Override
		public void commitMutableMethod() {
			log.trace("commitMutableMethod()");
			log.debug(database.getSession());

			MockDatabaseSession session = database.getSession();
			log.debug(session.query());
		}

		@Override
		public void rollbackMutableMethod() throws Exception {
			log.trace("rollbackMutableMethod()");
			throw new Exception();
		}

		@Override
		public void commitImmutableMethod() {
			log.trace("commitImmutableMethod()");
		}

		@Override
		public void rollbackImmutableMethod() throws Exception {
			log.trace("rollbackImmutableMethod()");
			throw new Exception();
		}

		@Override
		public void threadSessionObject() throws InterruptedException {
			log.trace("threadSessionObject()");

			class ThreadSession {
				Object session;
			}

			Object session = database.getSession();
			final ThreadSession threadSession = new ThreadSession();
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					threadSession.session = database.getSession();
				}
			});
			thread.start();
			thread.join();

			Assert.assertEquals(session, threadSession.session);
		}
	}

	private static class MockTransactionManager extends TransactionManagerStub {
		public MockTransaction transaction;

		@Override
		public void config(Config config) {
			if (config != null) {
				config.dump();
			}
		}

		@Override
		public Transaction createTransaction(String schema) {
			return (transaction = new MockTransaction());
		}

		@Override
		public Transaction createReadOnlyTransaction(String schema) {
			return (transaction = new MockTransaction());
		}
	}

	private static class MockTransaction implements Transaction {
		private static final Log log = LogFactory.getLog(MockTransaction.class);

		public Object session = new MockDatabaseSession();
		public int commitCounter;
		public int rollbackCounter;
		public int closeCounter;

		public MockTransaction() {
			log.trace("MockTransaction()");
		}

		@Override
		public void commit() {
			++commitCounter;
		}

		@Override
		public void rollback() {
			++rollbackCounter;
		}

		@Override
		public boolean close() {
			++closeCounter;
			return true;
		}

		@Override
		public boolean unused() {
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getSession() {
			return (T) session;
		}
	}

	private static class MockDatabaseSession {
		public String query() {
			return "query";
		}
	}
}
