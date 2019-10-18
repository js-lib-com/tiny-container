package js.tiny.container;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.ManagedPreDestroy;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.AppFactory;
import js.transaction.Transaction;
import js.transaction.TransactionContext;
import js.transaction.TransactionManager;

/**
 * Transactional resource implementation. See {@link TransactionalResource} interface for transactional resource concept
 * description.
 * <p>
 * Current implementation keeps a reference to {@link TransactionManager} and delegates it for transactions creation. Also has a
 * thread local storage, {@link #sessionStorage} that keeps session object references. Application code can retrieve session
 * object using {@link TransactionContext#getSession()} and execute service API on it while {@link ManagedProxyHandler} takes
 * care to provide transaction boundaries.
 * <p>
 * Because session objects are stored on an inheritable thread local storage, {@link InheritableThreadLocal}, it is legal for
 * application logic to create child threads while still being able to access session object. Anyway, keep in mind that when
 * return from transactional method container destroy transaction and session object. If method creates child threads, perhaps
 * for some parallel computation, it should wait for all child threads end before returning. Fail to do so may result in not
 * specified behavior.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class TransactionalResourceImpl implements TransactionalResource, OptionalConfigurable, ManagedPreDestroy {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(TransactionalResource.class);

	/**
	 * Transaction manager. Since is legal to have runtimes without database support, e.g. embedded systems, transaction manager
	 * is optional. In fact, transaction manager is not even implemented by this library but provided as service.
	 */
	private final TransactionManager transactionManager;

	/**
	 * Keep currently executing transactional session object on thread local storage. Uses inheritable thread local so that
	 * child threads can still access session object.
	 */
	private final ThreadLocal<Object> sessionStorage = new InheritableThreadLocal<>();

	/**
	 * Construct transactional resource for application served by given factory.
	 * 
	 * @param appFactory application factory.
	 */
	public TransactionalResourceImpl(AppFactory appFactory) {
		log.trace("TransactionalResource(AppFactory)");
		// uses AppFactory instead of Classes.loadService to acquire transaction manager since need scope management
		this.transactionManager = appFactory.getOptionalInstance(TransactionManager.class);
		if (this.transactionManager == null) {
			throw new BugError("Transaction manager service not found. Ensure there is |%s| service provider on run-time.", TransactionManager.class);
		}
	}

	/**
	 * Configure underlying transaction manager.
	 * 
	 * @param config configuration object.
	 * @throws ConfigException if given configuration object is not valid, as requested by transaction manager implementation.
	 * @throws Exception if configuration of the transaction manager implementation fails.
	 */
	@Override
	public void config(Config config) throws Exception {
		log.trace("config(Config.Element)");
		log.debug("Configure transaction manager |%s|.", transactionManager.getClass());
		transactionManager.config(config);
	}

	/** Destroy transaction manager and release all resources like caches and connection pools. */
	@Override
	public void preDestroy() throws Exception {
		log.trace("preDestroy()");
		transactionManager.destroy();
	}

	@Override
	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	// --------------------------------------------------------------------------------------------
	// TRANSACTIONAL SESSION

	/**
	 * Store currently executing session on thread local variable.
	 * 
	 * @param session currently executing session.
	 */
	@Override
	public void storeSession(Object session) {
		sessionStorage.set(session);
	}

	/** After transaction completes release session from thread local storage. */
	@Override
	public void releaseSession() {
		sessionStorage.set(null);
	}

	/**
	 * Get session object bound to current thread. Session object methods are executed inside transaction boundaries, also bound
	 * to current thread.
	 * 
	 * @return current thread session.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getSession() {
		return (T) sessionStorage.get();
	}

	// --------------------------------------------------------------------------------------------
	// TRANSACTION CREATION

	/**
	 * Delegates {@link TransactionManager#createTransaction()}. When create a transaction is possible to request a specific
	 * transactional schema. This limits the scope of transactional resource objects that can be accessed from transaction. If
	 * not provided created transaction used implicit / global schema.
	 * 
	 * @param schema optional transactional schema, null if not used.
	 * @return created transaction.
	 */
	@Override
	public Transaction createTransaction(String schema) {
		return transactionManager.createTransaction(schema);
	}

	/**
	 * Delegates {@link TransactionManager#createReadOnlyTransaction()}. When create a transaction is possible to request a
	 * specific transactional schema. This limits the scope of transactional resource objects that can be accessed from
	 * transaction. If not provided created transaction used implicit / global schema.
	 * 
	 * @param schema optional transactional schema, null if not used.
	 * @return created read-only transaction.
	 */
	@Override
	public Transaction createReadOnlyTransaction(String schema) {
		return transactionManager.createReadOnlyTransaction(schema);
	}
}