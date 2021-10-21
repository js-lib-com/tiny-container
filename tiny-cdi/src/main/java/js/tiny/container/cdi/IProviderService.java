package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

public interface IProviderService<T> {

	Class<? extends Annotation> getScope();

	IProviderDecorator<T> getProviderFactory();

}
