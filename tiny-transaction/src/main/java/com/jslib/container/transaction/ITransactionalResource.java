package com.jslib.container.transaction;

import com.jslib.api.transaction.Transaction;
import com.jslib.api.transaction.TransactionContext;
import com.jslib.api.transaction.TransactionManager;
import com.jslib.api.transaction.WorkingUnit;

/**
 * External resource that provides a transactional API. A transactional resource has two kinds of APIs: one exposed by resource
 * manager that deals with specific services and one for managing transactions, accessible via {@link Transaction} interface.
 * Service API is executed inside transactional boundaries managed by transaction API. When transaction object is retrieved from
 * transaction manager, it also contains a reference to associated resource manager.
 * <p>
 * Transactional resource stores resource manager on current thread - see {@link #storeResourceManager(Object)}, so that managed
 * method implemented by application can get resource manager reference and execute service API on it.
 * 
 * <pre>
 *    begin transaction      +-----------------+
 * --------------------------&gt;                 |
 *                  +--------|-----------+       |
 *    service API   |                    |       |
 * -----------------&gt;  Resource Manager  |       |
 *                  |                    |       |
 *                  +--------|-----------+       |
 *    commit transaction     |                   |    
 * --------------------------&gt;  Transactional    |
 *    close transaction      |  Resource         |
 * --------------------------&gt;                   |
 *                           +-------------------+
 * </pre>
 * <p>
 * After application managed method finishes its execution transaction is closed and resource manager reference is discarded
 * from thread local. Anyway, for nested transactions, {@link #releaseResourceManager()} is executed only after outermost
 * transaction was closed.
 * <p>
 * Sequence diagram for transaction and resource manager interaction. It is a simplified version and does not cover
 * {@link TransactionManager} and {@link TransactionContext} interfaces.
 * 
 * <pre>
 * +---------------+  +---------------------+  +-----------------------+  +-------------+  +-----------------+
 * | ManagedMethod |  | ManagedProxyHandler |  | TransactionalResource |  | Transaction |  | ResourceManager |
 * +-------+-------+  +-----------+---------+  +-------------+---------+  +------+------+  +----+------------+
 *         |     invoke           |                          |                   |              |
 * ~------------------------------&gt;  createTransaction       |   new             |              |      
 *         |                      +--------------------------&gt;-------------------&gt;              |
 *         |                      | storeResourceManager     |                   |              |           
 *         |                      +--------------------------&gt;                   |              |
 *         |     getSession       |                          |                   |              |
 *         &lt;-------------------------------------------------+                   |              |
 *         |     service API      |                          |                   |              |
 *         +------------------------------------------------------------------------------------&gt;
 *         |                      |  commit                  |                   |              |
 *         |                      +----------------------------------------------&gt;              |
 *         |                      |  close                   |                   |              |
 *         |                      +----------------------------------------------&gt;              |
 *         |                      |  releaseResourceManager  |                   |              |
 *         |                      +--------------------------&gt;                   |              |
 *         |                      |                          |                   |              |
 * </pre>
 * 
 * <p>
 * This interface is for container internals and should not be used by applications; it is public for testing support.
 * <p>
 * For special needs, transactional resource allows for manual transaction management. This is accomplished by delegating
 * underlying transaction manager, see {@link TransactionManager#exec(com.jslib.api.transaction.WorkingUnit, Object...)}. In sample code,
 * transactional resource creates and configures the transaction manager that takes care to execute {@link WorkingUnit} inside
 * transaction boundaries. In this example resource manager is a Hibernate Session object.
 * 
 * <pre>
 * TransactionalResource transactionalResource = Factory.getInstance(TransactionalResource.class);
 * TransactionManager transactionManager = transactionalResource.getTransactionManager();
 * 
 * Address address = transactionManager.exec(new WorkingUnit&lt;Session, Address&gt;() {
 *   public Address exec(Session session, Object... args) throws Exception {
 *     Person person = (Person)args[0];
 *     SQLQuery sql = session.createSQLQuery("SELECT * FROM address ...");
 *     ...
 *     return (Address)sql.uniqueResult();
 *   }
 * }, person);
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public interface ITransactionalResource extends TransactionContext {
	/**
	 * Low level access to transaction manager for manual transaction management. See class description for sample code.
	 * 
	 * @return transaction manager used by this transactional resource.
	 */
	TransactionManager getTransactionManager();

	/**
	 * Create mutable, that is, read-write transaction. Implementation may delegate transaction manager, see
	 * {@link TransactionManager#createTransaction()}.
	 * <p>
	 * When create a transaction is possible to request a specific transactional schema. This limits the scope of transactional
	 * resource objects that can be accessed from transaction. If not provided created transaction used implicit / global
	 * schema.
	 * 
	 * @param schema optional transactional schema, null if not used.
	 * @return created transaction.
	 */
	Transaction createTransaction(String schema);

	/**
	 * Create immutable, that is, read-only transaction. Implementation may delegate transaction manager, see
	 * {@link TransactionManager#createReadOnlyTransaction()}.
	 * <p>
	 * When create a transaction is possible to request a specific transactional schema. This limits the scope of transactional
	 * resource objects that can be accessed from transaction. If not provided created transaction used implicit / global
	 * schema.
	 * 
	 * @param schema optional transactional schema, null if not used.
	 * @return created read-only transaction.
	 */
	Transaction createReadOnlyTransaction(String schema);

	/**
	 * Store currently executing resource manager on thread local. Resource manager deals with transactional resource services.
	 * Application retrieve it from thread local, see {@link TransactionContext#getSession()} and executes its methods inside
	 * transaction boundaries.
	 * 
	 * @param resourceManager currently executing session object.
	 */
	void storeResourceManager(Object resourceManager);

	/**
	 * After transaction completes release resource manager from thread local storage. For nested transactions this method
	 * should be executed only after the outermost transaction was closed.
	 */
	void releaseResourceManager();
}