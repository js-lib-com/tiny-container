package js.tiny.container.spi;

public interface IClassDescriptor<T> {

	Class<T> getInterfaceClass();

	Class<? extends T> getImplementationClass();

	InstanceType getInstanceType();

	InstanceScope getInstanceScope();

	String getImplementationURL();

}