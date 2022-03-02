package js.tiny.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface IManagedParameter {

	Type getType();

	Annotation[] getAnnotations();

	<A extends Annotation> A scanAnnotation(Class<A> annotationClass);
	
}
