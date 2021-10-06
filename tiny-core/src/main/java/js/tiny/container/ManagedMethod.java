package js.tiny.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remote;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.SecurityContext;
import js.tiny.container.interceptor.Interceptor;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceMeta;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;
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
public final class ManagedMethod implements IManagedMethod, IMethodInvocationProcessor {
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
	 * A managed method is remotely accessible, also known as net method, if is annotated with {@link Remote} or declaring class
	 * is remotely accessible.
	 */
	private boolean remotelyAccessible;

	/**
	 * EJB3.1 17.3.2.2 - The Bean Provider or Application Assembler can indicate that all roles are permitted to execute one or
	 * more specified methods (i.e., the methods should not be “checked” for authorization prior to invocation by the
	 * container). The unchecked element is used instead of a role name in the method-permission element to indicate that all
	 * roles are permitted.
	 */
	private boolean unchecked;

	/** There is at least one metadata attribute related to security. */
	private boolean securityEnabled;

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

	private final Map<Class<? extends IContainerServiceMeta>, IContainerServiceMeta> serviceMetas = new HashMap<>();

	private final Set<IMethodInvocationProcessor> invocationProcessors = new HashSet<>();

	/**
	 * Roles allowed to invoke this managed method. If empty and if this method is private all authenticated users are
	 * authorized.
	 */
	private String[] roles = new String[0];

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
	public ManagedMethod(IManagedClass declaringClass, Method method) {
		this.container = declaringClass.getContainer();
		this.declaringClass = declaringClass;
		this.method = method;
		this.method.setAccessible(true);

		List<String> formalParameters = new ArrayList<String>();
		for (Class<?> formalParameter : method.getParameterTypes()) {
			formalParameters.add(formalParameter.getSimpleName());
		}
		signature = String.format(QUALIFIED_NAME_FORMAT, method.getDeclaringClass().getName(), method.getName(), Strings.join(formalParameters, ','));

		argumentsProcessor = new ArgumentsProcessor();

		declaringClass.getServices().forEach(service -> {
			if (service instanceof IMethodInvocationProcessor) {
				invocationProcessors.add((IMethodInvocationProcessor) service);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED METHOD INTERFACE

	@Override
	public IManagedClass getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public String getName() {
		return method.getName();
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
		MethodInvocationProcessorsChain processorsChain = new MethodInvocationProcessorsChain();
		processorsChain.addProcessors(invocationProcessors);
		// this managed method is a method invocation processor too
		// its priority ensures that it is executed at the end, after all other processors
		processorsChain.addProcessor(this);

		processorsChain.createIterator();
		IMethodInvocation methodInvocation = processorsChain.createMethodInvocation(this, object, args);
		return (T) processorsChain.invokeNextProcessor(methodInvocation);
	}

	@Override
	public Priority getPriority() {
		return Priority.NONE;
	}

	@Override
	public Object invoke(IMethodInvocationProcessorsChain unused, IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException {
		if (securityEnabled && !unchecked && !container.isAuthorized(roles)) {
			log.info("Reject not authenticated access to |%s|.", method);
			throw new AuthorizationException();
		}

		// arguments processor converts <args> to empty array if it is null
		// it can be null if on invocation chain there is Proxy invoked with no arguments
		Object[] arguments = argumentsProcessor.preProcessArguments(this, methodInvocation.arguments());

		if (methodInvocation.instance() instanceof Proxy) {
			// if object is a Java Proxy does not apply method services implemented by below block
			// instead directly invoke Java method on the Proxy instance
			// container will call again this method but with the real object instance, in which case executes the next logic
			try {
				return method.invoke(methodInvocation.instance(), arguments);
			} catch (InvocationTargetException e) {
				throw new InvocationException(e.getTargetException());
			} catch (IllegalAccessException e) {
				throw new BugError("Illegal access on method with accessibility set true.");
			}
		}

		if (meter == null) {
			try {
				return method.invoke(methodInvocation.instance(), arguments);
			} catch (InvocationTargetException e) {
				throw new InvocationException(e.getTargetException());
			} catch (IllegalAccessException e) {
				throw new BugError("Illegal access on method with accessibility set true.");
			}
		}

		meter.incrementInvocationsCount();
		meter.startProcessing();
		Object returnValue = null;
		try {
			returnValue = method.invoke(methodInvocation.instance(), arguments);
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

	/**
	 * Returns a string describing this managed method.
	 */
	@Override
	public String toString() {
		return signature;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IContainerServiceMeta> T getServiceMeta(Class<T> type) {
		return (T) serviceMetas.get(type);
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

	// --------------------------------------------------------------------------------------------
	// PACKAGE METHODS

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

	void addServiceMeta(IContainerService service, IContainerServiceMeta serviceMeta) {
		log.debug("Add service meta |%s| to managed method |%s|", serviceMeta.getClass(), this);
		serviceMetas.put(serviceMeta.getClass(), serviceMeta);
		if (service instanceof IMethodInvocationProcessor) {
			invocationProcessors.add((IMethodInvocationProcessor) service);
		}
	}

	void setRoles(String[] roles) {
		Params.notNullOrEmpty(roles, "Roles");
		this.unchecked = false;
		this.roles = roles;
	}

	void setSecurityEnabled(boolean securityEnabled) {
		this.securityEnabled = securityEnabled;
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
}
