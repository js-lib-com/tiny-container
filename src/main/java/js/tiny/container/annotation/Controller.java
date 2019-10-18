package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.tiny.container.ManagedClassSPI;

/**
 * A controller is a net class that has methods returning resources. A controller can have a request URI path, retrievable by
 * {@link ManagedClassSPI#getRequestPath()}. This path is publicly known and is used by clients to identify the target net
 * class.
 * <p>
 * To access controller from below sample, request URL may be <code>http://server/app/admin/pages/index.xsp</code>, where
 * <code>admin/pages</code> is the controller request URI path. If this annotation is used without value argument, controller
 * has no request URI path and is deemed as <code>default</code>, in which case request URL becomes
 * <code>http://server/app/index.xsp</code>.
 * 
 * <pre>
 * &#064;Controller("admin/pages")
 * class Pages {
 * 	...
 * 	public View index() {
 * 		... 
 * 	} 
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
	/**
	 * Request URI path, optional, default to empty string.
	 * 
	 * @return request URI path, possible empty.
	 */
	String value() default "";
}
