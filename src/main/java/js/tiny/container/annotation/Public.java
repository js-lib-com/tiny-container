package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Net class or method that do not require authorization. If applied to class or interface all owned methods become <b>public
 * for remote access</b>, unless individual method is tagged as {@link Private}. Trying to annotate as <code>public</code> a
 * method that is not accessible remote has no effect. This annotation has no parameter.
 * <p>
 * In sample code below controller is declared remote but because is not explicitly tagged as public, all its methods are
 * private - that is, require authenticated security context. Anyway, login method should be accessible public in order to
 * allows for authentication.
 * 
 * <pre>
 * &#064;Remote
 * class Controller {
 * 	...
 * 	&#064;Public
 * 	public boolean login(Login login) {
 * 	}
 * }
 * </pre>
 * 
 * Note that this annotation has meaning only for remote access. For local invocation Java visibility control applies.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Public {
}
