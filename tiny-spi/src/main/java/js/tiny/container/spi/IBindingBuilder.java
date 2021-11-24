package js.tiny.container.spi;

import java.net.URI;

public interface IBindingBuilder<T> {

	IBindingBuilder<T> to(Class<? extends T> implementationClass);

	IBindingBuilder<T> instance(T instance);

	IBindingBuilder<T> type(InstanceType type);

	IBindingBuilder<T> scope(InstanceScope scope);

	IBindingBuilder<T> on(URI implementationURL);
	
	void build();

}
