package js.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import js.container.InstanceType;

/**
 * Execute managed method in a separated thread of execution. This annotation can be applied to public methods but only if
 * managed class is of {@link InstanceType#PROXY} or is remotely accessible, otherwise bug error is thrown. Also it is a bug if
 * annotated managed method does return a value - it should be of void type. {@literal @}Asynchronous annotation on private
 * method is silently ignored.
 * <p>
 * As stated, an asynchronous managed method is executed in a separated thread of execution. Since is reasonable to expect long
 * running time, asynchronous method cannot be part of a transaction scope. It is considered a bug attempting to enable
 * asynchronous mode on a transactional methods.
 * <p>
 * Asynchronous annotation does not have any parameters, it is a mark annotation. In sample code, import employees launch a
 * thread of execution and returns immediately. No value and no exception can be returned to caller.
 * 
 * <pre>
 * &#064;Service
 * class Controller {
 * 	... 
 * 	&#064;Asynchronous
 * 	public void importEmployees(MultipartForm form) {
 * 	}
 * }
 * </pre>
 * <p>
 * Since working unit is performed into separated thread there is no way to send exception back to invoker. It is implementation
 * detail how asynchronous working unit deals with exception but probably they are recorded to system logger.
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Asynchronous {
}
