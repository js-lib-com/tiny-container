package js.tiny.container.transaction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;
import js.transaction.Immutable;
import js.transaction.Mutable;
import js.transaction.Transaction;
import js.transaction.TransactionContext;
import js.transaction.Transactional;

final class TransactionService implements IMethodInvocationProcessor, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(TransactionService.class);

	private final IContainer container;

	public TransactionService(IContainer container) {
		log.trace("TransactionService(IContainer)");
		this.container = container;
	}

	@Override
	public Priority getPriority() {
		return Priority.TRANSACTION;
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedClass<?> managedClass) {
		List<IServiceMeta> servicesMeta = new ArrayList<>();

		Transactional transactional = managedClass.getAnnotation(Transactional.class);
		if (transactional != null) {
			servicesMeta.add(new TransactionalMeta(this, transactional));
		}

		Immutable immutable = managedClass.getAnnotation(Immutable.class);
		if (immutable != null) {
			servicesMeta.add(new ImmutableMeta(this));
		}

		return servicesMeta;
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		List<IServiceMeta> servicesMeta = new ArrayList<>();

		Transactional transactional = managedMethod.getAnnotation(Transactional.class);
		if (transactional != null) {
			servicesMeta.add(new TransactionalMeta(this, transactional));
		}

		Immutable immutable = managedMethod.getAnnotation(Immutable.class);
		if (immutable != null) {
			servicesMeta.add(new ImmutableMeta(this));
		}

		Mutable mutable = managedMethod.getAnnotation(Mutable.class);
		if (mutable != null) {
			servicesMeta.add(new MutableMeta(this));
		}

		return servicesMeta;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();
		if (!isTransactional(managedMethod)) {
			return chain.invokeNextProcessor(invocation);
		}

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

	private static boolean isTransactional(IManagedMethod managedMethod) {
		if (managedMethod.getServiceMeta(TransactionalMeta.class) != null) {
			return true;
		}
		if (managedMethod.getDeclaringClass().getServiceMeta(TransactionalMeta.class) != null) {
			return true;
		}
		return false;
	}

	private static boolean isMutable(IManagedMethod managedMethod) {
		if (managedMethod.getServiceMeta(MutableMeta.class) != null) {
			return true;
		}
		if (managedMethod.getServiceMeta(ImmutableMeta.class) != null) {
			return false;
		}
		// at this point business method has no transaction related annotations
		if (managedMethod.getDeclaringClass().getServiceMeta(MutableMeta.class) != null) {
			return true;
		}
		// by default transactional class is immutable
		return false;
	}

	private static String getScheme(IManagedMethod managedMethod) {
		TransactionalMeta transactional = managedMethod.getServiceMeta(TransactionalMeta.class);
		if (transactional == null) {
			transactional = managedMethod.getDeclaringClass().getServiceMeta(TransactionalMeta.class);
		}
		if (transactional == null) {
			return null;
		}
		return transactional.schema();
	}
}
