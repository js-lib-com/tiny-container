package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
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
public class ManagedMethod implements IManagedMethod {
	private static final Log log = LogFactory.getLog(IManagedMethod.class);

	/** Format string for managed method simple name, without class name. */
	private static final String SIMPLE_NAME_FORMAT = "%s(%s)";

	/** Format string for managed method fully qualified name. */
	private static final String QUALIFIED_NAME_FORMAT = "%s#" + SIMPLE_NAME_FORMAT;

	/** The managed class declaring this managed method. */
	private final IManagedClass<?> declaringClass;

	/** Wrapped Java reflective method. This method instance reflects the method declared by managed class implementation. */
	private final Method method;

	/** Managed method signature, mainly for debugging. */
	private final String signature;

	private final ArgumentsValidator argumentsValidator = new ArgumentsValidator();

	private final Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

	/**
	 * Join point processors attached to {@link #invoke(Object, Object...)} method. When this method is executed all processors
	 * hold by this join point are executed followed by {@link #onMethodInvocation(IInvocationProcessorsChain, IInvocation)}
	 * that does the actual method invocation.
	 */
	private final FlowProcessorsSet<IMethodInvocationProcessor> invocationProcessors = new FlowProcessorsSet<>();

	/**
	 * Construct a managed method. Store declaring class and Java reflective method, initialize this managed method signature
	 * and arguments processor.
	 * 
	 * @param declaringClass declaring managed class,
	 * @param method Java reflective method wrapped by this managed method.
	 */
	public ManagedMethod(IManagedClass<?> declaringClass, Method method) {
		this.declaringClass = declaringClass;
		this.method = method;
		this.method.setAccessible(true);

		List<String> formalParameters = new ArrayList<String>();
		for (Class<?> formalParameter : method.getParameterTypes()) {
			formalParameters.add(formalParameter.getSimpleName());
		}
		signature = String.format(QUALIFIED_NAME_FORMAT, declaringClass.getImplementationClass().getCanonicalName(), method.getName(), Strings.join(formalParameters, ','));
	}

	@Override
	public void scanServices(Collection<IContainerService> services) {
		services.forEach(service -> {
			// current implementation consider only method invocation processors
			if (service instanceof IMethodInvocationProcessor) {
				IMethodInvocationProcessor processor = (IMethodInvocationProcessor) service;
				if (processor.bind(this)) {
					invocationProcessors.add(processor);
				}
			}
		});
	}

	@Override
	public IManagedClass<?> getDeclaringClass() {
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

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(method.getModifiers());
	}

	/**
	 * Join point where application logic execution cross-cuts container services related to method invocation. When an
	 * application method should be executed container routes request to this join point. Here invocation processors chain is
	 * created and executed; this way all processors from {@link #invocationProcessors} are executed before the actual
	 * application method execution, via {@link #onMethodInvocation(IInvocationProcessorsChain, IInvocation)}.
	 * 
	 * @param instance managed instance against which method is executed,
	 * @param arguments optional managed method invocation arguments.
	 * @param <T> returned value type.
	 * @return value returned by method or null for void.
	 * @throws Exception any exception from method or container service execution is bubbled up.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(Object instance, Object... arguments) throws Exception {
		InvocationProcessorsChain processorsChain = new InvocationProcessorsChain(invocationProcessors, this);
		return (T) processorsChain.invokeNextProcessor(processorsChain.createInvocation(this, instance, arguments));
	}

	@Override
	public Priority getPriority() {
		return Priority.METHOD;
	}

	/**
	 * Managed method implements {@link IMethodInvocationProcessor} interface so that it can be part of invocation processors
	 * chain, created and executed by {@link #invoke(Object, Object...)}. This managed method also inherits default priority
	 * value - see {@link #getPriority()}; its value guarantees that managed method is executed last, after all container
	 * services were executed.
	 * 
	 * This method gets invocation processors chain parameter, mandated by interface signature. Anyway, it is not unused; this
	 * method does not call {@link IInvocationProcessorsChain#invokeNextProcessor(IInvocation)}, and as a consequence processing
	 * chain is ended.
	 * 
	 * @param chain invocation processor chain, unused.
	 * @param invocation invocation object.
	 */
	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		Object[] arguments = argumentsValidator.validateArguments(this, invocation.arguments());
		return method.invoke(invocation.instance(), arguments);
	}

	@Override
	public <T extends Annotation> T scanAnnotation(Class<T> annotationClass) {
		T annotation = method.getAnnotation(annotationClass);
		if (annotation == null) {
			final String name = method.getName();
			final Class<?>[] parameterTypes = method.getParameterTypes();
			if (annotation == null) {
				for (Class<?> interfaceClass : method.getDeclaringClass().getInterfaces()) {
					try {
						annotation = interfaceClass.getMethod(name, parameterTypes).getAnnotation(annotationClass);
						if (annotation != null) {
							break;
						}
					} catch (NoSuchMethodException unused) {
					}
				}
			}
		}
		return annotation;
	}

	@Override
	public void addAnnotation(Annotation serviceMeta) {
		log.debug("Add service meta |%s| to managed method |%s|", serviceMeta.getClass(), this);
		annotations.put(serviceMeta.annotationType(), serviceMeta);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Annotation> T getAnnotation(Class<T> type) {
		return (T) annotations.get(type);
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
			return null;
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

	@Override
	public String toString() {
		return signature;
	}

	@Override
	public int hashCode() {
		return Objects.hash(method);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ManagedMethod other = (ManagedMethod) obj;
		return Objects.equals(method, other.method);
	}

	// --------------------------------------------------------------------------------------------
	// PACKAGE METHODS

	void addInvocationProcessor(IMethodInvocationProcessor invocationProcessor) {
		invocationProcessors.add(invocationProcessor);
	}
}
