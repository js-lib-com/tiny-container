package js.tiny.container;


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remote;
import javax.interceptor.Interceptors;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import js.lang.AsyncTask;
import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.SyntaxException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.SecurityContext;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.util.Classes;
import js.util.Params;
import js.util.Strings;
import js.util.Types;

/**
 * Managed method provides method level services. A managed method is a thin wrapper around Java reflective method. It has
 * methods to retrieve internal state and miscellaneous flags but the real added piece of functionality is
 * {@link #invoke(Object, Object...)} method. It is in charge, of course beside executing wrapped Java method, with interceptors
 * and invocation meters handling. For details please see {@link Interceptor} and {@link InvocationMeter} interfaces.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class ManagedMethod implements IManagedMethod {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(IManagedMethod.class);

	/** Format string for managed method simple name, without class name. */
	private static final String SIMPLE_NAME_FORMAT = "%s(%s)";

	/** Format string for managed method fully qualified name. */
	private static final String QUALIFIED_NAME_FORMAT = "%s#" + SIMPLE_NAME_FORMAT;

	/** Back reference to parent container. */
	private final IContainer container;

	/** The managed class declaring this managed method. */
	private final IManagedClass declaringClass;

	/**
	 * Wrapped Java reflective method. This method instance reflects the method declared by managed class interface, see
	 * {@link IManagedClass#getInterfaceClass()}.
	 */
	private final Method method;

	/**
	 * Request URI path for this managed method configured by {@link Path} annotation. If annotation is missing this request
	 * path is initialized with method name converted to dashed case. Initialization is performed by
	 * {@link #setRequestPath(String)}.
	 */
	private String requestPath;

	/** Flag indicating if method should be executed into transactional context. */
	private boolean transactional;

	/**
	 * Transactional method marked as immutable, that is, read-only. By default method is mutable till explicitly set by
	 * {@link #setImmutable(boolean)}.
	 */
	private boolean immutable;

	/**
	 * A managed method is remotely accessible, also known as net method, if is annotated with {@link Remote} or declaring class
	 * is remotely accessible.
	 */
	private boolean remotelyAccessible;

	/** Flag for asynchronous method execution. If true, method invocation is delegated to {@link AsyncInvoker}. */
	private boolean asynchronous;

	/**
	 * EJB3.1 17.3.2.2 - The Bean Provider or Application Assembler can indicate that all roles are permitted to execute one or
	 * more specified methods (i.e., the methods should not be “checked” for authorization prior to invocation by the
	 * container). The unchecked element is used instead of a role name in the method-permission element to indicate that all
	 * roles are permitted.
	 */
	private boolean unchecked;

	/**
	 * Invoker strategy, initialized at this managed method construction. If managed method is created without interceptor
	 * class, which is the commons case, this invoker is initialized to {@link DefaultInvoker}; if managed method is created
	 * with interceptor class uses {@link InterceptedInvoker}.
	 */
	private Invoker invoker;

	/** Managed method signature, mainly for debugging. */
	private final String signature;

	/** Method invocation arguments processor. */
	private final ArgumentsProcessor argumentsProcessor;

	/**
	 * Optional invocation meter used to record invocations and errors count and processing time. By default meter is not used
	 * and is null. It is created on the fly by {@link #getMeter()}, supposedly called only if application instrumentation is
	 * active.
	 */
	private Meter meter;

	private final Map<Class<? extends IServiceMeta>, IServiceMeta> serviceMetas = new HashMap<>();

	/**
	 * Roles allowed to invoke this managed method. If empty and if this method is private all authenticated users are
	 * authorized.
	 */
	private String[] roles = new String[0];

	/** Returned content type initialized from {@link Produces} annotation. */
	private String returnContentType;

	/**
	 * Construct a managed method. This is a convenient constructor that just delegates
	 * {@link #ManagedMethod(IManagedClass, Class, Method)} with null interceptor class.
	 * 
	 * @param declaringClass declaring managed class,
	 * @param method Java reflective method wrapped by this managed method.
	 */
	public ManagedMethod(IManagedClass declaringClass, Method method) {
		this(declaringClass, null, method);
	}

	/**
	 * Construct a managed method with optional invocation interceptor. Store declaring class and Java reflective method,
	 * initialize this managed method signature and arguments processor.
	 * <p>
	 * If method invocation interceptor is present initialize {@link #invoker} field with {@link InterceptedInvoker}; otherwise
	 * uses {@link DefaultInvoker}.
	 * 
	 * @param declaringClass declaring managed class,
	 * @param methodInterceptor optional method invocation interceptor, possible null,
	 * @param method Java reflective method wrapped by this managed method.
	 */
	public ManagedMethod(IManagedClass declaringClass, Class<? extends Interceptor> methodInterceptor, Method method) {
		this.container = declaringClass.getContainer();
		this.declaringClass = declaringClass;
		this.method = method;
		this.method.setAccessible(true);

		invoker = methodInterceptor == null ? new DefaultInvoker() : new InterceptedInvoker(methodInterceptor);

		List<String> formalParameters = new ArrayList<String>();
		for (Class<?> formalParameter : method.getParameterTypes()) {
			formalParameters.add(formalParameter.getSimpleName());
		}
		signature = String.format(QUALIFIED_NAME_FORMAT, method.getDeclaringClass().getName(), method.getName(), Strings.join(formalParameters, ','));

		argumentsProcessor = new ArgumentsProcessor();
	}

	// --------------------------------------------------------------------------------------------
	// PACKAGE METHODS

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IServiceMeta> T getServiceMeta(Class<T> type) {
		return (T) serviceMetas.get(type);
	}

	/**
	 * Set this method request URI path, that is, the path component by which this method is referred into request URI. If given
	 * request URI path is null uses method name converted to dashed case.
	 * 
	 * @param requestPath request URI path for this method, possible null.
	 */
	void setRequestPath(String requestPath) {
		this.requestPath = requestPath != null ? requestPath : Strings.memberToDashCase(method.getName());
	}

	/**
	 * Set remote accessibility flag.
	 * 
	 * @param remotelyAccessible remote accessibility flag.
	 */
	void setRemotelyAccessible(boolean remotelyAccessible) {
		this.remotelyAccessible = remotelyAccessible;
	}

	void setUnchecked(boolean unchecked) {
		this.unchecked = unchecked;
	}

	/**
	 * Set this managed method transactional state.
	 * 
	 * @param transactional transactional flag.
	 */
	void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

	/**
	 * Set transaction immutable state.
	 * 
	 * @param immutable immutable flag.
	 */
	void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	/**
	 * Set asynchronous mode. If <code>asynchronous</code> arguments is true, internal {@link #invoker} is decorated with
	 * {@link AsyncInvoker}; this process is irreversible.
	 * 
	 * @param asynchronous asynchronous mode flag.
	 */
	void setAsynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
		if (asynchronous) {
			invoker = new AsyncInvoker(invoker);
		}
	}

//	void setSchedule(Schedule schedule) {
//		ScheduleMeta scheduleMeta = new ScheduleMeta();
//		scheduleMeta.second(schedule.second());
//		scheduleMeta.minute(schedule.minute());
//		scheduleMeta.hour(schedule.hour());
//		scheduleMeta.dayOfMonth(schedule.dayOfMonth());
//		scheduleMeta.dayOfWeek(schedule.dayOfWeek());
//		scheduleMeta.month(schedule.month());
//		scheduleMeta.year(schedule.year());
//
//		serviceMetas.put(ScheduleMeta.class, scheduleMeta);
//		this.remotelyAccessible = false;
//	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> type) {
		T annotation = method.getAnnotation(type);
		if (annotation == null) {
			try {
				annotation = declaringClass.getImplementationClass().getMethod(method.getName(), method.getParameterTypes()).getAnnotation(type);
			} catch (NoSuchMethodException | SecurityException e) {
			}
			if (annotation == null) {
				for (Class<?> interfaceClass : declaringClass.getInterfaceClasses()) {
					try {
						annotation = interfaceClass.getMethod(method.getName(), method.getParameterTypes()).getAnnotation(type);
						if (annotation != null) {
							return annotation;
						}
					} catch (NoSuchMethodException unused) {
					}
				}
			}
		}
		return annotation;
	}

	void addServiceMeta(IServiceMeta serviceMeta) {
		serviceMetas.put(serviceMeta.getClass(), serviceMeta);
	}

	void setRoles(String[] roles) {
		Params.notNullOrEmpty(roles, "Roles");
		this.unchecked = false;
		this.roles = roles;
	}

	/**
	 * Set method returned content type from {@link Produces} annotation. Given content type value should be accepted by
	 * {@link ContentType#valueOf(String)}.
	 * 
	 * @param contentType content type to be used on HTTP response.
	 * @throws SyntaxException if <code>value</code> is not a valid content type.
	 * @see ContentType
	 */
	void setReturnContentType(String contentType) throws SyntaxException {
		this.returnContentType = contentType;
	}

	/**
	 * Enable instrumentation on the fly and return this method invocation meter.
	 * 
	 * @return this method invocation meter.
	 * @see meter
	 */
	Meter getMeter() {
		// creating meter on the fly is not dangerous and need not be synchronized
		// this assumption is related to #invoke(Object, Object...) method logic
		// if change invoke logic it may need to add synchronization here too

		if (meter == null) {
			meter = new Meter(method);
		}
		return meter;
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED METHOD SPI

	@Override
	public IManagedClass getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public Method getMethod() {
		return method;
	}

	@Override
	public Type[] getParameterTypes() {
		// there are rumors that generic type information is not preserved in byte code but not confirmed
		// this library does believe documentation and relies on next excerpt from Method.getGenericParameterTypes API:

		// If a formal parameter type is a parameterized type, the Type object returned for it must accurately reflect the
		// actual type parameters used in the source code.

		return method.getGenericParameterTypes();
	}

	@Override
	public Type getReturnType() {
		return method.getGenericReturnType();
	}

	@Override
	public String getReturnContentType() {
		return returnContentType;
	}

	/**
	 * Invoke managed method and applies method level services. Delegates this managed method {@link #invoker}; accordingly
	 * selected strategy invoker can be {@link DefaultInvoker} or {@link InterceptedInvoker}, if this managed method is
	 * annotated with {@link Interceptor}. Also takes care to update {@link #meter}.
	 * 
	 * @param object managed instance against which method is executed,
	 * @param args optional managed method invocation arguments.
	 * @param <T> returned value type.
	 * @return value returned by method or null for void.
	 * @throws AuthorizationException if method is private and {@link SecurityContext} is not authenticated.
	 * @throws IllegalArgumentException if invocation arguments does not match method signature.
	 * @throws InvocationException if method execution fails for whatever reason.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(Object object, Object... args) throws AuthorizationException, IllegalArgumentException, InvocationException {
		if (remotelyAccessible && !unchecked && !container.isAuthorized(roles)) {
			log.info("Reject not authenticated access to |%s|.", method);
			throw new AuthorizationException();
		}

		// arguments processor converts <args> to empty array if it is null
		// it can be null if on invocation chain there is Proxy invoked with no arguments
		args = argumentsProcessor.preProcessArguments(this, args);

		if (object instanceof Proxy) {
			// if object is a Java Proxy does not apply method services implemented by below block
			// instead directly invoke Java method on the Proxy instance
			// container will call again this method but with the real object instance, in which case executes the next logic
			try {
				return (T) method.invoke(object, args);
			} catch (InvocationTargetException e) {
				throw new InvocationException(e.getTargetException());
			} catch (IllegalAccessException e) {
				throw new BugError("Illegal access on method with accessibility set true.");
			}
		}

		if (meter == null) {
			try {
				return (T) invoker.invoke(object, args);
			} catch (InvocationTargetException e) {
				throw new InvocationException(e.getTargetException());
			} catch (IllegalAccessException e) {
				throw new BugError("Illegal access on method with accessibility set true.");
			}
		}

		meter.incrementInvocationsCount();
		meter.startProcessing();
		T returnValue = null;
		try {
			returnValue = (T) invoker.invoke(object, args);
		} catch (InvocationTargetException e) {
			meter.incrementExceptionsCount();
			throw new InvocationException(e.getTargetException());
		} catch (IllegalAccessException e) {
			// this condition is a bug; do not increment exceptions count
			throw new BugError("Illegal access on method with accessibility set true.");
		}
		meter.stopProcessing();
		return returnValue;
	}

	@Override
	public String getRequestPath() {
		if (!remotelyAccessible) {
			throw new BugError("Attempt to retrieve request URI path from local managed method |%s|.", this);
		}
		return requestPath;
	}

	@Override
	public boolean isVoid() {
		return Types.isVoid(method.getReturnType());
	}

	@Override
	public boolean isRemotelyAccessible() {
		return remotelyAccessible;
	}

	@Override
	public boolean isUnchecked() {
		return unchecked;
	}

	@Override
	public boolean isTransactional() {
		return transactional;
	}

	@Override
	public boolean isImmutable() {
		return immutable;
	}

	@Override
	public boolean isAsynchronous() {
		return asynchronous;
	}

	/**
	 * Returns a string describing this managed method.
	 */
	@Override
	public String toString() {
		return signature;
	}

	// --------------------------------------------------------------------------------------------
	// INVOKER CLASSES

	/**
	 * Nested utility interface used to invoke outer managed method while adding method level services. This interface follows a
	 * strategy pattern and is used to define behaviors implemented by inner classes:
	 * <ol>
	 * <li>{@link DefaultInvoker} default implementation used when no additional services are defined,
	 * <li>{@link InterceptedInvoker} implements the actual intercepted invocation for managed methods tagged with
	 * {@link Interceptors} annotation,
	 * <li>{@link AsyncInvoker} execute method in a separated thread of execution.
	 * </ol>
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static interface Invoker {
		/**
		 * Invoke managed method adding specific services. Services are implementation specific.
		 * 
		 * @param object instance on which managed method is invoked,
		 * @param arguments invocation arguments.
		 * @return value returned by managed method execution.
		 * @throws IllegalArgumentException if invocation arguments does not match method formal parameters,
		 * @throws IllegalAccessException never happen but needed by Java method signature,
		 * @throws InvocationTargetException if method invocation fails.
		 */
		Object invoke(Object object, Object[] arguments) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;
	}

	/**
	 * Default invoker used when no additional services are defined on outer managed method. This is the default {@link Invoker}
	 * implementation.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private final class DefaultInvoker implements Invoker {
		/**
		 * Invoke Java reflective method from outer managed method without any fuss.
		 * 
		 * @param object instance on which method is invoked,
		 * @param args invocation arguments.
		 * @return value returned by method execution.
		 * @throws IllegalArgumentException if invocation arguments does not match method formal parameters,
		 * @throws IllegalAccessException never happen but needed by Java method signature,
		 * @throws InvocationTargetException if method invocation fails.
		 */
		@Override
		public Object invoke(Object object, Object[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			return ManagedMethod.this.method.invoke(object, args);
		}
	};

	/**
	 * Intercepted invoker used for managed method tagged as intercepted. This invoker constructor has an interceptor class as
	 * argument that is used to initialize interceptor instance, {@link #interceptor}. Interceptor instance can be fresh created
	 * or reused, if interceptor is a managed class with non local scope.
	 * <p>
	 * Interceptor execution occurs on {@link #invoke(Object, Object[])} execution either before or after actual method
	 * invocation, depending on interface interceptor is implementing. It is legal for interceptor to implement both
	 * {@link PreInvokeInterceptor} and {@link PostInvokeInterceptor} interfaces.
	 * 
	 * @version final
	 */
	private final class InterceptedInvoker implements Invoker {
		/**
		 * Interceptor instance. If interceptor class is managed this instance can be reused depending on managed class scope.
		 * Otherwise it is created at every invoker construction.
		 */
		private final Interceptor interceptor;

		/**
		 * Initialize internal interceptor instance for given interceptor class. If interceptor class is managed, instance can
		 * be reused if instance scope is not {@link InstanceScope#LOCAL}. Otherwise a new interceptor instance is created.
		 * 
		 * @param interceptorClass interceptor class, managed or plain Java class.
		 */
		@SuppressWarnings("unchecked")
		public InterceptedInvoker(Class<? extends Interceptor> interceptorClass) {
			if (container.isManagedClass(interceptorClass)) {
				interceptor = container.getInstance((Class<? super Interceptor>) interceptorClass);
			} else {
				interceptor = Classes.newInstance(interceptorClass);
			}
		}

		/**
		 * Invoke outer managed method and returns execution value. Takes care to execute interceptor instance before, after or
		 * both before and after method execution, depending on interceptor implemented interfaces.
		 * 
		 * @param object instance on which method is invoked,
		 * @param args invocation arguments.
		 * @return value returned by method execution.
		 * @throws IllegalArgumentException if invocation arguments does not match method formal parameters,
		 * @throws IllegalAccessException never happen but needed by Java method signature,
		 * @throws InvocationTargetException if method invocation or interceptor execution fails. Exception target value is
		 *             execution fail root cause.
		 */
		@Override
		public Object invoke(Object object, Object[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			final IManagedMethod managedMethod = ManagedMethod.this;

			if (interceptor instanceof PreInvokeInterceptor) {
				log.debug("Execute pre-invoke interceptor for method |%s|.", managedMethod);
				PreInvokeInterceptor preInvokeInterceptor = (PreInvokeInterceptor) interceptor;
				try {
					preInvokeInterceptor.preInvoke(managedMethod, args);
				} catch (Exception e) {
					log.error("Exception on pre-invoke interceptor for method |%s|: %s", managedMethod, e);
					throw new InvocationTargetException(e);
				}
			}

			Object returnValue = ManagedMethod.this.method.invoke(object, args);

			if (interceptor instanceof PostInvokeInterceptor) {
				log.debug("Execute post-invoke interceptor for method |%s|.", managedMethod);
				PostInvokeInterceptor postInvokeInterceptor = (PostInvokeInterceptor) interceptor;
				try {
					postInvokeInterceptor.postInvoke(managedMethod, args, returnValue);
				} catch (Exception e) {
					log.error("Exception on post-invoke interceptor for method |%s|: %s", managedMethod, e);
					throw new InvocationTargetException(e);
				}
			}
			return returnValue;
		}
	}

	/**
	 * Execute {@link Invoker} instance in a separated thread of execution. This invoker implementation is a decorator; it gets
	 * an externally created invoker and adds asynchronous execution.
	 * <p>
	 * Current implementation is based on {@link AsyncTask} and has no means to <code>join</code> after starting asynchronous
	 * tasks. If invoker executed asynchronously fails the only option to be notified is application logger.
	 * 
	 * @version final
	 */
	private final class AsyncInvoker implements Invoker {
		/** Reference to externally created invoker to be executed asynchronously. */
		private Invoker invoker;

		/**
		 * Create asynchronous invoker for given invoker instance.
		 * 
		 * @param invoker invoker instance to be executed asynchronously.
		 */
		public AsyncInvoker(Invoker invoker) {
			this.invoker = invoker;
		}

		/**
		 * Create and start a new asynchronous task to execute {@link #invoker}. This method returns immediately whit always
		 * null value and does not throw any exception. If underlying invoker execution fail for any reason the only option to
		 * be notified is application logger.
		 * 
		 * @param object instance on which method is invoked,
		 * @param args invocation arguments.
		 * @return always returns null.
		 */
		@Override
		public Object invoke(final Object object, final Object[] args) {
			AsyncTask<Void> asyncTask = new AsyncTask<Void>() {
				@Override
				protected Void execute() throws Throwable {
					AsyncInvoker.this.invoker.invoke(object, args);
					return null;
				}
			};
			asyncTask.start();
			return null;
		}
	}

	// --------------------------------------------------------------------------------------------
	// INVOCATION METER

	/**
	 * Invocation meters implementation for managed methods. Standard usage pattern is to update meter state on
	 * {@link ManagedMethod#invoke(Object, Object...)} execution, like in sample code below. Also, not present in pseudo-code,
	 * increment exceptions count if method logic fails.
	 * 
	 * <pre>
	 * class ManagedMethod {
	 * 	void invoke() {
	 * 		this.meter.incrementInvocationsCount();
	 * 		this.meter.startProcessing();
	 * 		// process method logic
	 * 		this.meter.stopProcessing();
	 * 	}
	 * }
	 * </pre>
	 * 
	 * Managed method meter class implements {@link InvocationMeter} interface for meter counters reading.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static class Meter implements InvocationMeter {
		/** Declaring class for instrumented managed method. */
		private Class<?> declaringClass;

		/** Instrumented method signature. */
		private String methodSignature;

		/** Method invocations count. Updated by {@link #incrementInvocationsCount()}. */
		private long invocationsCount;

		/** Method exceptions count. Updated by {@link #incrementExceptionsCount()}. */
		private long exceptionsCount;

		/** Total processing time. */
		private long totalProcessingTime;

		/** Maximum value of processing time. */
		private long maxProcessingTime;

		/** Timestamp for processing time recording start. */
		private long startProcessingTimestamp;

		/**
		 * Construct meter instance. Store declaring class and initialize method signature.
		 * 
		 * @param method instrumented method.
		 */
		Meter(Method method) {
			this.declaringClass = method.getDeclaringClass();

			List<String> formalParameters = new ArrayList<String>();
			for (Class<?> formalParameter : method.getParameterTypes()) {
				formalParameters.add(formalParameter.getSimpleName());
			}

			this.methodSignature = String.format(SIMPLE_NAME_FORMAT, method.getName(), Strings.join(formalParameters, ','));
		}

		@Override
		public Class<?> getMethodDeclaringClass() {
			return declaringClass;
		}

		@Override
		public String getMethodSignature() {
			return methodSignature;
		}

		@Override
		public long getInvocationsCount() {
			return invocationsCount;
		}

		@Override
		public long getExceptionsCount() {
			return exceptionsCount;
		}

		@Override
		public long getTotalProcessingTime() {
			return totalProcessingTime;
		}

		@Override
		public long getMaxProcessingTime() {
			return maxProcessingTime;
		}

		@Override
		public void reset() {
			invocationsCount = 0;
			exceptionsCount = 0;
			totalProcessingTime = 0;
			maxProcessingTime = 0;
		}

		@Override
		public String toExternalForm() {
			long totalProcessingTime = this.totalProcessingTime / 1000000;
			long averageProcessingTime = invocationsCount != 0 ? totalProcessingTime / invocationsCount : 0;

			StringBuilder sb = new StringBuilder();
			sb.append(declaringClass.getCanonicalName());
			sb.append("#");
			sb.append(methodSignature);
			sb.append(": ");
			sb.append(invocationsCount);
			sb.append(": ");
			sb.append(exceptionsCount);
			sb.append(": ");
			sb.append(totalProcessingTime);
			sb.append(": ");
			sb.append(maxProcessingTime / 1000000);
			sb.append(": ");
			sb.append(averageProcessingTime);
			return sb.toString();
		}

		/** Increment invocation count on every method invocation, including those failed. */
		private void incrementInvocationsCount() {
			++invocationsCount;
		}

		/** Increment exceptions count for every failed method invocation. */
		private void incrementExceptionsCount() {
			++exceptionsCount;
		}

		/**
		 * Start recording of processing time. It is called just before method execution. Update internal
		 * {@link #startProcessingTimestamp} to current system time.
		 */
		private void startProcessing() {
			startProcessingTimestamp = System.nanoTime();
		}

		/** Stop recording of processing time and update total and maximum processing time. */
		private void stopProcessing() {
			long processingTime = System.nanoTime() - startProcessingTimestamp;
			totalProcessingTime += processingTime;
			if (maxProcessingTime < processingTime) {
				maxProcessingTime = processingTime;
			}
		}
	}
}
