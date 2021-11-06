package js.tiny.container.interceptor;

import java.lang.annotation.Annotation;
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
import js.tiny.container.spi.IAnnotationsScanner;
import js.util.Classes;

public class InterceptorService implements IMethodInvocationProcessor, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(InterceptorService.class);

	private IContainer container;

	public InterceptorService() {
		log.trace("InterceptorService()");
	}

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.container = container;
	}

	@Override
	public Priority getPriority() {
		return Priority.INTERCEPTOR;
	}

	@Override
	public List<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		List<Annotation> annotations = new ArrayList<>();

		Intercepted intercepted = managedClass.scanAnnotation(Intercepted.class);
		if (intercepted != null) {
			annotations.add(intercepted);
		}

		return annotations;
	}

	@Override
	public List<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		List<Annotation> annotations = new ArrayList<>();

		Intercepted intercepted = managedMethod.scanAnnotation(Intercepted.class);
		if (intercepted != null) {
			annotations.add(intercepted);
		}

		return annotations;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();
		final Object[] arguments = invocation.arguments();

		Intercepted intercepted = managedMethod.getAnnotation(Intercepted.class);
		if (intercepted == null) {
			intercepted = managedMethod.getDeclaringClass().getAnnotation(Intercepted.class);
		}
		if (intercepted == null) {
			return chain.invokeNextProcessor(invocation);
		}

		Interceptor interceptor = container.getOptionalInstance(intercepted.value());
		if (interceptor == null) {
			interceptor = Classes.newInstance(intercepted.value());
		}

		if (interceptor instanceof PreInvokeInterceptor) {
			log.debug("Execute pre-invoke interceptor for method |%s|.", managedMethod);
			PreInvokeInterceptor preInvokeInterceptor = (PreInvokeInterceptor) interceptor;
			try {
				preInvokeInterceptor.preInvoke(managedMethod, arguments);
			} catch (Exception e) {
				log.error("Exception on pre-invoke interceptor for method |%s|: %s", managedMethod, e);
				throw new InvocationException(e);
			}
		}

		Object returnValue = chain.invokeNextProcessor(invocation);

		if (interceptor instanceof PostInvokeInterceptor) {
			log.debug("Execute post-invoke interceptor for method |%s|.", managedMethod);
			PostInvokeInterceptor postInvokeInterceptor = (PostInvokeInterceptor) interceptor;
			try {
				postInvokeInterceptor.postInvoke(managedMethod, arguments, returnValue);
			} catch (Exception e) {
				log.error("Exception on post-invoke interceptor for method |%s|: %s", managedMethod, e);
				throw new InvocationException(e);
			}
		}
		return returnValue;
	}
}
