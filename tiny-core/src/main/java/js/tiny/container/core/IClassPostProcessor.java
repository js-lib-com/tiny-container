package js.tiny.container.core;

import js.tiny.container.Container;
import js.tiny.container.ManagedClass;
import js.tiny.container.ManagedClassSPI;

/**
 * Post processor executed on managed class. These hooks are executed by {@link Container} after {@link ManagedClass} creation
 * and generally deals with implementation static fields initialization, but not limited to.
 * <p>
 * Note that these processors are global and executed for every created managed classes.
 * 
 * @author Iulian Rotaru
 */
public interface IClassPostProcessor extends IJoinPointProcessor {
	/**
	 * Execute post-processing after managed class creation.
	 * 
	 * @param managedClass just created managed class.
	 */
	void postProcessClass(ManagedClassSPI managedClass);
}
