package js.tiny.container.interceptor;

import jakarta.interceptor.Interceptors;

/**
 * Stateless managed method invocation interceptor. This interface is base abstraction for specialized interceptors executed
 * before and after managed method execution. User space code may chose one of them or even both. Interceptor interface works
 * closely with {@link Interceptors} annotation as in next example.
 * 
 * In sample below managed method is intercepted by audit class. Container executes {@link PreInvokeInterceptor} just before
 * executing managed method and {@link PostInvokeInterceptor} after.
 * 
 * <pre>
 * class ManagedClass {
 * 	&#064;Intercepted(Audit.class)
 * 	Object managedMethod(Object... args) {
 * 	}
 * }
 * 
 * class Audit implements PreInvokeInterceptor, PostInvokeInterceptor {
 * 	void preInvoke(ManagedMethod managedMethod, Object[] parameters) throws Exception {
 * 	}
 * 
 * 	void postInvoke(ManagedMethod managedMethod, Object[] parameters, Object returnValue) throws Exception {
 * 	}
 * }
 * </pre>
 * 
 * Warn: interceptor instance is reused by container for every method invocation; therefore implementation should refrain to
 * hold mutable state.
 * 
 * @author Iulian Rotaru
 */
public interface Interceptor {
}
