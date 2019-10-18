package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark constructor as <code>test constructor</code>. Container should ignore it when select constructor for manage instance
 * creation. This annotation is ignored if applied to not managed class constructors.
 * 
 * @author Iulian Rotaru
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestConstructor {

}
