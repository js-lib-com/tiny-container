package js.tiny.container;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Schedule;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import js.lang.BugError;
import js.lang.InvocationException;
import js.tiny.container.core.SecurityContext;
import js.transaction.Immutable;
import js.transaction.Mutable;
import js.transaction.Transactional;
import js.util.Strings;

/**
 * Managed method service provider interface. Although public, this interface is designed for library internal usage. User space
 * code should consider this interface as volatile and subject to change without notice.
 * <p>
 * This interface exposes strictly needed functionality from managed method implementation.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ManagedMethodSPI {
	/**
	 * Get managed class that declares this managed method.
	 * 
	 * @return parent managed class.
	 */
	ManagedClassSPI getDeclaringClass();

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
	 * Get content type used when serialize method return value to HTTP response. This value is initialized from
	 * {@link Produces} annotation. Return null if annotation is not declared.
	 * 
	 * @return content type used for returned value serialization.
	 */
	String getReturnContentType();

	/**
	 * Invoke managed method. Just delegate wrapped Java reflective method but takes care to execute {@link Interceptor}, if any
	 * configured, and to update internal {@link InvocationMeter}.
	 * 
	 * @param object managed instance against which method is executed,
	 * @param args invocation arguments.
	 * @param <T> returned value type.
	 * @return value returned by method or null for void.
	 * @throws AuthorizationException if method is private and {@link SecurityContext} is not authenticated.
	 * @throws IllegalArgumentException if invocation arguments does not match method signature.
	 * @throws InvocationException if method execution fails for whatever reason.
	 */
	<T> T invoke(Object object, Object... args) throws AuthorizationException, IllegalArgumentException, InvocationException;

	/**
	 * Get request URI path of this net method, that is, the path component by which net method is referred into request URI. A
	 * net method is a managed method marked as remote accessible via {@link Remote} annotation. A net method can have a name by
	 * which is publicly known, set via {@link Path} annotation.
	 * <p>
	 * A net method is not mandatory to have {@link Path} annotation. If annotation is missing implementation should use this
	 * method name converted to dash case, see {@link Strings#toDashCase(String)}.
	 * <p>
	 * Attempting to retrieve request URI path for a local managed method is considered a bug.
	 * 
	 * @return request URI path of this net method, never null.
	 * @throws BugError if attempt to use this getter on a local managed method.
	 */
	String getRequestPath();

	/**
	 * Test if this managed method return type is void.
	 * 
	 * @return true if this managed method is void.
	 */
	boolean isVoid();

	/**
	 * Test if this managed method can be accessed remotely, that is, is a net method. A managed method is accessible remote if
	 * it has {@link Remote} annotation or if its declaring class is remote.
	 * <p>
	 * A remotely accessible managed method is known as <code>net method</code>.
	 * 
	 * @return true if this managed method can be accessed remotely.
	 */
	boolean isRemotelyAccessible();

	/**
	 * Test if this managed method is unchecked, that is, can be accessed remotely without authorization. This predicate value
	 * has meaning only if this managed method is remote, as defined by {@link #isRemotelyAccessible()}.
	 * <p>
	 * Usually, a managed method is private; it can be accessed remotely only inside an authorized security context. Anyway, it
	 * becomes public if tagged with {@link PermitAll} annotation - this public state is known as security unchecked.
	 * <p>
	 * <b>Warning:</b> a public remote managed method can be accessed without authorization.
	 * <p>
	 * EJB3.1 17.3.2.2 - The Bean Provider or Application Assembler can indicate that all roles are permitted to execute one or
	 * more specified methods (i.e., the methods should not be “checked” for authorization prior to invocation by the
	 * container). The unchecked element is used instead of a role name in the method-permission element to indicate that all
	 * roles are permitted.
	 * 
	 * @return true if this managed method is unchecked, that is, can be accessed remotely without authorization.
	 */
	boolean isUnchecked();

	/**
	 * Test if this managed method should be executed into a transactional context. A managed method is transactional if it has
	 * {@link Transactional} annotation or if its declaring class is transactional.
	 * 
	 * @return true if this managed method is transactional.
	 */
	boolean isTransactional();

	/**
	 * Test if this transactional managed method is immutable. This predicate has meaning only if {@link #isTransactional()} is
	 * true. A managed method is immutable it is tagged so using {@link Immutable} annotation. Also is immutable if its
	 * declaring class is immutable and has no {@link Mutable} annotation.
	 * 
	 * @return true if this transactional managed method is immutable.
	 */
	boolean isImmutable();

	/**
	 * Test if this managed method is asynchronous. A managed method is asynchronous if is tagged so using {@link Asynchronous}
	 * annotation. An asynchronous managed method is executed in a separated execution thread. It is considered a flaw in logic
	 * if an asynchronous managed method has a return type, i.e. different from void. Implementation should rise
	 * {@link BugError} if encounter such condition.
	 * 
	 * @return true if this managed method is asynchronous.
	 */
	boolean isAsynchronous();

	Schedule getSchedule();
}