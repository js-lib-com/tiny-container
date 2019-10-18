package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.util.Strings;

/**
 * Set request URI path to managed classes and methods. Request URI path is the public name a type or a method is known for
 * remote access. This annotation value is stored at managed class creation and retrievable with
 * {@link ManagedClassSPI#getRequestPath()}, respective {@link ManagedMethodSPI#getRequestPath()}.
 * <p>
 * To access service method from below sample, request URL may be <code>http://server.com/app/admin/service/user</code>, where
 * <code>admin/service</code> and <code>user</code> are the class, respective method request URI path set by this annotation
 * value.
 * 
 * <pre>
 * &#064;Remote
 * &#064;RequestPath(("admin/service")
 * class Service {
 * 	...
 * 	&#064;RemotePath("user")
 * 	public User getUserById(int userId) {
 * 	} 
 * }
 * </pre>
 * <p>
 * There are shorthand annotations for {@link Service} and {@link Controller} that combine {@link Remote} and this request path
 * annotations.
 * 
 * <pre>
 * &#064;Service("admin/service")
 * class Service {
 * 	...
 * }
 * </pre>
 * 
 * If &#064;RequestPath annotation is not present on a net method, container should use method name converted to dash case, see
 * {@link Strings#toDashCase(String)}. In our example URL becomes
 * <code>http://server.com/app/admin/service/get-user-by-id</code>
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPath {
	/**
	 * The value of request URI path for annotated method.
	 * 
	 * @return method request URI path.
	 */
	String value();
}
