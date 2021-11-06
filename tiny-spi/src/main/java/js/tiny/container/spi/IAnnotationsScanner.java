package js.tiny.container.spi;

import java.lang.annotation.Annotation;

public interface IAnnotationsScanner {

	Iterable<Annotation> scanClassAnnotations(IManagedClass<?> managedClass);

	Iterable<Annotation> scanMethodAnnotations(IManagedMethod managedMethod);

}
