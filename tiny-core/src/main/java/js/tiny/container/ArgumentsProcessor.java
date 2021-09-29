package js.tiny.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import js.lang.IllegalArgumentException;
import js.lang.VarArgs;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Types;

/**
 * Pre-process constructor or method invocation arguments. This processor is used when new managed instance is created or a
 * managed method is about to be invoked. It injects dependencies if formal parameters are required but no invocation arguments
 * provided. Also performs arguments validity check against formal parameters throwing illegal arguments if validation fails.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class ArgumentsProcessor extends DependencyProcessor {
	/** Reusable empty arguments used when provided arguments array is null. */
	private static final Object[] EMPTY_ARGS = new Object[0];

	/**
	 * Pre-process constructor arguments for local managed classes. A managed class is <code>local</code> if is of
	 * {@link InstanceType#POJO} or {@link InstanceType#PROXY} type. Attempting to pre-process arguments for other managed class
	 * types is silently ignored. This method delegates
	 * {@link #preProcessArguments(IManagedClass, Member, Class[], Object...)}.
	 * 
	 * @param managedClass managed class,
	 * @param args constructor arguments.
	 * @return processed arguments.
	 */
	public Object[] preProcessArguments(IManagedClass managedClass, Object... args) {
		// arguments can be null if on invocations chain there is Proxy handler invoked with no arguments
		if (args == null) {
			args = EMPTY_ARGS;
		}
		if (managedClass.getImplementationClass() == null) {
			return args;
		}
		Constructor<?> constructor = managedClass.getConstructor();

		// managed class constructor parameters have the same limitation as injected fields: they must be a managed
		// class on their turn; it is considered a bug trying to use not managed classes as constructor argument

		// because managed class is not generic is safe to use getParameterTypes instead of getGenericParameterTypes
		final Class<?>[] types = constructor.getParameterTypes();
		return preProcessArguments(managedClass, constructor, types, args);
	}

	/**
	 * Pre-process managed method invocation arguments. This processor prepares invocation arguments for given managed method;
	 * it just delegates {@link #preProcessArguments(IManagedClass, Member, Class[], Object...)}.
	 * 
	 * @param managedMethod managed method,
	 * @param args method invocation arguments.
	 * @return processed arguments.
	 */
	public Object[] preProcessArguments(IManagedMethod managedMethod, Object... args) {
		// arguments can be null if on invocations chain there is Proxy handler invoked with no arguments
		if (args == null) {
			args = EMPTY_ARGS;
		}

		final IManagedClass managedClass = managedMethod.getDeclaringClass();
		final Method method = managedMethod.getMethod();
		final Class<?>[] types = method.getParameterTypes();
		return preProcessArguments(managedClass, method, types, args);
	}

	/**
	 * Update and validate invocation arguments against given formal parameter types. If formal parameters is not empty but no
	 * invocation arguments this method will inject dependency using {@link #getDependencyValue(IManagedClass, Class)}. This
	 * method also performs arguments validity check against formal parameters throwing illegal arguments if validation fails.
	 * 
	 * @param managedClass managed class owning constructor or method,
	 * @param member constructor or method for given arguments,
	 * @param formalParameters formal parameter types,
	 * @param args constructor or method invocation arguments.
	 * @return given arguments updated and validated.
	 */
	private static Object[] preProcessArguments(IManagedClass managedClass, Member member, Class<?>[] formalParameters, Object... args) {
		switch (args.length) {
		case 0:
			args = new Object[formalParameters.length];
			for (int i = 0; i < args.length; i++) {
				args[i] = getDependencyValue(managedClass, formalParameters[i]);
			}
			break;

		case 1:
			// TODO: refine variable arguments: test for formal parameters type, document and test
			if (args[0] instanceof VarArgs && formalParameters.length == 1 && formalParameters[0].isArray()) {
				args[0] = ((VarArgs<?>) args[0]).getArguments();
			}
			break;
		}

		// arguments validity test against formal parameters: count and types
		if (formalParameters.length != args.length) {
			throw new IllegalArgumentException("Invalid arguments count on method |%s|. Expected |%d| but got |%d|.", member, formalParameters.length, args.length);
		}
		for (int i = 0; i < formalParameters.length; ++i) {
			if (args[i] != null && !Types.isInstanceOf(args[i], formalParameters[i])) {
				throw new IllegalArgumentException("Invalid argument type at position |%d| on method |%s|. Expected |%s| but got |%s|.", i, member, formalParameters[i], args[i].getClass());
			}
		}
		return args;
	}
}
