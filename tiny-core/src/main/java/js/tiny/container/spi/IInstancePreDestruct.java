package js.tiny.container.spi;

public interface IInstancePreDestruct extends IJoinPointProcessor {

	void preDestructInstance(IManagedClass managedClass, Object instance);

	Priority getPriority();

	enum Priority implements IPriority {
		DESTROY
	}
}
