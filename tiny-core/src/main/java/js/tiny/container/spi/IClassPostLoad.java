package js.tiny.container.spi;

/**
 * Post processing executed on managed classes. These container services are executed after {@link IManagedClass} load and
 * generally deals with static fields initialization, but is not limited to.
 * 
 * @author Iulian Rotaru
 */
public interface IClassPostLoad extends IJoinPointProcessor {

	/**
	 * Execute container service logic after managed class loading.
	 * 
	 * @param managedClass just loaded managed class.
	 */
	void postLoadClass(IManagedClass managedClass);

	Priority getPriority();

	/**
	 * Predefined priorities available to class post-load processors.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - inject values to class static fields */
		INJECT
	}

}
