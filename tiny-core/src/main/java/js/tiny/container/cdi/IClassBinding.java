package js.tiny.container.cdi;

public interface IClassBinding<T> {

	Class<T> getInterfaceClass();

	Class<? extends T> getImplementationClass();

}