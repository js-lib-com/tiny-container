package js.tiny.container;

import javax.interceptor.Interceptors;

/**
 * Invocation interceptor executed just before managed method invocation. Pre-invoke interceptor interface works closely with
 * {@link Interceptors} annotation, see below.
 * <p>
 * In sample code, managed method is intercepted by audit class. Container executes Audit#preInvoke just before invoking managed
 * method.
 * 
 * <pre>
 * class ManagedClass {
 * 	&#064;Intercepted(Audit.class)
 * 	Object managedMethod(Object... args) {
 * 	}
 * }
 * 
 * class Audit implements PreInvokeInterceptor {
 * 	void preInvoke(ManagedMethodSPI managedMethod, Object[] args) throws Exception {
 * 	}
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface PreInvokeInterceptor extends Interceptor {
	/**
	 * Invocation handler defined by application and executed by container. This hook is executed by container just before
	 * managed method invocation.
	 * 
	 * @param managedMethod managed method about to be invoked,
	 * @param args method actual call arguments.
	 * @throws Exception any exception thrown by hook logic.
	 */
	void preInvoke(ManagedMethodSPI managedMethod, Object[] args) throws Exception;
}
