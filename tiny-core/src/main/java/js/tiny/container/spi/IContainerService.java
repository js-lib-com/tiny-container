package js.tiny.container.spi;

import java.util.List;

/**
 * Container service implements the actual functionality provided by container. A service instance for a container can be
 * obtained only from a container service provider, see {@link IContainerServiceProvider}.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerService {

	List<IServiceMeta> scan(IManagedClass managedClass);

	List<IServiceMeta> scan(IManagedMethod managedMethod);

	void destroy();
}
