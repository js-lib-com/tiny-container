package com.jslib.container.contextparam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark non final fields for initialization from runtime context parameter. Context parameters are configured on runtime
 * environment and loaded at container start. Both static and instance fields can be initialized from context.
 * 
 * By default context parameter is not mandatory. If parameter is missing from runtime context, field is left to its compile
 * time value, either default / null value or that explicitly initialized by code. Anyway, if {@link #mandatory()} attribute is
 * configured to true, container should throw {@link RuntimeException} if context parameter is missing.
 * 
 * @author Iulian Rotaru
 * @since 1.3
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextParam {
	/**
	 * Context, case sensitive, parameter name.
	 * 
	 * @return context parameter name.
	 */
	String name();

	/**
	 * If this attribute is true container should throw exception if parameter value is not defined on runtime environment. By
	 * default, context parameter is not mandatory.
	 * 
	 * @return mandatory flag, default to false.
	 */
	boolean mandatory() default false;

	Class<? extends Parser> parser() default NullParser.class;

	static interface Parser {
		Object parse(String value) throws Exception;
	}

	static class NullParser implements Parser {
		public Object parse(String value) {
			return null;
		}
	}
}
