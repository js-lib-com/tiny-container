package js.tiny.container.spi;

/**
 * Container service implements the actual functionality provided by container. A service instance for a container can be
 * obtained only from a container service provider, see {@link IContainerServiceProvider}.
 * 
 * Container service implementation should not contain any mutable state so that it can be safely reused, and behave civilized
 * in multi-threaded running environments.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerService {

	Iterable<IServiceMeta> scan(IManagedClass managedClass);

	Iterable<IServiceMeta> scan(IManagedMethod managedMethod);

	void destroy();
}
