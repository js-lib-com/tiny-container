package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

public interface IInjector {
	
	<T> T getInstance(Class<T> type);
	
	<T> T getInstance(Class<T> type, Annotation qualifier);
	
	<T> T getInstance(Class<T> type, String name);

}
