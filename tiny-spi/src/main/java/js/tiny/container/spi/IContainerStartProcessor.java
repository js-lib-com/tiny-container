package js.tiny.container.spi;

import jakarta.ejb.Startup;

/**
 * Extension processors executed at container start.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerStartProcessor extends IFlowProcessor {

	void onContainerStart(IContainer container);

	Priority getPriority();

	/** Predefined container start priorities available to processor. */
	enum Priority implements IPriority {
		/** 0 - eager instance creation for managed classes marked with {@link Startup} annotation */
		SINGLETON_START
	}

}
