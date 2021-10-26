package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

@Deprecated
public interface IProviderService<T> {

	Class<? extends Annotation> getScope();

	IProviderDecorator<T> getProviderFactory();

}
