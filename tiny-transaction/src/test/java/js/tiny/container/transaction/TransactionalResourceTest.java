package js.tiny.container.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ManagedPreDestroy;
import js.tiny.container.core.AppFactory;
import js.tiny.container.core.OptionalConfigurable;
import js.transaction.TransactionManager;

@RunWith(MockitoJUnitRunner.class)
public class TransactionalResourceTest {
	@Mock
	private AppFactory appFactory;
	@Mock
	private TransactionManager transactionManager;

	@Before
	public void beforeTest() throws Exception {
		when(appFactory.getOptionalInstance(TransactionManager.class)).thenReturn(transactionManager);
	}

	@Test
	public void GivenTransactionManager_WhenConstructor_ThenValidState() {
		// given

		// when
		ITransactionalResource transactionalResource = new TransactionalResource(appFactory);

		// then
		assertThat(transactionalResource.getTransactionManager(), equalTo(transactionManager));
	}

	@Test(expected = BugError.class)
	public void GivenNoTransactionManager_WhenConstructor_ThenException() {
		// given
		when(appFactory.getOptionalInstance(TransactionManager.class)).thenReturn(null);

		// when
		new TransactionalResource(appFactory);

		// then
	}

	/**
	 * Ensure session storage from {@link ITransactionalResource} implementation is inheritable thread local, so that child
	 * threads can still access session object.
	 */
	@Test
	public void GivenTransactionalResource_WhenGetSessionStorage_ThenInheritable() {
		// given
		TransactionalResource transactionalResource = new TransactionalResource(appFactory);

		// when
		Object sessionStorage = transactionalResource.getResourceManagerStorage();

		// then
		assertTrue(sessionStorage instanceof InheritableThreadLocal);
	}

	/**
	 * Create a {@link ITransactionalResource} in main thread and store a session object. Create a child thread and get the
	 * session object from thread. Session object from main thread and from child thread should be the same.
	 */
	@Test
	public void GivenSessionObject_WhenRetrieveFromChildThread_ThenTheSame() throws InterruptedException {
		// given
		Object session = new Object();

		final ITransactionalResource transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.storeResourceManager(session);

		// when
		class ThreadData {
			Object session;
		}

		final ThreadData threadData = new ThreadData();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				threadData.session = transactionalResource.getResourceManager();
			}
		});
		thread.start();
		thread.join();

		// then
		assertThat(threadData.session, equalTo(session));
	}

	@Test
	public void GivenSessionObject_WhenStoreSession_ThenRetrieve() {
		// given
		ITransactionalResource transactionalResource = new TransactionalResource(appFactory);

		// when
		Object session = new Object();
		transactionalResource.storeResourceManager(session);

		// then
		assertThat(transactionalResource.getResourceManager(), equalTo(session));
	}

	@Test
	public void Given_WhenReleaseSession_ThenNull() {
		// given
		TransactionalResource transactionalResource = new TransactionalResource(appFactory);
		ThreadLocal<Object> sessionStorage = transactionalResource.getResourceManagerStorage();
		sessionStorage.set(new Object());

		// when
		transactionalResource.releaseResourceManager();

		// then
		assertNull(sessionStorage.get());
	}

	/** Configuration object should be passed to transaction manager. */
	@Test
	public void GivenConfigInstance_WhenConfig_ThenTransactionManagerConfig() throws Exception {
		// given
		Config config = new Config("test");

		// when
		OptionalConfigurable transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.config(config);

		// then
		verify(transactionManager, times(1)).config(config);
	}

	/** Pre-destroy hook should delegate transaction manager destroy. */
	@Test
	public void GivenDefaults_WhenPreDestroy_ThenDestroyTransactionManager() throws Exception {
		// given

		// when
		ManagedPreDestroy transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.preDestroy();

		// then
		verify(transactionManager, times(1)).destroy();
	}

	/** Create transaction should delegate transaction manager. */
	@Test
	public void GivenSchema_WhenCreateTransaction_ThenDelegateTransactionManager() {
		// given
		String schema = "app";

		// when
		ITransactionalResource transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.createTransaction(schema);

		//
		verify(transactionManager, times(1)).createTransaction(schema);
	}

	@Test
	public void GivenNullSchema_WhenCreateTransaction_ThenDelegateTransactionManager() {
		// given
		String schema = null;

		// when
		ITransactionalResource transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.createTransaction(schema);

		//
		verify(transactionManager, times(1)).createTransaction(schema);
	}

	/** Create read-only transaction should delegate transaction manager. */
	@Test
	public void GivenSchema_WhenCreateReadOnlyTransaction_ThenDelegateTransactionManager() {
		// given
		String schema = "app";

		// when
		ITransactionalResource transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.createReadOnlyTransaction(schema);

		//
		verify(transactionManager, times(1)).createReadOnlyTransaction(schema);
	}

	@Test
	public void GivenNullSchema_WhenCreateReadOnlyTransaction_ThenDelegateTransactionManager() {
		// given
		String schema = null;

		// when
		ITransactionalResource transactionalResource = new TransactionalResource(appFactory);
		transactionalResource.createReadOnlyTransaction(schema);

		//
		verify(transactionManager, times(1)).createReadOnlyTransaction(schema);
	}
}
