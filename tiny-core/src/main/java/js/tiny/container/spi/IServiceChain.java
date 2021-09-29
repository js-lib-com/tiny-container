package js.tiny.container.spi;

public interface IServiceChain {
	
	Object invoke(IManagedMethod managedMethod, Object instance, Object[] arguments) throws Throwable;

}
