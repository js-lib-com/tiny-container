package com.jslib.container.async;

import java.util.concurrent.Future;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IMethodInvocationProcessor;
import com.jslib.container.spi.IThreadsPool;
import com.jslib.container.spi.ServiceConfigurationException;

import jakarta.ejb.Asynchronous;
import com.jslib.lang.AsyncTask;
import com.jslib.util.Types;

/**
 * Container service for method asynchronous execution.
 * 
 * @author Iulian Rotaru
 */
public class AsyncService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(AsyncService.class);

	private IThreadsPool threadsPool;

	public AsyncService() {
		log.trace("AsyncService()");
	}

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.threadsPool = container.getInstance(IThreadsPool.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.ASYNCHRONOUS;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES) == null) {
			return false;
		}
		if (managedMethod.isStatic()) {
			throw new ServiceConfigurationException("Asynchronous method |%s| cannot be static.", managedMethod);
		}
		if (managedMethod.isVoid()) {
			if (managedMethod.getExceptionTypes().length != 0) {
				throw new ServiceConfigurationException("Void asynchronous method |%s| cannot have checked exception(s).", managedMethod);
			}
			return true;
		}
		if (!Types.isKindOf(managedMethod.getReturnType(), Future.class)) {
			throw new ServiceConfigurationException("Invalid asynchronous method |%s| return type. Should be void or Future.", managedMethod);
		}
		return true;
	}

	/**
	 * Perform method invocation in a separated thread of execution.
	 * 
	 * Current implementation is based on {@link AsyncTask} and has no means to <code>join</code> after starting asynchronous
	 * tasks. If invoker executed asynchronously fails the only option to be notified is application logger.
	 * 
	 */
	@Override
	public Object onMethodInvocation(final IInvocationProcessorsChain chain, final IInvocation invocation) throws Throwable {
		final String methodName = invocation.method().toString();
		log.debug("Execute asynchronous |{java_method}|.", methodName);

		if (invocation.method().isVoid()) {
			threadsPool.execute(methodName, () -> chain.invokeNextProcessor(invocation));
			return null;
		}

		// at this point we know that invocation method returns a future
		return threadsPool.submit(methodName, () -> {
			try {
				return (Future<?>) chain.invokeNextProcessor(invocation);
			} catch (Throwable e) {
				log.error(e);
				return null;
			}
		});
	}
}
