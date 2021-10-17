package js.tiny.container.spi;

/**
 * Flow processors executed just before instance destruction.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePreDestructionProcessor extends IFlowProcessor {

	void onInstancePreDestruction(IManagedClass managedClass, Object instance);

	Priority getPriority();

	/**
	 * Predefined priorities available to instance pre-destruction processors.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - release resources on instance destruction */
		DESTRUCTOR
	}
}
