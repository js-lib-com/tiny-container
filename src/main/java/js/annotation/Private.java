package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Private net methods, that cannot be invoked without authorization. Trying to remotely invoke a method tagged as
 * <code>private</code> will redirect client to authentication form. Since net classes are by default <code>private</code> for
 * remote access, this annotation can be used only for methods. Using this annotation on a method that is not accessible remote
 * has no effect.
 * <p>
 * This annotation is used to selectively declare <code>private</code> couple methods in a net class that is declared
 * <code>public</code>. If {@link Public} annotation is not used on net class all methods are <code>private</code> by default
 * and &#064;Private annotation is superfluous.
 * 
 * <pre>
 * &#064;Remote
 * &#064;Public
 * class Controller {
 * 	...
 * 	&#064;Private
 * 	public CreditCard getCreditCard(int userId) {
 * 	}
 * }
 * </pre>
 * 
 * Note that this annotation has meaning only for remote access. For local invocation Java language visibility control applies.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Private {
}
