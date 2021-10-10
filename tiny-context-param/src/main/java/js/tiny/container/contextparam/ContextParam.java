package js.tiny.container.contextparam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field initialized from context parameter. Context parameters are configured on runtime environment and loaded at
 * container start. Both static and instance fields can be initialized from context.
 * <p>
 * By default context parameter is not mandatory. If parameter is missing from runtime environment, field is left to is
 * compile time value, either default / null value or that explicitly initialized by code. Anyway, if
 * {@link #mandatory()} attribute is configured to true, container should throw {@link RuntimeException} if context
 * parameter is missing.
 * 
 * @author Iulian Rotaru
 * @since 1.3
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextParam {
  /**
   * Context parameter name. The 'value' name may be misleading since this attribute is in fact the parameter name;
   * anyway, use it to simplify declaration when {@link #mandatory()} attribute is missing.
   * 
   * @return context parameter name.
   */
  String value();

  /**
   * If this attribute is true container should throw exception if parameter value is not defined on runtime
   * environment. By default, context parameter is not mandatory.
   * 
   * @return mandatory flag, default to false.
   */
  boolean mandatory() default false;
}
