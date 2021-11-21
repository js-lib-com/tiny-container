package js.tiny.container.transaction;

import javax.annotation.PreDestroy;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.transaction.Transaction;
import js.transaction.TransactionContext;
import js.transaction.TransactionManager;

/**
 * Transactional resource implementation. See {@link ITransactionalResource} interface for transactional resource concept
 * description.
 * <p>
 * Current implementation keeps a reference to {@link TransactionManager} and delegates it for transactions creation. Also has a
 * thread local storage, {@link #resourceManagerStorage} that keeps session object references. Application code can retrieve
 * session object using {@link TransactionContext#getSession()} and execute service API on it while {@link ManagedProxyHandler}
 * takes care to provide transaction boundaries.
 * <p>
 * Because session objects are stored on an inheritable thread local storage, {@link InheritableThreadLocal}, it is legal for
 * application logic to create child threads while still being able to access session object. Anyway, keep in mind that when
 * return from transactional method container destroy transaction and session object. If method creates child threads, perhaps
 * for some parallel computation, it should wait for all child threads end before returning. Fail to do so may result in not
 * specified behavior.
 * 
 * @author Iulian Rotaru
 */
final class TransactionalResource implements ITransactionalResource {
	private static final Log log = LogFactory.getLog(ITransactionalResource.class);

	/**
	 * Transaction manager. Since is legal to have runtimes without database support, e.g. embedded systems, transaction manager
	 * is optional. In fact, transaction manager is not even implemented by this library but provided as service.
	 */
	private final TransactionManager transactionManager;

	/**
	 * Keep the reference of currently executing resource manager on thread local storage. Uses inheritable thread local so that
	 * child threads can still access the resource manager reference.
	 */
	private final ThreadLocal<Object> resourceManagerStorage = new InheritableThreadLocal<>();

	/**
	 * Construct transactional resource for application served by given factory.
	 * 
	 * @param appFactory application factory.
	 */
	public TransactionalResource(IContainer container) {
		log.trace("TransactionalResource(IContainer)");
		// uses AppFactory instead of Classes.loadService to acquire transaction manager since need scope management
		this.transactionManager = container.getOptionalInstance(TransactionManager.class);
		if (this.transactionManager == null) {
			throw new BugError("Transaction manager service not found. Ensure there is |%s| service provider on run-time.", TransactionManager.class);
		}
	}

	/** Destroy transaction manager and release all resources like caches and connection pools. */
	@PreDestroy
	public void preDestroy() {
		log.trace("preDestroy()");
		transactionManager.destroy();
	}

	@Override
	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	// --------------------------------------------------------------------------------------------
	// TRANSACTIONAL RESOURCE MANAGER REFRENCE

	/**
	 * Store the reference of the currently executing resource manager on thread local variable.
	 * 
	 * @param resourceManager currently executing resource manager.
	 */
	@Override
	public void storeResourceManager(Object resourceManager) {
		resourceManagerStorage.set(resourceManager);
	}

	/** After transaction completes release resource manager reference from thread local storage. */
	@Override
	public void releaseResourceManager() {
		resourceManagerStorage.set(null);
	}

	/**
	 * Get resource manager bound to current thread. Resource manager methods are executed inside transaction boundaries, also
	 * bound to current thread.
	 * 
	 * @return current thread resource manager.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getResourceManager() {
		return (T) resourceManagerStorage.get();
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

	// --------------------------------------------------------------------------------------------
	// tests access to private state

	ThreadLocal<Object> getResourceManagerStorage() {
		return resourceManagerStorage;
	}
}