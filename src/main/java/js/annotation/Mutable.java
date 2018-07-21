package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Force mutable transaction on particular method inside a {@link Immutable} managed class. This annotation can be used only for
 * transactional managed methods. It is considered a bug if attempt to use mutable annotation on a non-transactional method.
 * <p>
 * {@literal @}Mutable annotation is silently ignored if used with plain Java objects. As one can observe from sample code
 * below, mutable annotation has no parameters.
 * 
 * <pre>
 * &#064;Transactional
 * &#064;Immutable
 * class DaoImpl implements Dao {
 * 	...   
 * 	&#064;Mutable
 * 	void saveUser(User user) {
 * 	} 		   
 * }
 * </pre>
 * 
 * In above sample all public DAO methods are transactional and immutable. Anyway, <code>saveUser</code> method is explicitly
 * marked as mutable since it does alter database content.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mutable {
}
