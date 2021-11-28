package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

/**
 * Binding parameters collected by container internal bindings builder - {@link ContainerBindingBuilder}.
 * 
 * @author Iulian Rotaru
 */
class ContainerBindingParameters<T> {
	private final Class<T> interfaceClass;

	private T instance;
	private Class<? extends T> implementationClass;
	private Class<? extends Annotation> scope;
	private boolean service;

	public ContainerBindingParameters(Class<T> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public ContainerBindingParameters<T> setInstance(T instance) {
		this.instance = instance;
		return this;
	}

	public ContainerBindingParameters<T> setImplementationClass(Class<? extends T> implementationClass) {
		this.implementationClass = implementationClass;
		return this;
	}

	public ContainerBindingParameters<T> setScope(Class<? extends Annotation> scope) {
		this.scope = scope;
		return this;
	}

	public ContainerBindingParameters<T> setService(boolean service) {
		this.service = service;
		return this;
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
}
