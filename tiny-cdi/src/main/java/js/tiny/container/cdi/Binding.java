package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

public class Binding<T> {
	private final Class<T> interfaceClass;
	private final Class<? extends T> implementationClass;
	private final Class<? extends Annotation> scopeClass;

	public Binding(Class<T> interfaceClass) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = null;
		this.scopeClass = null;
	}

	public Binding(Class<T> interfaceClass, Class<? extends T> implementationClass) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
		this.scopeClass = null;
	}

	public Binding(Class<T> interfaceClass, Class<? extends T> implementationClass, Class<? extends Annotation> scopeClass) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
		this.scopeClass = scopeClass;
	}

	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}

	public Class<? extends Annotation> getScopeClass() {
		return scopeClass;
	}
}