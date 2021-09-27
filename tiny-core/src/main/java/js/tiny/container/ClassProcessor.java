package js.tiny.container;

/**
 * Post processor executed on managed class. These hooks are executed by {@link Container} after {@link ManagedClass} creation
 * and generally deals with implementation static fields initialization, but not limited to.
 * <p>
 * Note that these processors are global and executed for every created managed classes.
 * 
 * @author Iulian Rotaru
 */
public interface ClassProcessor {
	/**
	 * Execute post-processing after managed class creation.
	 * 
	 * @param managedClass just created managed class.
	 */
	void postProcessClass(ManagedClassSPI managedClass);
}
