package js.tiny.container.spi;

public interface IMethodInvocation {
	
	IManagedMethod method();

	Object instance();

	Object[] arguments();
	
}
