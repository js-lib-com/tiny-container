package com.jslib.tiny.container.core;

import java.lang.reflect.Type;

import com.jslib.lang.IllegalArgumentException;
import com.jslib.lang.VarArgs;
import com.jslib.tiny.container.spi.IManagedMethod;
import com.jslib.util.Types;

/**
 * Validate method invocation arguments against method formal parameters count and types.
 * 
 * @author Iulian Rotaru
 */
class ArgumentsValidator {
	private static final Object[] EMPTY_ARGS = new Object[0];

	/**
	 * Validate managed method invocation arguments against method signature. Throws {@link IllegalArgumentException} if given
	 * invocation arguments count and types does not match managed method formal parameters.
	 * 
	 * If given invocation arguments array is null this method returns empty arguments array.
	 * 
	 * @param managedMethod managed method,
	 * @param arguments method invocation arguments, null accepted.
	 * @return validated arguments.
	 * @throws IllegalArgumentException if given invocation arguments are not valid.
	 */
	public Object[] validateArguments(IManagedMethod managedMethod, Object[] arguments) {
		if (arguments == null) {
			return EMPTY_ARGS;
		}
		final Type[] formalParameters = managedMethod.getParameterTypes();

		switch (arguments.length) {
		case 0:
			if (formalParameters.length != 0) {
				throw new IllegalArgumentException("Missing arguments for on method |%s|.", managedMethod);
			}
			break;

		case 1:
			if (arguments[0] instanceof VarArgs && formalParameters.length == 1 && Types.isArray(formalParameters[0])) {
				arguments[0] = ((VarArgs<?>) arguments[0]).getArguments();
			}
			break;
		}

		if (formalParameters.length != arguments.length) {
			throw new IllegalArgumentException("Invalid arguments count on method |%s|. Expected |%d| but got |%d|.", managedMethod, formalParameters.length, arguments.length);
		}
		for (int i = 0; i < formalParameters.length; ++i) {
			if (arguments[i] != null && !Types.isInstanceOf(arguments[i], formalParameters[i])) {
				throw new IllegalArgumentException("Invalid argument type at position |%d| on method |%s|. Expected |%s| but got |%s|.", i, managedMethod, formalParameters[i], arguments[i].getClass());
			}
		}
		return arguments;
	}
}
