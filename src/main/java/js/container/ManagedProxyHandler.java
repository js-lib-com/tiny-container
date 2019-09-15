package js.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import js.lang.InstanceInvocationHandler;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.util.Params;

/**
 * Invocation handler implementing container services for managed classes of {@link InstanceType#PROXY} type. Managed classes
 * declared as {@link InstanceType#PROXY} have the actual managed instance wrapped in a Java Proxy, dynamically generated. That
 * proxy routes all managed instance method invocations to this invocation handler {@link #invoke(Object, Method, Object[])}
 * method that, on its turn, delegates wrapped instance. Since all invocations pass through a single point is possible to add
 * cross-cutting functionality like transactions.
 * <p>
 * Current version implements directly only logic related to transactional execution and delegates {@link ManagedMethod} for
 * method level service.
 * 
 * <h3>Transactions</h3>
 * <p>
 * This class supports declarative transactions, both mutable and immutable. It uses {@link TransactionManager} to create a new
 * transaction, invoke managed instance method and commit transaction; on any exception performs rollback. This class uses
 * services provided by external implementation via {@link TransactionManager} interface and cannot work if there is no service
 * provider on run-time. Anyway, {@link TransactionalResource}, that is injected by constructor, does detect transactional
 * service provider and throws exception if not found.
 * 
 * <pre>
 *                    +--------------------+  new  +-------------+  new  +---------+
 *                    | TransactionManager +-------&gt; Transaction +-------&gt; Session |
 *                    +---------+----------+       +-------------+       +---------+
 *                              |
 *                       +------v------+        
 *                       | Transaction |
 *                       +-------------+
 *                       | session     |
 *                       +------v------+
 *                              |
 *               container      |                          TransactionalResource 
 *               +--------------v---------------+  store    +----------------------+
 * invoke -------&gt; tx = tm.createTransaction    +-----------&gt; TLS&lt;Session&gt;         |
 *        &lt;---+  |                              |           +------+------^--------+    
 *            |  |    instance                  |                  |      |         
 *            |  |    +-----------------------+ |  uses            |      | 
 *            |  |    | database              &lt;-+------------------+      | 
 *            |  |    +-----------|-----------+ |                         |
 *            |  |    | method                | |                         |
 *            |  |    | database.getSession() | |                         |
 *            |  |    +-----------------------+ |                         |
 *            |  |    | ...                   | |                         | 
 *            |  |    +-----------------------+ |                         |
 *            |  |                              |                         |
 *            |  | tx.commit / tx.rollback      |  release                |
 *            +--+ tx.close                     +-------------------------+
 *               +------------------------------+
 *               ContainerProxyHandler
 * </pre>
 * <p>
 * Here is transaction handling algorithm implemented by managed proxy handler.
 * <ol>
 * <li>ManagedProxyHandler invokes createTransaction on TransactionResource
 * <li>TransactionalResource delegates transaction creation to TransactionManager
 * <li>TransactionManager creates transaction and session object
 * <li>TransactionManager returns transaction that contains session object
 * <li>ManagedProxyHandler stores session object on TransactionalResource
 * <li>ManagedProxyHandler uses transaction instance to control transaction life cycle
 * <li>ManagedProxyHandler invokes service method
 * <li>service method uses TransactionalResource to retrieve session object
 * <li>ManagedProxyHandler close transaction and release session from TransactionalResource
 * </ol>
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class ManagedProxyHandler implements InstanceInvocationHandler<Object> {
	/** Class logger. */
	private static Log log = LogFactory.getLog(ManagedProxyHandler.class);

	/** External resource that provides a transactional API. */
	private final TransactionalResource transactionalResource;

	/**
	 * Transactional resource may support schema. This allows to limit the scope of resource objects accessible from transaction
	 * boundaries. This field store the name of the schema as set by {@link js.transaction.Transactional#schema()} managed class
	 * annotation.
	 * <p>
	 * Transactional schema is optional with default to null.
	 */
	private final String transactionalSchema;

	/** Wrapped managed class. */
	private final ManagedClassSPI managedClass;

	/** Managed instance. */
	private final Object managedInstance;

	/**
	 * Construct non-transactional proxy invocation handler for given managed instance.
	 * 
	 * @param managedClass managed class,
	 * @param managedInstance instance of managed class.
	 * @throws IllegalArgumentException if <code>managedClass</code> or <code>managedInstance</code> argument is null.
	 */
	public ManagedProxyHandler(ManagedClassSPI managedClass, Object managedInstance) {
		this(null, managedClass, managedInstance);
	}

	/**
	 * Construct transactional proxy invocation handler for given managed instance.
	 * 
	 * @param transactionalResource transactional resource,
	 * @param managedClass managed class,
	 * @param managedInstance instance of managed class.
	 * @throws IllegalArgumentException if <code>managedClass</code> is null or not transactional or
	 *             <code>managedInstance</code> is null.
	 */
	public ManagedProxyHandler(TransactionalResource transactionalResource, ManagedClassSPI managedClass, Object managedInstance) {
		Params.notNull(managedClass, "Managed class");
		if (transactionalResource != null) {
			Params.isTrue(managedClass.isTransactional(), "Managed class is not transactional");
		}
		Params.notNull(managedInstance, "Managed instance");

		this.transactionalResource = transactionalResource;
		this.transactionalSchema = managedClass.getTransactionalSchema();
		this.managedClass = managedClass;
		this.managedInstance = managedInstance;
	}

	/**
	 * Return wrapped implementation instance.
	 * 
	 * @return wrapped implementation instance.
	 * @see #managedInstance
	 */
	@Override
	public Object getWrappedInstance() {
		return managedInstance;
	}

	/**
	 * Invocation handler implementation. Every method invocation on managed class interface is routed to this point. Here
	 * actual container services are implemented and method is invoked against wrapped instance.
	 * 
	 * @param proxy instance of dynamically generated Java Proxy,
	 * @param method interface Java method about to invoke,
	 * @param args method invocation arguments.
	 * @return value returned by implementation method.
	 * @throws Throwable forward any exception rose by implementation method.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final ManagedMethodSPI managedMethod = managedClass.getManagedMethod(method);
		log.trace("Invoke |%s|.", managedMethod);

		if (!managedMethod.isTransactional()) {
			// execute managed method that is not included within transactional boundaries
			try {
				return managedMethod.invoke(managedInstance, args);
			} catch (Throwable t) {
				throw throwable(t, "Non transactional method |%s| invocation fails.", managedMethod);
			}
		}

		if (managedMethod.isImmutable()) {
			return executeImmutableTransaction(managedMethod, args);
		}
		return executeMutableTransaction(managedMethod, args);
	}

	/**
	 * Helper method for mutable transaction execution.
	 * 
	 * @param managedMethod managed method to be executed into transactional scope,
	 * @param args managed method arguments.
	 * @return value returned by managed method.
	 * @throws Throwable forward any error rose by method execution.
	 */
	private Object executeMutableTransaction(ManagedMethodSPI managedMethod, Object[] args) throws Throwable {
		// store transaction session on current thread via transactional resource utility
		// it may happen to have multiple nested transaction on current thread
		// since all are created by the same transactional resource, all are part of the same session
		// and there is no harm if storeSession is invoked multiple times
		// also performance penalty is comparable with the effort to prevent this multiple write

		Transaction transaction = transactionalResource.createTransaction(transactionalSchema);
		transactionalResource.storeSession(transaction.getSession());

		try {
			Object result = managedMethod.invoke(managedInstance, args);
			transaction.commit();
			if (transaction.unused()) {
				log.debug("Method |%s| superfluously declared transactional.", managedMethod);
			}
			return result;
		} catch (Throwable throwable) {
			transaction.rollback();
			throw throwable(throwable, "Mutable transactional method |%s| invocation fail.", managedMethod);
		} finally {
			if (transaction.close()) {
				// it may happen to have multiple nested transaction on this thread
				// if this is the case, remove session from current thread only if outermost transaction is closed
				// of course if not nested transactions, remove at once
				transactionalResource.releaseSession();
			}
		}
	}

	/**
	 * Helper method for immutable transaction execution.
	 * 
	 * @param managedMethod managed method to be executed into transactional scope,
	 * @param args managed method arguments.
	 * @return value returned by managed method.
	 * @throws Throwable forward any error rose by method execution.
	 */
	private Object executeImmutableTransaction(ManagedMethodSPI managedMethod, Object[] args) throws Throwable {
		Transaction transaction = transactionalResource.createReadOnlyTransaction(transactionalSchema);
		// see mutable transaction comment
		transactionalResource.storeSession(transaction.getSession());

		try {
			Object result = managedMethod.invoke(managedInstance, args);
			if (transaction.unused()) {
				log.debug("Method |%s| superfluously declared transactional.", managedMethod);
			}
			return result;
		} catch (Throwable throwable) {
			throw throwable(throwable, "Immutable transactional method |%s| invocation fail.", managedMethod);
		} finally {
			if (transaction.close()) {
				// see mutable transaction comment
				transactionalResource.releaseSession();
			}
		}
	}

	/**
	 * Prepare given throwable and dump it to logger with formatted message. Return prepared throwable. If throwable is
	 * {@link InvocationTargetException} or its unchecked related version, {@link InvocationException} replace it with root
	 * cause.
	 * 
	 * @param throwable throwable instance,
	 * @param message formatted error message,
	 * @param args optional formatted message arguments.
	 * @return prepared throwable.
	 */
	private static Throwable throwable(Throwable throwable, String message, Object... args) {
		Throwable t = throwable;
		if (t instanceof InvocationException && t.getCause() != null) {
			t = t.getCause();
		}
		if (t instanceof InvocationTargetException && ((InvocationTargetException) t).getTargetException() != null) {
			t = ((InvocationTargetException) t).getTargetException();
		}
		message = String.format(message, args);
		log.dump(message, t);
		return t;
	}
}
