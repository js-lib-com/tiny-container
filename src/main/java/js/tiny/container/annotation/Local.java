package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Forbid remote access to particular methods inside a {@link Remote} accessible managed class. Local annotation can be applied
 * only to remote accessible managed methods; otherwise bug error is thrown when container parses managed class annotations.
 * Behavior is not specified if use both {@literal @} Remote and {@literal @}Local on the same managed method.
 * <p>
 * If used on plain Java objects this annotation is silently ignored.
 * 
 * <pre>
 * &#064;Remote
 * class Controller {
 * 	... 
 * 	&#064;Local
 * 	public void localMethod() {
 * 	}
 * }
 * </pre>
 * 
 * In above controller all methods are declared as remote accessible less <code>localMethod</code> that is forced to local.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Local {
}
