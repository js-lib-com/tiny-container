package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.tiny.container.InstanceType;
import js.tiny.container.Interceptor;

/**
 * An intercepted managed method executes an interceptor cross-cutting logic whenever is invoked. Works in conjunction with
 * {@link Interceptor} interface to facilitate a basic aspect programming. {@literal @}Intercepted is used to annotate specific
 * public method or if this annotation is used on a class, all class public methods. Using this annotation on private or
 * protected methods is silently ignored. Also {@literal @}Intercepted should be used on managed class of
 * {@link InstanceType#PROXY} type or {@link InstanceType#POJO} annotated with {@link Remote}, {@link Controller} or
 * {@link Service}; otherwise is considered a bug.
 * <p>
 * Interceptor is executed every time an intercepted managed method is invoked.
 * <p>
 * This annotation value is the interceptor class. Interceptor should be instantiable; it can be both managed class or plain
 * Java object. In any case interceptor should implement {@link Interceptor} or one of its specialized interface. The benefit of
 * using managed class as interceptor is managed life span and the possibility to reuse interceptor instance; for plain Java
 * object, interceptor instance is created for every intercepted method invocation.
 * 
 * <pre>
 * class ManagedClass implements ManagedInterface {
 * 	...
 * 	&#064;Intercepted(AuditInterceptor.class)
 * 	public String getName() {
 * 	}
 * }
 * </pre>
 * 
 * In above snippet audit interceptor is executed every time name getter is called. Depending on which interceptor interface is
 * implementing, audit is executed before, after or around method invocation. See {@link Interceptor} for details.
 * <p>
 * If {@literal @}Intercepted annotation is used on a class all its public methods are intercepted.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Intercepted {
	/**
	 * Get interceptor class. If interceptor is a managed class returned value is the managed class interface, not
	 * implementation. If interceptor is plain Java object returned value is interceptor implementation. In any case managed
	 * method knows to instantiate and execute interceptor. Anyway, if interceptor is managed class there is the benefit of
	 * interceptor instance reuse, of course if managed class scope says so. For plain Java object interceptor a new instance is
	 * created for every intercepted method invocation.
	 * 
	 * @return interceptor class.
	 */
	Class<? extends Interceptor> value();
}
