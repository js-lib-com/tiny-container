package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

@Deprecated
public interface IScopeService {

	Class<? extends Annotation> getAnnotation();

	IScope getScope();

}
