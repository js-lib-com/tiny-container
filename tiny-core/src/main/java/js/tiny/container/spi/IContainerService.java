package js.tiny.container.spi;

/**
 * Container service implements the actual functionality provided by container. A service instance for a container can be
 * obtained only from a container service provider, see {@link IContainerServiceProvider}.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerService {

	Iterable<IServiceMeta> scan(IManagedClass managedClass);

	Iterable<IServiceMeta> scan(IManagedMethod managedMethod);
	
	void destroy();
}
