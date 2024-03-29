package com.jslib.container.transaction;

import java.lang.reflect.InvocationTargetException;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.enterprise.context.ApplicationScoped;
import com.jslib.lang.InvocationException;
import com.jslib.api.transaction.Immutable;
import com.jslib.api.transaction.Mutable;
import com.jslib.api.transaction.Transaction;
import com.jslib.api.transaction.TransactionContext;
import com.jslib.api.transaction.TransactionManager;
import com.jslib.api.transaction.Transactional;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IMethodInvocationProcessor;

public class TransactionService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(TransactionService.class);

	private IContainer container;

	@Override
	public void configure(IContainer container) {
		this.container = container;
		container.bind(TransactionManager.class).service().in(ApplicationScoped.class).build();
		container.bind(TransactionContext.class).to(TransactionalResource.class).in(ApplicationScoped.class).build();
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
		if (managedMethod.getDeclaringClass().scanAnnotation(Transactional.class) != null) {
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
		if (managedMethod.getDeclaringClass().scanAnnotation(Mutable.class) != null) {
			return true;
		}
		// by default transactional class is immutable
		return false;
	}

	private static String getScheme(IManagedMethod managedMethod) {
		Transactional transactional = managedMethod.scanAnnotation(Transactional.class);
		if (transactional == null) {
			transactional = managedMethod.getDeclaringClass().scanAnnotation(Transactional.class);
		}
		if (transactional == null) {
			return null;
		}
		if (transactional.schema() == null) {
			return null;
		}
		return transactional.schema().isEmpty() ? null : transactional.schema();
	}
}
