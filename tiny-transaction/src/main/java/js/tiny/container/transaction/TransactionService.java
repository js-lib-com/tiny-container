package js.tiny.container.transaction;

import java.lang.reflect.InvocationTargetException;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.transaction.Immutable;
import js.transaction.Mutable;
import js.transaction.Transaction;
import js.transaction.TransactionContext;
import js.transaction.Transactional;

public class TransactionService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(TransactionService.class);

	private IContainer container;

	public TransactionService() {
		log.trace("TransactionService()");
	}

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.container = container;
	}

	@Override
	public Priority getPriority() {
		return Priority.TRANSACTION;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(Transactional.class) != null) {
			return true;
		}
		if (managedMethod.getDeclaringClass().getAnnotation(Transactional.class) != null) {
			return true;
		}
		return false;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();
		ITransactionalResource transactionalResource = (ITransactionalResource) container.getInstance(TransactionContext.class);
		if (isMutable(managedMethod)) {
			return executeMutableTransaction(transactionalResource, chain, invocation);
		}
		return executeImmutableTransaction(transactionalResource, chain, invocation);
	}

	/**
	 * Helper method for mutable transaction execution.
	 * 
	 * @param managedMethod managed method to be executed into transactional scope,
	 * @param args managed method arguments.
	 * @return value returned by managed method.
	 * @throws Throwable forward any error rose by method execution.
	 */
	private Object executeMutableTransaction(ITransactionalResource transactionalResource, IInvocationProcessorsChain serviceChain, IInvocation methodInvocation) throws InvocationException {
		// store resource manager owning current transaction on current thread via transactional resource utility
		// it may happen to have multiple nested transaction on current thread

		// since all are created by the same transactional resource, all are owned by the same resource manager, and there is no
		// harm if storeResourceManager() is invoked multiple times; also performance penalty is comparable with the effort to
		// prevent this multiple write

		final IManagedMethod managedMethod = methodInvocation.method();
		final Transaction transaction = transactionalResource.createTransaction(getScheme(managedMethod));
		transactionalResource.storeResourceManager(transaction.getResourceManager());

		try {
			Object result = serviceChain.invokeNextProcessor(methodInvocation);
			transaction.commit();
			return result;
		} catch (Throwable throwable) {
			transaction.rollback();
			throw throwable(throwable, "Mutable transactional method |%s| invocation fail.", managedMethod);
		} finally {
			if (transaction.close()) {
				// it may happen to have multiple nested transaction on this thread
				// if this is the case, remove resource manager from current thread only if outermost transaction is closed
				// of course if not nested transactions, remove at once
				transactionalResource.releaseResourceManager();
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
	private Object executeImmutableTransaction(ITransactionalResource transactionalResource, IInvocationProcessorsChain serviceChain, IInvocation methodInvocation) throws InvocationException {
		final IManagedMethod managedMethod = methodInvocation.method();
		final Transaction transaction = transactionalResource.createReadOnlyTransaction(getScheme(managedMethod));
		// see mutable transaction comment
		transactionalResource.storeResourceManager(transaction.getResourceManager());

		try {
			return serviceChain.invokeNextProcessor(methodInvocation);
		} catch (Throwable throwable) {
			throw throwable(throwable, "Immutable transactional method |%s| invocation fail.", managedMethod);
		} finally {
			if (transaction.close()) {
				// see mutable transaction comment
				transactionalResource.releaseResourceManager();
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
	private static InvocationException throwable(Throwable throwable, String message, Object... args) {
		Throwable t = throwable;
		if (t instanceof InvocationException && t.getCause() != null) {
			t = t.getCause();
		}
		if (t instanceof InvocationTargetException && ((InvocationTargetException) t).getTargetException() != null) {
			t = ((InvocationTargetException) t).getTargetException();
		}
		message = String.format(message, args);
		log.dump(message, t);
		return new InvocationException(t);
	}

	private static boolean isMutable(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(Mutable.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(Immutable.class) != null) {
			return false;
		}
		// at this point business method has no transaction related annotations
		if (managedMethod.getDeclaringClass().getAnnotation(Mutable.class) != null) {
			return true;
		}
		// by default transactional class is immutable
		return false;
	}

	private static String getScheme(IManagedMethod managedMethod) {
		Transactional transactional = managedMethod.scanAnnotation(Transactional.class);
		if (transactional == null) {
			transactional = managedMethod.getDeclaringClass().getAnnotation(Transactional.class);
		}
		if (transactional == null) {
			return null;
		}
		if(transactional.schema() == null) {
			return null;
		}
		return transactional.schema().isEmpty() ? null : transactional.schema();
	}
}
