package js.tiny.container.spi;

public interface IMethodInvocation extends IJoinPointProcessor {

	Object invoke(IServiceChain serviceChain, IManagedMethod managedMethod, Object instance, Object[] arguments) throws Throwable;

}
