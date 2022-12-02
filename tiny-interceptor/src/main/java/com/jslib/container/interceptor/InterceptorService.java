package com.jslib.container.interceptor;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IMethodInvocationProcessor;
import com.jslib.container.spi.IThreadsPool;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.lang.BugError;
import com.jslib.util.Classes;
import com.jslib.util.Params;

import jakarta.ejb.Asynchronous;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;

/**
 * Container service for managed method invocation intercepting.
 * 
 * @author Iulian Rotaru
 */
public class InterceptorService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(InterceptorService.class);

	private final Map<IManagedMethod, List<TinyInterceptor<PreInvokeInterceptor>>> preInvokeInterceptorsCache = new HashMap<>();
	private final Map<IManagedMethod, List<TinyInterceptor<PostInvokeInterceptor>>> postInvokeInterceptorsCache = new HashMap<>();

	private final Map<IManagedMethod, List<JakartaInterceptor>> aroundInvokeInterceptorsCache = new HashMap<>();

	private IThreadsPool threadsPool;

	public InterceptorService() {
		log.trace("InterceptorService()");
	}

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.threadsPool = container.getInstance(IThreadsPool.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.INTERCEPTOR;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		// non standard interceptors based on container proprietary annotation
		Intercepted intercepted = managedMethod.scanAnnotation(Intercepted.class);
		if (intercepted == null) {
			intercepted = managedMethod.getDeclaringClass().scanAnnotation(Intercepted.class);
		}
		if (intercepted != null) {
			List<TinyInterceptor<PreInvokeInterceptor>> preInterceptors = new ArrayList<>();
			preInvokeInterceptorsCache.put(managedMethod, preInterceptors);

			List<TinyInterceptor<PostInvokeInterceptor>> postInterceptors = new ArrayList<>();
			postInvokeInterceptorsCache.put(managedMethod, postInterceptors);
			return true;
		}

		// standard interceptors based on Jakarata annotation
		Interceptors interceptors = managedMethod.scanAnnotation(Interceptors.class);
		if (interceptors == null) {
			interceptors = managedMethod.getDeclaringClass().scanAnnotation(Interceptors.class);
		}
		if (interceptors != null) {
			List<JakartaInterceptor> aroundInterceptors = new ArrayList<>();
			aroundInvokeInterceptorsCache.put(managedMethod, aroundInterceptors);
			return true;
		}

		return false;
	}

	@Override
	public void postCreate(IContainer container) {
		preInvokeInterceptorsCache.forEach((managedMethod, preInterceptors) -> {
			Intercepted intercepted = managedMethod.scanAnnotation(Intercepted.class);
			for (Class<? extends Interceptor> interceptorClass : intercepted.value()) {
				Interceptor instance = container.getOptionalInstance(interceptorClass);
				if (instance == null) {
					throw new ServiceConfigurationException("Missing interceptor class %s", interceptorClass);
				}
				if (instance instanceof PreInvokeInterceptor) {
					preInterceptors.add(new TinyInterceptor<>((PreInvokeInterceptor) instance, isAsynchronous(instance, "preInvoke")));
				}
			}
		});

		postInvokeInterceptorsCache.forEach((managedMethod, postInterceptors) -> {
			Intercepted intercepted = managedMethod.scanAnnotation(Intercepted.class);
			for (Class<? extends Interceptor> interceptorClass : intercepted.value()) {
				Interceptor instance = container.getOptionalInstance(interceptorClass);
				if (instance == null) {
					throw new ServiceConfigurationException("Missing interceptor class %s", interceptorClass);
				}
				if (instance instanceof PostInvokeInterceptor) {
					postInterceptors.add(new TinyInterceptor<>((PostInvokeInterceptor) instance, isAsynchronous(instance, "postInvoke")));
				}
			}
		});

		aroundInvokeInterceptorsCache.forEach((managedMethod, aroundInterceptors) -> {
			Interceptors interceptors = managedMethod.scanAnnotation(Interceptors.class);
			for (Class<?> interceptorClass : interceptors.value()) {
				Object instance = container.getOptionalInstance(interceptorClass);
				if (instance == null) {
					throw new ServiceConfigurationException("Missing interceptor class %s", interceptorClass);
				}
				for (Method method : interceptorClass.getDeclaredMethods()) {
					if (method.isAnnotationPresent(AroundInvoke.class)) {
						aroundInterceptors.add(new JakartaInterceptor(instance, method));
						break;
					}
				}
			}
		});
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
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Throwable {
		final IManagedMethod managedMethod = invocation.method();
		final Object[] arguments = invocation.arguments();

		// handle first around interceptors that are implemented with Jakarta annotations
		// if found Jakarata interceptors, Tiny Container interceptors are not executed
		List<JakartaInterceptor> aroundInterceptors = aroundInvokeInterceptorsCache.get(managedMethod);
		if (!aroundInterceptors.isEmpty()) {
			InvocationContext context = new InvocationContext(chain, invocation.instance(), managedMethod, arguments);
			for (JakartaInterceptor interceptor : aroundInvokeInterceptorsCache.get(managedMethod)) {
				log.debug("Execute around-invoke interceptor for method |{}|.", managedMethod);
				try {
					return interceptor.method.invoke(interceptor.instance, context);
				} catch (InvocationTargetException e) {
					Throwable t = e.getTargetException();
					if(t == null) {
						t = e.getCause();
					}
					if(t == null) {
						t = e;
					}
					log.error("Exception on around-invoke interceptor |{java_type}|: {exception}", interceptor.type, t);
					throw t;
				}
			}
		}

		// we step here only if there are no around interceptors declared with Jakarata annotations
		// next block deals with Tiny Container proprietary interceptors implementation

		for (TinyInterceptor<PreInvokeInterceptor> interceptor : preInvokeInterceptorsCache.get(managedMethod)) {
			log.debug("Execute pre-invoke interceptor for method |{managed_method}|.", managedMethod);
			final PreInvokeInterceptor preInvokeInterceptor = interceptor.instance;

			if (interceptor.asynchronous) {
				final String name = format("pre-invoke interceptor %s", preInvokeInterceptor.getClass().getCanonicalName());
				threadsPool.execute(name, () -> preInvokeInterceptor.preInvoke(managedMethod, arguments));
			} else {
				try {
					preInvokeInterceptor.preInvoke(managedMethod, arguments);
				} catch (Exception e) {
					log.error("Exception on pre-invoke interceptor |{java_type}|: {exception}", preInvokeInterceptor.getClass().getCanonicalName(), e);
					throw e;
				}
			}
		}

		Object returnValue = chain.invokeNextProcessor(invocation);

		for (TinyInterceptor<PostInvokeInterceptor> interceptor : postInvokeInterceptorsCache.get(managedMethod)) {
			log.debug("Execute post-invoke interceptor for method |{managed_method}|.", managedMethod);
			final PostInvokeInterceptor postInvokeInterceptor = interceptor.instance;

			if (interceptor.asynchronous) {
				final String name = format("post-invoke interceptor %s", postInvokeInterceptor.getClass().getCanonicalName());
				threadsPool.execute(name, () -> postInvokeInterceptor.postInvoke(managedMethod, arguments, returnValue));
			} else {
				try {
					postInvokeInterceptor.postInvoke(managedMethod, arguments, returnValue);
				} catch (Exception e) {
					log.error("Exception on post-invoke interceptor |{java_type}|: {exception}", postInvokeInterceptor.getClass().getCanonicalName(), e);
					throw e;
				}
			}
		}

		return returnValue;
	}

	private static class TinyInterceptor<T> {
		final T instance;
		final boolean asynchronous;

		public TinyInterceptor(T instance, boolean asynchronous) {
			this.instance = instance;
			this.asynchronous = asynchronous;
		}
	}

	private static class JakartaInterceptor {
		final Object instance;
		final String type;
		final Method method;

		/**
		 * Create interceptor data item for interceptors based on Jakarta annotations.
		 * 
		 * @param instance interceptor instance,
		 * @param method interceptor method.
		 */
		public JakartaInterceptor(Object instance, Method method) {
			Params.notNull(instance, "Interceptor instance");
			this.instance = instance;
			this.type = instance.getClass().getCanonicalName();
			this.method = method;
		}
	}
}
