package js.tiny.container.spi;

/**
 * Container services related to managed instances post processing. Instance processors are registered to container and enacted
 * by instance retrieval logic. Note that post processing is executed only on newly created instances but not if managed
 * instance is reused from scope factory.
 * 
 * Instance processor may have side effects on given instance, depending on specific implementation. For example
 * {@link LoggerInstanceProcessor} does not alter given instance whereas {@link InstanceFieldsInjectionProcessor} does inject
 * dependencies, altering instance state.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePostConstruct extends IJoinPointProcessor {

	/**
	 * Execute specific post processing logic on instance of a given managed class. Implementation may or may not alter instance
	 * state, depending on specific kind of processing.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 */
	void postConstructInstance(IManagedClass managedClass, Object instance);

	Priority getPriority();

	/**
	 * Predefined priorities available to instance constructor post processing.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - inject value to instance fields */
		INJECT,
		/** 1 - execute instance configuration from external descriptors */
		CONFIG,
		/** 2 - instance life-cycle management */
		LIFE_CYCLE,
		/** 3 - timer services */
		TIMER,
		/** 4 - dump instance informations to logger */
		LOGGER
	}
}
