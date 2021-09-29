package js.tiny.container.core;

import java.util.List;

import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;

public interface IContainerService {

	List<IServiceMeta> scan(ManagedClassSPI managedClass);

	List<IServiceMeta> scan(ManagedMethodSPI managedMethod);

	void destroy();
	
}
