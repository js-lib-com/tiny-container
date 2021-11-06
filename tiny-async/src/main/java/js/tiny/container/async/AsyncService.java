package js.tiny.container.async;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Asynchronous;

import js.lang.AsyncTask;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IAnnotationsScanner;

public class AsyncService implements IMethodInvocationProcessor, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(AsyncService.class);

	public AsyncService() {
		log.trace("AsyncService()");
	}

	@Override
	public Priority getPriority() {
		return Priority.ASYNCHRONOUS;
	}

	@Override
	public List<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		List<Annotation> annotations = new ArrayList<>();

		Asynchronous asynchronous = managedClass.scanAnnotation(Asynchronous.class);
		if (asynchronous != null) {
			annotations.add(asynchronous);
		}

		return annotations;
	}

	@Override
	public List<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		List<Annotation> annotations = new ArrayList<>();

		Asynchronous asynchronous = managedMethod.scanAnnotation(Asynchronous.class);
		if (asynchronous != null) {
			annotations.add(asynchronous);
		}

		return annotations;
	}

	/**
	 * Perform method invocation in a separated thread of execution.
	 * 
	 * Current implementation is based on {@link AsyncTask} and has no means to <code>join</code> after starting asynchronous
	 * tasks. If invoker executed asynchronously fails the only option to be notified is application logger.
	 * 
	 */
	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		if (!isAsynchronous(invocation.method())) {
			return chain.invokeNextProcessor(invocation);
		}

		log.debug("Execute asynchronous |%s|.", invocation.method());
		AsyncTask<Void> asyncTask = new AsyncTask<Void>() {
			@Override
			protected Void execute() throws Throwable {
				chain.invokeNextProcessor(invocation);
				return null;
			}
		};
		asyncTask.start();

		return null;
	}

	private static boolean isAsynchronous(IManagedMethod managedMethod) {
		if (managedMethod.getAnnotation(Asynchronous.class) != null) {
			return true;
		}
		return managedMethod.getDeclaringClass().getAnnotation(Asynchronous.class) != null;
	}
}
