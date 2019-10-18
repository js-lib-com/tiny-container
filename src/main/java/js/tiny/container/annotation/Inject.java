package js.tiny.container.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Managed instance field value injection. This annotation takes effect only when applied to managed classes fields; on other
 * contexts is simply ignored. Usually target fields are of managed class type but plain Java objects are supported, of course
 * if instantiable with no argument. Field access modifier is not relevant but is not allowed to use this annotation on final or
 * static fields.
 * <p>
 * As result from sample code below injection annotation does not have any parameters.
 * 
 * <pre>
 * class DaoImpl implements Dao {
 * 	&#064;Inject
 * 	private SessionManager sm;
 * 	...
 * }
 * </pre>
 * <p>
 * Field injection is in contrast with constructor injection that can be enacted by simple declaring constructor with fields to
 * inject. Above fields injection can be rewritten as sample below.
 * 
 * <pre>
 * class DaoImpl implements Dao {
 * 	private final SessionManager sm;
 * 	...
 * 	public DaoImpl(SesionManager sm) {
 * 		this.sm = sm;
 * 	}
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
}
