package js.tiny.container.spi;

import js.tiny.container.service.InstanceFieldsInjectionProcessor;
import js.tiny.container.service.LoggerInstanceProcessor;

/**
 * Container services related to managed instances post processing. Instance processors are registered to container and enacted
 * by managed instance factory. Note that post processing is executed only on newly created instances but not if managed
 * instance is reused from scope factory.
 * 
 * Instance processor may have side effects on given instance, depending on specific implementation. For example
 * {@link LoggerInstanceProcessor} does not alter given instance whereas {@link InstanceFieldsInjectionProcessor} does inject
 * fields value, altering instance state.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePostConstructionProcessor extends IFlowProcessor {

	/**
	 * Execute specific post processing logic on instance of a given managed class. Implementation may or may not alter instance
	 * state, depending on specific kind of processing.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 */
	void onInstancePostConstruction(IManagedClass managedClass, Object instance);

	Priority getPriority();

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
		TIMER,
		/** 4 - dump instance informations to logger */
		LOGGER
	}
}
