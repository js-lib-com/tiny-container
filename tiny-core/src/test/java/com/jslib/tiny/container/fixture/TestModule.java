package com.jslib.tiny.container.fixture;

import java.util.ArrayList;
import java.util.List;

import com.jslib.api.injector.IBinding;
import com.jslib.api.injector.IInjector;
import com.jslib.api.injector.IModule;

public class TestModule implements IModule {
	private final List<IBinding<?>> bindings = new ArrayList<>();
	
	public TestModule(Class<?> interfaceClass) {
		
	}
	
	@Override
	public IModule configure(IInjector injector) {
		return this;
	}

	@Override
	public List<IBinding<?>> bindings() {
		return bindings;
	}
}
