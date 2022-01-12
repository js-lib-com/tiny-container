package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Managed method deals, beside the actual application method invocation, with container services execution. It is the central
 * join point where container services cross-cut business services.
 * 
 * It is a thin wrapper for Java method and keeps a list of invocation processors ordered by processor priority. When
 * {@link #invoke(Object, Object...)} managed method first executes registered invocation processors in sequence, and only after
 * that delegates wrapped Java method. Managed method is also responsible for container services and annotations scanning.
 *
 * Implementation should ensure that wrapped Java method is from implementation class not from interface. Also implementation
 * should properly implements equals and hash code; recommended solution is to delegate wrapped Java method.
 * 
 * @author Iulian Rotaru
 */
public interface IManagedMethod {

	/**
	 * Gets method simple name, that is, not qualified name.
	 * 
	 * @return method name.
	 */
	String getName();

	/**
	 * Gets method signature including declaring class and parameters.
	 * 
	 * @return method signature.
	 */
	String getSignature();

	/**
	 * Gets managed class that declares this managed method.
	 * 
	 * @return parent managed class.
	 */
	IManagedClass<?> getDeclaringClass();

	/**
	 * Test if this method is public.
	 * 
	 * @return true if this method is public.
	 */
	boolean isPublic();

	/**
	 * Test if this method is static.
	 * 
	 * @return true if this method is static.
	 */
	boolean isStatic();

	/**
	 * Test if this method has no return value.
	 * 
	 * @return true if this method is void.
	 */
	boolean isVoid();

	/**
	 * Gets managed method parameter types. If a formal parameter type is a parameterized type, the {@link Type} object returned
	 * for it must accurately reflect the actual type parameters used in the source code.
	 * 
	 * @return this managed method parameter types.
	 */
	Type[] getParameterTypes();

	Type[] getExceptionTypes();

	/**
	 * Gets this managed method return type. If return type is a parameterized type, the returned {@link Type} object must
	 * accurately reflect the actual type parameter used in the source code.
	 * 
	 * @return this managed class return type.
	 */
	Type getReturnType();

	/**
	 * Invoke managed method taking care to execute container services, if any. First execute services then delegate wrapped
	 * Java method. Any exception from method or container services execution is propagated to caller.
	 * 
	 * @param instance managed instance against which method is executed,
	 * @param arguments invocation arguments.
	 * @param <T> returned value type.
	 * @return value returned by method or null for void.
	 * @throws Exception any exception from method or container service execution is bubbled up.
	 */
	<T> T invoke(Object instance, Object... arguments) throws Exception;

	/**
	 * Scans this method for requested annotation in both implementation and interface classes, in this order. Interface should
	 * be that declared by parent managed class - see {@link IManagedClass#getInterfaceClass()}, not detected from Java method
	 * declaring class.
	 * 
	 * @param annotationClass annotation class to search for.
	 * @return method annotation instance or null if not found.
	 * @param <T> generic annotation type.
	 */
	<A extends Annotation> A scanAnnotation(Class<A> annotationClass, Flags... flags);

	<T> T scanAnnotations(Function<Annotation, T> predicate);

	enum Flags {
		INCLUDE_TYPES
	}

}
