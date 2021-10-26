package js.tiny.container.spi;

/**
 * Flow processors executed at container start.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerStartProcessor extends IFlowProcessor {

	void onContainerStart(IContainer container);

	Priority getPriority();

	/**
	 * Predefined priorities available to instance post processing.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - create managed instances marked with eager creation */
		START
	}
}