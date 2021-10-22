package js.tiny.container.spi;

public interface IServiceMetaScanner {

	Iterable<IServiceMeta> scanServiceMeta(IManagedClass<?> managedClass);

	Iterable<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod);

}
