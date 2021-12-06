package js.tiny.container.spi;

/**
 * Extension processors executed at container close.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerCloseProcessor extends IFlowProcessor {

	void onContainerClose(IContainer container);

	Priority getPriority();

	/** Predefined container close priorities available to processor. */
	enum Priority implements IPriority {
		/** 0 - close managed classes with singleton life span */
		SINGLETON_CLOSE
	}
	
}
