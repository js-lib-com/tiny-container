package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

public class ContainerBinding<T> {
	private final Class<T> interfaceClass;
	
	private T instance;
	private Class<? extends T> implementationClass;
	private Class<? extends Annotation> scope;
	private boolean service;

	public ContainerBinding(Class<T> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public void setInstance(T instance) {
		this.instance = instance;
	}

	public void setImplementationClass(Class<? extends T> implementationClass) {
		this.implementationClass = implementationClass;
	}

	public void setScope(Class<? extends Annotation> scope) {
		this.scope = scope;
	}

	public void setService(boolean service) {
		this.service = service;
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
