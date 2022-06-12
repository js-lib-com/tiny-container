package js.tiny.container.interceptor;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Asynchronous;
import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IThreadsPool;
import js.util.Classes;

/**
 * Container service for managed method invocation intercepting.
 * 
 * @author Iulian Rotaru
 */
public class InterceptorService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(InterceptorService.class);

	private final Map<IManagedMethod, List<CacheItem<PreInvokeInterceptor>>> preInvokeInterceptorsCache = new HashMap<>();
	private final Map<IManagedMethod, List<CacheItem<PostInvokeInterceptor>>> postInvokeInterceptorsCache = new HashMap<>();

	private IContainer container;
	private IThreadsPool threadsPool;

	public InterceptorService() {
		log.trace("InterceptorService()");
	}

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.container = container;
		this.threadsPool = container.getInstance(IThreadsPool.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.INTERCEPTOR;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		Intercepted intercepted = managedMethod.scanAnnotation(Intercepted.class);
		if (intercepted == null) {
			intercepted = managedMethod.getDeclaringClass().scanAnnotation(Intercepted.class);
		}
		if (intercepted == null) {
			return false;
		}

		List<CacheItem<PreInvokeInterceptor>> preInterceptors = new ArrayList<>();
		preInvokeInterceptorsCache.put(managedMethod, preInterceptors);

		List<CacheItem<PostInvokeInterceptor>> postInterceptors = new ArrayList<>();
		postInvokeInterceptorsCache.put(managedMethod, postInterceptors);

		for (Class<? extends Interceptor> interceptorClass : intercepted.value()) {
			Interceptor instance = container.getOptionalInstance(interceptorClass);
			if (instance == null) {
				instance = Classes.newInstance(interceptorClass);
			}

			if (instance instanceof PreInvokeInterceptor) {
				preInterceptors.add(new CacheItem<>((PreInvokeInterceptor) instance, isAsynchronous(instance, "preInvoke")));
			}

			if (instance instanceof PostInvokeInterceptor) {
				postInterceptors.add(new CacheItem<>((PostInvokeInterceptor) instance, isAsynchronous(instance, "postInvoke")));
			}
		}
		return true;
	}

	private static boolean isAsynchronous(Object instance, String methodName) {
		try {
			Method method = Classes.findMethod(instance.getClass(), methodName);
			return method.getAnnotation(Asynchronous.class) != null;
		} catch (NoSuchMethodException e) {
			throw new BugError(e);
		}
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();
		final Object[] arguments = invocation.arguments();

		for (CacheItem<PreInvokeInterceptor> interceptor : preInvokeInterceptorsCache.get(managedMethod)) {
			log.debug("Execute pre-invoke interceptor for method |%s|.", managedMethod);
			final PreInvokeInterceptor preInvokeInterceptor = interceptor.instance;

			if (interceptor.asynchronous) {
				final String name = format("pre-invoke interceptor %s", preInvokeInterceptor.getClass().getCanonicalName());
				threadsPool.execute(name, () -> preInvokeInterceptor.preInvoke(managedMethod, arguments));
			} else {
				try {
					preInvokeInterceptor.preInvoke(managedMethod, arguments);
				} catch (Exception e) {
					log.error("Exception on pre-invoke interceptor |%s|: %s", preInvokeInterceptor.getClass().getCanonicalName(), e);
					throw new InvocationException(e);
				}
			}
		}

		Object returnValue = chain.invokeNextProcessor(invocation);

		for (CacheItem<PostInvokeInterceptor> interceptor : postInvokeInterceptorsCache.get(managedMethod)) {
			log.debug("Execute post-invoke interceptor for method |%s|.", managedMethod);
			final PostInvokeInterceptor postInvokeInterceptor = interceptor.instance;

			if (interceptor.asynchronous) {
				final String name = format("post-invoke interceptor %s", postInvokeInterceptor.getClass().getCanonicalName());
				threadsPool.execute(name, () -> postInvokeInterceptor.postInvoke(managedMethod, arguments, returnValue));
			} else {
				try {
					postInvokeInterceptor.postInvoke(managedMethod, arguments, returnValue);
				} catch (Exception e) {
					log.error("Exception on post-invoke interceptor |%s|: %s", postInvokeInterceptor.getClass().getCanonicalName(), e);
					throw new InvocationException(e);
				}
			}
		}

		return returnValue;
	}

	private static class CacheItem<T extends Interceptor> {
		final T instance;
		final boolean asynchronous;

		public CacheItem(T instance, boolean asynchronous) {
			this.instance = instance;
			this.asynchronous = asynchronous;
		}
	}
}
