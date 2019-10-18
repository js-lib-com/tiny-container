package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.tiny.container.ManagedClassSPI;

/**
 * A service is a managed class that has methods remotely accessible. A service can have a request URI path, retrievable by
 * {@link ManagedClassSPI#getRequestPath()}. This path is publicly known and is used by clients to identify the target
 * service.
 * <p>
 * To access service from below sample, request URL may be <code>http://server/app/admin/services/user</code>, where
 * <code>admin/services</code> is the request URI path. If this annotation is used without value argument, service has no
 * request URI path and is deemed as <code>default</code>, in which case request URL becomes <code>http://server/app/user</code>.
 * 
 * <pre>
 * &#064;Service("admin/services")
 * class Service {
 * 	...
 * 	&#064;MethodPath("user")
 * 	public User getUserById(int userId) {
 * 	} 
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
	/**
	 * Request URI path, optional, default to empty string.
	 * 
	 * @return request URI path, possible empty.
	 */
	String value() default "";
}
