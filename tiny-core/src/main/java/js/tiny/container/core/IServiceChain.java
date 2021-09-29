package js.tiny.container.core;

import js.tiny.container.ManagedMethodSPI;

public interface IServiceChain {
	
	Object invoke(ManagedMethodSPI managedMethod, Object instance, Object[] arguments) throws Throwable;

}
