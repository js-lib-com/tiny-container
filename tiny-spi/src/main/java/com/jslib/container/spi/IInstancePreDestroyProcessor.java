package com.jslib.container.spi;

/**
 * Flow processors executed just before instance destruction.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePreDestroyProcessor extends IFlowProcessor {

	Priority getPriority();

	default <T> boolean bind(IManagedClass<T> managedClass) {
		return true;
	}

	<T> void onInstancePreDestroy(T instance);

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
