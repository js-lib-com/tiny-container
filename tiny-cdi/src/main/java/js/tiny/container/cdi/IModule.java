package js.tiny.container.cdi;

import java.util.List;

import js.tiny.container.cdi.service.CDI;

public interface IModule {
	
	IModule configure(CDI parentContainer);

	List<IBinding<?>> bindings();

}
