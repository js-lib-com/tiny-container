package com.jslib.injector;

import java.util.List;

import js.tiny.container.cdi.CDI;

public interface IModule {
	
	IModule configure(CDI parentContainer);

	List<IBinding<?>> bindings();

}
