package js.tiny.container.cdi;

/**
 * Bindings for managed classes. This bindings are collected by CDI when pre-process injector modules and are used by container
 * to create managed classes.
 * 
 * @author Iulian Rotaru
 */
class ClassBinding<T> implements IClassBinding<T> {
	private final Class<T> interfaceClass;
	private final Class<? extends T> implementationClass;

	public ClassBinding(Class<T> interfaceClass, Class<? extends T> implementationClass) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
	}

	@Override
	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	@Override
	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}
}