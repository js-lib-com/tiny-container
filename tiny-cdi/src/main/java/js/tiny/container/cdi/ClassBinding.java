package js.tiny.container.cdi;

public class ClassBinding<T> {
	private final Class<T> interfaceClass;
	private final Class<? extends T> implementationClass;

	public ClassBinding(Class<T> interfaceClass, Class<? extends T> implementationClass) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
	}

	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}
}