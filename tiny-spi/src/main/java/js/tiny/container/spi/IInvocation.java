package js.tiny.container.spi;

public interface IInvocation {
	
	IManagedMethod method();

	Object instance();

	Object[] arguments();
	
}
