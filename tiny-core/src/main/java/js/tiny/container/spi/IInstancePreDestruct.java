package js.tiny.container.spi;

public interface IInstancePreDestruct {
	
	void preDestructInstance(IManagedClass managedClass, Object instance);
	
}
