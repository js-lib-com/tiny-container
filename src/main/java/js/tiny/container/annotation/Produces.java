package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.tiny.container.http.ContentType;

/**
 * Define content type to be used when serialize method returned value to the HTTP response. Annotation value is mandatory and
 * should be a valid content type as accepted by {@link ContentType#valueOf(String)}.
 * 
 * <pre>
 * &#064;Remote
 * class Controller {
 * 	...
 * 	&#064;Produces("text/plain")
 * 	public String method() {
 * 	}
 * }
 * </pre>
 * 
 * Note that this annotation have meaning only for remote methods. When method is invoked locally this annotation is ignored.
 * 
 * 
 * @author Iulian Rotaru
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Produces {
	String value();
}
