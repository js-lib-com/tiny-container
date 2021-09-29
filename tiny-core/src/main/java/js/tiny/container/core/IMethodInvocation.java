package js.tiny.container.core;

import js.tiny.container.ManagedMethodSPI;

public interface IMethodInvocation {

	Object invoke(IServiceChain serviceChain, ManagedMethodSPI managedMethod, Object instance, Object[] arguments) throws Throwable;

}
