package com.jslib.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainerService;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IManagedParameter;
import com.jslib.container.spi.IMethodInvocationProcessor;
import com.jslib.util.Strings;
import com.jslib.util.Types;

/**
 * A managed method, aka business or application method, is a thin wrapper around Java reflective method. It has methods to
 * retrieve internal state and miscellaneous flags but the real added piece of functionality is
 * {@link #invoke(Object, Object...)} method that takes care to execute container services before delegating Java reflective
 * method.
 * 
 * @author Iulian Rotaru
 */
class ManagedMethod implements IManagedMethod, IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(ManagedMethod.class);

	/** Format string for managed method simple name, without class name. */
	private static final String SIMPLE_NAME_FORMAT = "%s(%s)";

	/** Format string for managed method fully qualified name. */
	private static final String QUALIFIED_NAME_FORMAT = "%s#" + SIMPLE_NAME_FORMAT;

	/** The managed class declaring this managed method. */
	private final IManagedClass<?> declaringClass;

	/** Wrapped Java reflective method. This method instance reflects the method declared by managed class interface. */
	private final Method interfaceMethod;

	/** Implementation method is used for annotation scanning and is not null only if declaring class has interface class. */
	private final Method implementationMethod;

	/** Managed method signature, mainly for debugging. */
	private final String signature;

	private final ArgumentsValidator argumentsValidator = new ArgumentsValidator();

	/**
	 * Join point processors attached to {@link #invoke(Object, Object...)} method. When this method is executed all processors
	 * hold by this join point are executed followed by {@link #onMethodInvocation(IInvocationProcessorsChain, IInvocation)}
	 * that does the actual method invocation.
	 */
	private final FlowProcessorsSet<IMethodInvocationProcessor> invocationProcessors = new FlowProcessorsSet<>();

	public ManagedMethod(IManagedClass<?> declaringClass, Method interfaceMethod) {
		this.declaringClass = declaringClass;
		this.interfaceMethod = interfaceMethod;
		this.interfaceMethod.setAccessible(true);

		Method implementationMethod = null;
		if (!declaringClass.getInterfaceClass().equals(declaringClass.getImplementationClass())) {
			final String name = interfaceMethod.getName();
			final Class<?>[] parameterTypes = interfaceMethod.getParameterTypes();
			try {
				implementationMethod = declaringClass.getImplementationClass().getMethod(name, parameterTypes);
			} catch (NoSuchMethodException | SecurityException e) {
				log.error(e);
			}
		}
		this.implementationMethod = implementationMethod;

		List<String> formalParameters = new ArrayList<String>();
		for (Class<?> formalParameter : interfaceMethod.getParameterTypes()) {
			formalParameters.add(formalParameter.getSimpleName());
		}
		signature = String.format(QUALIFIED_NAME_FORMAT, declaringClass.getImplementationClass().getCanonicalName(), interfaceMethod.getName(), Strings.join(formalParameters, ','));
	}

	/**
	 * Scan container services and register discovered invocation processors. Only invocation processors that bind successfully
	 * to this managed method are added to {@link #invocationProcessors} list.
	 * 
	 * @param services container services.
	 */
	public boolean scanServices(Iterable<IContainerService> services) {
		class ServicesFound {
			boolean value;
		}
		final ServicesFound servicesFound = new ServicesFound();

		services.forEach(service -> {
			// current implementation consider only method invocation processors
			if (service instanceof IMethodInvocationProcessor) {
				IMethodInvocationProcessor processor = (IMethodInvocationProcessor) service;
				if (processor.bind(this)) {
					servicesFound.value = true;
					invocationProcessors.add(processor);
				}
			}
		});

		return servicesFound.value;
	}

	@Override
	public IManagedClass<?> getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public String getName() {
		return interfaceMethod.getName();
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public Type[] getParameterTypes() {
		// there are rumors that generic type information is not preserved in byte code but not confirmed
		// this library does believe documentation and relies on next excerpt from Method.getGenericParameterTypes API:

		// If a formal parameter type is a parameterized type, the Type object returned for it must accurately reflect the
		// actual type parameters used in the source code.

		return interfaceMethod.getGenericParameterTypes();
	}

	@Override
	public List<IManagedParameter> getManagedParameters() {
		List<IManagedParameter> managedParameters = new ArrayList<>();
		if (implementationMethod == null) {
			for (Parameter interfaceParameter : interfaceMethod.getParameters()) {
				managedParameters.add(new ManagedParameter(interfaceParameter, null));
			}
		} else {
			final Parameter[] interfaceParameters = interfaceMethod.getParameters();
			final Parameter[] implementationParameters = implementationMethod.getParameters();
			assert interfaceParameters.length == implementationParameters.length;
			for (int i = 0; i < interfaceParameters.length; ++i) {
				managedParameters.add(new ManagedParameter(interfaceParameters[i], implementationParameters[i]));
			}
		}
		return managedParameters;
	}

	@Override
	public Type[] getExceptionTypes() {
		return interfaceMethod.getGenericExceptionTypes();
	}

	@Override
	public Type getReturnType() {
		return interfaceMethod.getGenericReturnType();
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(interfaceMethod.getModifiers());
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(interfaceMethod.getModifiers());
	}

	@Override
	public boolean isFinal() {
		return Modifier.isFinal(interfaceMethod.getModifiers());
	}

	@Override
	public boolean isVoid() {
		return Types.isVoid(interfaceMethod.getReturnType());
	}

	@Override
	public Method getMethod() {
		return implementationMethod != null ? implementationMethod : interfaceMethod;
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
	 * @throws Throwable any method execution exception is bubbled up.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(Object instance, Object... arguments) throws Throwable {
		InvocationProcessorsChain processorsChain = new InvocationProcessorsChain(invocationProcessors, this);
		return (T) processorsChain.invokeNextProcessor(processorsChain.createInvocation(this, instance, arguments));
	}

	@Override
	public Priority getPriority() {
		return Priority.METHOD;
	}

	/**
	 * Managed method implements {@link IMethodInvocationProcessor} interface so that it can be part of invocation processors
	 * chain, created and executed by {@link #invoke(Object, Object...)}. This managed method also define priority value - see
	 * {@link #getPriority()}; its value guarantees that managed method is executed last, after all container services were
	 * executed.
	 * 
	 * This method gets invocation processors chain parameter, mandated by interface signature. Anyway, it is not unused; this
	 * method does not call {@link IInvocationProcessorsChain#invokeNextProcessor(IInvocation)}, and as a consequence processing
	 * chain is ended.
	 * 
	 * This method does the actual method invocation using Java reflection. Invocation target exception is caught and replaced
	 * with the actual method execution exception, if any.
	 * 
	 * @param chain invocation processor chain, unused.
	 * @param invocation invocation object.
	 * @return value returned by method or null for void.
	 * @throws Throwable any method execution exception is bubbled up.
	 */
	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Throwable {
		Object[] arguments = argumentsValidator.validateArguments(this, invocation.arguments());
		try {
			return interfaceMethod.invoke(invocation.instance(), arguments);
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t == null) {
				t = e.getCause();
			}
			throw t != null ? t : e;
		}
	}

	@Override
	public <T extends Annotation> T scanAnnotation(Class<T> annotationClass, Flags... flags) {
		T annotation = interfaceMethod.getAnnotation(annotationClass);
		if (annotation == null && implementationMethod != null) {
			annotation = implementationMethod.getAnnotation(annotationClass);
		}

		// if desired annotation has target ElementType.TYPE give a try on method declaring class
		if (annotation == null && flags.length == 1 && flags[0] == Flags.INCLUDE_TYPES) {
			annotation = declaringClass.scanAnnotation(annotationClass);
		}
		return annotation;
	}

	@Override
	public <T> T scanAnnotations(Function<Annotation, T> function) {
		for (Annotation annotation : interfaceMethod.getAnnotations()) {
			T t = function.apply(annotation);
			if (t != null) {
				return t;
			}
		}

		if (implementationMethod != null) {
			for (Annotation annotation : implementationMethod.getAnnotations()) {
				T t = function.apply(annotation);
				if (t != null) {
					return t;
				}
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return signature;
	}

	@Override
	public int hashCode() {
		return Objects.hash(interfaceMethod);
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
		return Objects.equals(interfaceMethod, other.interfaceMethod);
	}
}
