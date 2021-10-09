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
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;
import js.tiny.container.spi.IServiceMeta;
import js.util.Params;
import js.util.Strings;
import js.util.Types;

/**
 * A managed method, aka business or application method, is a thin wrapper around Java reflective method. It has methods to
 * retrieve internal state and miscellaneous flags but the real added piece of functionality is
 * {@link #invoke(Object, Object...)} method that takes care to execute container services before delegating Java reflective
 * method.
 * 
 * @author Iulian Rotaru
 */
public final class ManagedMethod implements IManagedMethod, IMethodInvocationProcessor {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(IManagedMethod.class);

	/** Format string for managed method simple name, without class name. */
	static final String SIMPLE_NAME_FORMAT = "%s(%s)";

	/** Format string for managed method fully qualified name. */
	private static final String QUALIFIED_NAME_FORMAT = "%s#" + SIMPLE_NAME_FORMAT;

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

	/** Managed method signature, mainly for debugging. */
	private final String signature;

	/** Method invocation arguments processor. */
	private final ArgumentsProcessor argumentsProcessor;

	private final Map<Class<? extends IServiceMeta>, IServiceMeta> serviceMetas = new HashMap<>();

	private final Set<IMethodInvocationProcessor> invocationProcessors = new HashSet<>();

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
	public String getSignature() {
		return signature;
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
		// arguments processor converts <args> to empty array if it is null
		// it can be null if on invocation chain there is Proxy invoked with no arguments
		Object[] arguments = argumentsProcessor.preProcessArguments(this, methodInvocation.arguments());

		// TODO: deprecated ?
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

		try {
			return method.invoke(methodInvocation.instance(), arguments);
		} catch (InvocationTargetException e) {
			throw new InvocationException(e.getTargetException());
		} catch (IllegalAccessException e) {
			throw new BugError("Illegal access on method with accessibility set true.");
		}
	}

	@Override
	public boolean isVoid() {
		return Types.isVoid(method.getReturnType());
	}

	@Override
	public boolean isRemotelyAccessible() {
		return remotelyAccessible;
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
	public <T extends IServiceMeta> T getServiceMeta(Class<T> type) {
		return (T) serviceMetas.get(type);
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

	private final Map<String, Object> attributes = new HashMap<>();

	@Override
	public void setAttribute(Object context, String name, Object value) {
		attributes.put(key(context, name), value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAttribute(Object context, String name, Class<T> type) {
		String key = key(context, name);
		Object value = attributes.get(key);
		if (value == null) {
			throw new IllegalStateException(String.format("Cannot found managed method attribute |%s|.", key));
		}
		if (!Types.isInstanceOf(value, type)) {
			throw new ClassCastException(String.format("Cannot cast attribute |%s| to type |%s|.", key, type));
		}
		return (T) value;
	}

	private static final String key(Object context, String name) {
		if (!(context instanceof Class)) {
			context = context.getClass();
		}
		return Strings.concat(((Class<?>) context).getCanonicalName(), '#', name);
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

	void addInvocationProcessor(IMethodInvocationProcessor invocationProcessor) {
		invocationProcessors.add(invocationProcessor);
	}
	
	void addServiceMeta(IServiceMeta serviceMeta) {
		log.debug("Add service meta |%s| to managed method |%s|", serviceMeta.getClass(), this);
		serviceMetas.put(serviceMeta.getClass(), serviceMeta);
	}

	void setRoles(String[] roles) {
		Params.notNullOrEmpty(roles, "Roles");
	}
}
