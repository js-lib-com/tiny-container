package js.tiny.container.interceptor;

import java.util.ArrayList;
import java.util.List;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;
import js.util.Classes;

final class InterceptorService implements IContainerService, IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(InterceptorService.class);

	private final IContainer container;

	public InterceptorService(IContainer container) {
		log.trace("InterceptorService(IContainer)");
		this.container = container;
	}

	@Override
	public IMethodInvocationProcessor.Priority getPriority() {
		return Priority.FIRST;
	}

	@Override
	public List<IServiceMeta> scan(IManagedClass managedClass) {
		List<IServiceMeta> servicesMeta = new ArrayList<>();

		Intercepted intercepted = managedClass.getAnnotation(Intercepted.class);
		if (intercepted != null) {
			servicesMeta.add(new InterceptedMeta(this, intercepted));
		}

		return servicesMeta;
	}

	@Override
	public List<IServiceMeta> scan(IManagedMethod managedMethod) {
		List<IServiceMeta> servicesMeta = new ArrayList<>();

		Intercepted intercepted = managedMethod.getAnnotation(Intercepted.class);
		if (intercepted != null) {
			servicesMeta.add(new InterceptedMeta(this, intercepted));
		}

		return servicesMeta;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(IMethodInvocationProcessorsChain serviceChain, IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException {
		final IManagedMethod managedMethod = methodInvocation.method();
		final Object[] arguments = methodInvocation.arguments();

		InterceptedMeta intercepted = managedMethod.getServiceMeta(InterceptedMeta.class);
		if (intercepted == null) {
			intercepted = managedMethod.getDeclaringClass().getServiceMeta(InterceptedMeta.class);
		}
		if (intercepted == null) {
			return serviceChain.invokeNextProcessor(methodInvocation);
		}

		Interceptor interceptor;
		Class<? extends Interceptor> interceptorClass = intercepted.value();
		if (container.isManagedClass(interceptorClass)) {
			interceptor = container.getInstance((Class<Interceptor>) interceptorClass);
		} else {
			interceptor = Classes.newInstance(interceptorClass);
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

		Object returnValue = serviceChain.invokeNextProcessor(methodInvocation);

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

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
