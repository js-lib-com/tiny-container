package com.jslib.container.spi;

/**
 * Join point processors executed after managed class loaded. These container services generally deals with static fields
 * initialization, but is not limited to.
 * 
 * @author Iulian Rotaru
 */
public interface IClassPostLoadedProcessor extends IFlowProcessor {

	Priority getPriority();

	/**
	 * Execute container service logic after managed class loaded. This processor is executed on managed class loading but
	 * before any instance created for this particular managed class. Implementation should decide if it has instance and method
	 * invocation services that require managed class on runtime and return true if so.
	 * 
	 * @param managedClass just loaded managed class.
	 * @return true if managed class should be created.
	 */
	<T> boolean onClassPostLoaded(IManagedClass<T> managedClass);

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
