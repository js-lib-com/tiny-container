package com.jslib.container.interceptor;

import com.jslib.container.spi.IManagedMethod;

import jakarta.interceptor.Interceptors;

/**
 * Invocation interceptor executed just after managed method invocation. Post-invoke interceptor interface works closely with
 * {@link Interceptors} annotation, see below.
 * <p>
 * In sample code, managed method is intercepted by audit class. Container executes Audit#postInvoke just after managed method
 * return.
 * 
 * <pre>
 * class ManagedClass {
 * 	&#064;Intercepted(Audit.class)
 * 	Object managedMethod(Object... arguments) {
 * 	}
 * }
 * 
 * class Audit implements PostInvokeInterceptor {
 * 	void postInvoke(ManagedMethodSPI managedMethod, Object[] arguments, Object returnValue) throws Exception {
 * 	}
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public interface PostInvokeInterceptor extends Interceptor {
	/**
	 * Invocation handler defined by application and executed by container. This hook is executed by container just after
	 * managed method invocation.
	 * 
	 * @param managedMethod managed method about to be invoked,
	 * @param arguments method actual call arguments.
	 * @param returnValue value returned by managed method after execution.
	 * @throws Exception any exception thrown by hook logic.
	 */
	void postInvoke(IManagedMethod managedMethod, Object[] arguments, Object returnValue) throws Exception;
}
