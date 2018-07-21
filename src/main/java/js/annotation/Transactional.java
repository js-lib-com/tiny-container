package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.container.InstanceType;

/**
 * Execute managed method into transactional scope. This annotation can be applied to individual methods or on entire class, in
 * which case all public methods become transactional. Transactional annotation on protected or private method is silently
 * ignored.
 * <p>
 * Managed class should be of {@link InstanceType#PROXY} type otherwise bug error is thrown. It is considered also a bug if
 * combine transactional and asynchronous annotations. Finally, this annotation has no parameters.
 * 
 * <pre>
 *  &#064;Transactional
 *  class DaoImpl implements Dao {
 *      ...
 *      public User getUserByLogin(Login login) {
 *      }
 *  }
 * </pre>
 * 
 * In above sample code all public DAO methods are transactional. Please note that by default transactional managed class is
 * mutable. One can use {@link Immutable} to reverse this behavior and {@link Mutable} to select only desired methods.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
}
