package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cron {
	/**
	 * The value of request URI path for annotated method.
	 * 
	 * @return method request URI path.
	 */
	String value();
}
