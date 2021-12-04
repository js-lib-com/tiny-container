package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.net.URI;

/**
 * Binding parameters collected by container internal bindings builder - {@link BindingParametersBuilder}.
 * 
 * @author Iulian Rotaru
 */
class BindingParameters<T> {
	private final Class<T> interfaceClass;

	private T instance;
	private Class<? extends T> implementationClass;
	private Class<? extends Annotation> scope;
	private boolean service;
	private URI implementationURL;
	
	// for now do not use type qualifier
	
	public BindingParameters(Class<T> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public BindingParameters<T> setInstance(T instance) {
		this.instance = instance;
		return this;
	}

	public BindingParameters<T> setImplementationClass(Class<? extends T> implementationClass) {
		this.implementationClass = implementationClass;
		return this;
	}

	public BindingParameters<T> setScope(Class<? extends Annotation> scope) {
		this.scope = scope;
		return this;
	}

	public BindingParameters<T> setService(boolean service) {
		this.service = service;
		return this;
	}

	public void setImplementationURL(URI implementationURL) {
		this.implementationURL = implementationURL;
	}

	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	public T getInstance() {
		return instance;
	}

	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}

	public Class<? extends Annotation> getScope() {
		return scope;
	}

	public boolean isService() {
		return service;
	}

	public URI getImplementationURL() {
		return implementationURL;
	}
}
