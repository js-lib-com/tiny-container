package js.tiny.container;

import js.lang.BugError;
import js.lang.ManagedPostConstruct;
import js.log.Log;
import js.log.LogFactory;

/**
 * Execute {@link ManagedPostConstruct#postConstruct()} on managed instance. Instance post-construction is executed after
 * initialization and configuration, of course only if managed instance implements {@link ManagedPostConstruct} interface.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class PostConstructInstanceProcessor implements InstanceProcessor {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(PostConstructInstanceProcessor.class);

	/**
	 * Execute post-construct on managed instance. In order to perform instance post-construction, managed instance should
	 * implement {@link ManagedPostConstruct} interface.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 */
	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		if (!(instance instanceof ManagedPostConstruct)) {
			return;
		}
		ManagedPostConstruct managedInstance = (ManagedPostConstruct) instance;
		log.debug("Post-construct managed instance |%s|", managedInstance.getClass());
		try {
			managedInstance.postConstruct();
		} catch (Throwable t) {
			throw new BugError("Managed instance |%s| post-construct fail: %s", instance, t);
		}
	}
}
