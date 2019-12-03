package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the list of roles permitted to access method(s) in an application. The value of the RolesAllowed annotation is a
 * list of security role names.
 * <p>
 * This annotation can be specified on a class or on method(s). Specifying it at a class level means that it applies to all the
 * methods in the class. Specifying it on a method means that it is applicable to that method only. If applied at both the class
 * and methods level , the method value overrides the class value if the two conflict.
 * <p>
 * This annotation is cloned here from Java Enterprise Edition API in order to avoid dependency.
 * 
 * @author Iulian Rotaru
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RolesAllowed {
	String[] value();
}
