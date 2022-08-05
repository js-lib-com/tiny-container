package com.jslib.tiny.container.spi;

/**
 * Container services related to managed instances post processing. Instance processors are registered to container and enacted
 * by managed instance factory. Note that post processing is executed only on newly created instances but not if managed
 * instance is reused from scope factory.
 * 
 * Instance processor may have side effects on given instance, depending on specific implementation. For example logger instance
 * processor does not alter given instance whereas instance fields injection processor does inject fields value, altering
 * instance state.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePostConstructProcessor extends IFlowProcessor {

	Priority getPriority();

	default <T> boolean bind(IManagedClass<T> managedClass) {
		return true;
	}

	/**
	 * Execute specific post processing logic on instance of a given managed class. Implementation may or may not alter instance
	 * state, depending on specific kind of processing.
	 * 
	 * @param instance instance of given managed class.
	 */
	<T> void onInstancePostConstruct(T instance);

	/**
	 * Predefined priorities available to instance post processing.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - inject value to instance fields */
		INJECT,
		/** 1 - execute instance configuration from external descriptors */
		CONFIG,
		/** 2 - application specific initialization logic is executed after initialization and configuration */
		CONSTRUCTOR,
		/** 3 - timer services */
		TIMER
	}
}
