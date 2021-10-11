package js.tiny.container.spi;

public interface IInstancePreDestruct extends IJoinPointProcessor {

	void preDestructInstance(IManagedClass managedClass, Object instance);

	public enum Priority {
		DESTROY
	}
}
