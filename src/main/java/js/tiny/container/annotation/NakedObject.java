package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Experimental. A naked object is a model object that is used without changes into all application layers: persistence,
 * business logic and user interface. Tools should generate / update database tables, hibernate mappings, user interface views
 * and forms when create / update an naked object. This annotation should apply to model classes.
 * 
 * @author Iulian Rotaru
 * @version experimental
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NakedObject {
}
