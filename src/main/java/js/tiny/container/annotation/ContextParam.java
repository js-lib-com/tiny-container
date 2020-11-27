package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field initialized from context parameter. Context parameters are configured on deployment and loaded at tiny container start.
 * Both static and instance fields can be initialized from context.
 * <p>
 * By default context parameter is not mandatory. If parameter is missing from runtime context, field is left to is compile time
 * value, either default / null value or that explicitly initialized by code. Anyway, if {@link #mandatory()} attribute is
 * configured to true, tiny container throws {@link RuntimeException} if context parameter is missing.
 * 
 * @author Iulian Rotaru
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextParam {
	/**
	 * Context parameter name. The 'value' name may be misleading since this attribute is in fact the parameter name; anyway use
	 * it to simplify declaration when {@link #mandatory()} attribute is missing.
	 * 
	 * @return context parameter name.
	 */
	String value();

	/**
	 * If this attribute is true tiny container throws exception if parameter value is not defined on runtime context. By
	 * default, context parameter is not mandatory.
	 * 
	 * @return mandatory flag, default to false.
	 */
	boolean mandatory() default false;
}
