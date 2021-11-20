package js.tiny.container.spi;

import java.lang.annotation.Annotation;

public interface IAnnotationsScanner {

	Iterable<Annotation> scanMethodAnnotations(IManagedMethod managedMethod);

}
