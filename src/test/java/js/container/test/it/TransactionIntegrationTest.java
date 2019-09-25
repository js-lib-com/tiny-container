package js.container.test.it;

import static org.junit.Assert.fail;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import js.annotation.Inject;
import js.core.AppFactory;
import js.log.Log;
import js.log.LogFactory;
import js.transaction.Immutable;
import js.transaction.TransactionContext;
import js.transaction.Transactional;
import js.unit.TestContext;

public class TransactionIntegrationTest {
	private static final String DESCRIPTOR = "<?xml version='1.0' encoding='UTF-8'?>" + //
			"<test-config>" + //
			"	<managed-classes>" + //
			"		<transaction-manager interface='js.transaction.TransactionManager' class='js.transaction.hibernate.TransactionManagerImpl' />" + //
			"		<dao interface='js.container.test.it.TransactionIntegrationTest$Dao' class='js.container.test.it.TransactionIntegrationTest$DaoImpl' type='PROXY' />" + //
			"	</managed-classes>" + //
			"	<data-source>" + //
			"		<property name='hibernate.connection.driver_class' value='com.mysql.jdbc.Driver' />" + //
			"		<property name='hibernate.connection.url' value='jdbc:mysql://localhost:3306/test' />" + //
			"		<property name='hibernate.connection.password' value='test' />" + //
			"		<property name='hibernate.connection.username' value='test' />" + //
			"		<property name='hibernate.dialect' value='org.hibernate.dialect.MySQLDialect' />" + //
			"		<property name='hibernate.show_sql' value='true' />" + //
			"		<property name='hibernate.connection.provider_class' value='org.hibernate.connection.C3P0ConnectionProvider' />" + //
			"		<property name='hibernate.c3p0.min_size' value='5' />" + //
			"		<property name='hibernate.c3p0.max_size' value='140' />" + //
			"		<property name='hibernate.c3p0.max_statements' value='50' />" + //
			"		<property name='hibernate.c3p0.timeout' value='1800' />" + //
			// "		<mappings package='js.container.test.fixture.' />" + //
			"	</data-source>" + //
			"</test-config>";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private Dao dao;

	@Before
	public void setUp() throws Exception {
		AppFactory factory = TestContext.start(DESCRIPTOR);
		dao = factory.getInstance(Dao.class);
	}

	@Test
	public void testCommitMutableTransaction() throws Throwable {
		dao.commitMutableMethod();
	}

	@Test
	public void testRollbackMutableTransaction() throws Throwable {
		try {
			dao.rollbackMutableMethod();
			fail("Expected exception.");
		} catch (Exception expected) {
		}
	}

	@Test
	public void testCommitImmutableTransaction() throws Throwable {
		dao.commitImmutableMethod();
	}

	@Test
	public void testRollbackImmutableTransaction() throws Throwable {
		try {
			dao.rollbackImmutableMethod();
			fail("Expected exception.");
		} catch (Exception expected) {
		}
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

			Session session = database.getSession();
			SQLQuery query = session.createSQLQuery("SELECT name FROM person WHERE id=1");
			// query.setEntity("name", StandardBasicTypes.STRING);
			String name = (String) query.uniqueResult();
			Assert.assertEquals("Iulian", name);
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
}
