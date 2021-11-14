package js.tiny.container.spi;

/**
 * Join point processors executed after managed class loaded. These container services generally deals with static fields
 * initialization, but is not limited to.
 * 
 * @author Iulian Rotaru
 */
public interface IClassPostLoadedProcessor extends IFlowProcessor {

	/**
	 * Execute container service logic after managed class loaded.
	 * 
	 * @param managedClass just loaded managed class.
	 */
	<T> void onClassPostLoaded(IManagedClass<T> managedClass);

	Priority getPriority();

	/**
	 * Predefined priorities available to class post-load processors.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - register managed class to external services, e.g. CDI */
		REGISTER,
		/** 1 - inject values to class static fields */
		INJECT,
		/** 2 - scan class annotations for on the fly processing */
		SCAN
	}

}
