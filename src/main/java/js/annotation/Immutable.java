package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark transational class or method as immutable, that is, read-only transaction. {@literal @}Immutable annotation can be
 * applied to both classes and methods. If applied to class all transactional methods become immutable; this can be overridden
 * using {@link Mutable} annotation. Note that {@link Transactional} classes are by default mutable.
 * <p>
 * {@literal @}Immutable annotation has meaning only on a transactional managed method. It is considered a bug if dare to use
 * immutable annotation on a non-transactional class or method. {@literal @}Immutable annotation is ignored if applied to not
 * managed, plain Java objects. Finally, as result from sample code, immutable annotation has no parameter.
 * 
 * <pre>
 *  &#064;Transactional
 *  class DaoImpl implements Dao {
 *      ...
 *      &#064;Immutable
 *      public User getUserByLogin(Login login) {
 *      }
 *  }
 * </pre>
 * 
 * In above sample all public DAO methods are transactional and by default mutable. Anyway, <code>getUserByLogin</code> method
 * is explicitly marked as immutable since it does not alter database content.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Immutable {
}
