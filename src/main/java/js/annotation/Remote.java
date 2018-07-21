package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Grant remote access from client logic to managed classes and methods deployed in container. If applied to class or interface
 * all owned methods become <b>remote accessible</b>, unless individual method is tagged as {@link Local}.
 * <p>
 * This annotation is a mark annotation. It has no value, either on types nor on methods.
 * <p>
 * In the below code snippet controller is not declared remote so only methods explicitly annotated with &#064;Remote will be
 * accessible from client logic.
 * 
 * <pre>
 * class Controller {
 * 	...
 * 	&#064;Remote
 * 	boolean login(Login login) {
 * 	}
 * }
 * </pre>
 * 
 * This annotation should be used on managed classes. If used on plain Java classes it is silently ignored.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Remote {
}
