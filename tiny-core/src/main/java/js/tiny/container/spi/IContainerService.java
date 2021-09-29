package js.tiny.container.spi;

import java.util.List;

public interface IContainerService {

	List<IServiceMeta> scan(IManagedClass managedClass);

	List<IServiceMeta> scan(IManagedMethod managedMethod);

	void destroy();
	
}
