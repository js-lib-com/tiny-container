package js.tiny.container.spi;

import java.util.List;

public interface IContainerService {
	
	List<IContainerServiceMeta> scan(IManagedClass managedClass);

	List<IContainerServiceMeta> scan(IManagedMethod managedMethod);

	void destroy();
}
