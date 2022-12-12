package com.jslib.container.spi;

public interface IInvocation {
	
	IManagedMethod method();

	Object instance();

	Object[] arguments();
	
}
