package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Managed method service provider interface. Although public, this interface is designed for library internal usage. User space
 * code should consider this interface as volatile and subject to change without notice.
 * <p>
 * This interface exposes strictly needed functionality from managed method implementation.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface IManagedMethod extends IMethodInvocationProcessor {

	String getName();

	String getSignature();

	/**
	 * Get managed class that declares this managed method.
	 * 
	 * @return parent managed class.
	 */
	IManagedClass<?> getDeclaringClass();

	/**
	 * Get wrapped Java method.
	 * 
	 * @return wrapped Java method.
	 */
	Method getMethod();

	/**
	 * Get managed method parameter types. If a formal parameter type is a parameterized type, the Type object returned for it
	 * must accurately reflect the actual type parameters used in the source code.
	 * 
	 * @return this managed method parameter types.
	 */
	Type[] getParameterTypes();

	/**
	 * Get this managed method return type.
	 * 
	 * @return this managed class return type.
	 */
	Type getReturnType();

	/**
	 * Invoke managed method taking care to execute container services, if any .
	 * 
	 * @param object managed instance against which method is executed,
	 * @param args invocation arguments.
	 * @param <T> returned value type.
	 * @return value returned by method or null for void.
	 * @throws Exception any exception from method or container service execution is bubbled up.
	 */
	<T> T invoke(Object object, Object... args) throws Exception;

	<T extends Annotation> T scanAnnotation(Class<T> type);

	void addAnnotation(Annotation annotation);

	<T extends Annotation> T getAnnotation(Class<T> type);

	void setAttribute(Object context, String name, Object value);

	<T> T getAttribute(Object context, String name, Class<T> type);

}