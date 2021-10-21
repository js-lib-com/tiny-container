package js.tiny.container.cdi;

import java.util.List;

public interface IModule {

	void setScopeProviders(IProviders scopeProviders);
	
	void configure();

	List<IBinding<?>> getBindings();

}
