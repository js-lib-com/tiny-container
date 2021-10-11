package js.tiny.container;

import js.lang.BugError;
import js.lang.ManagedPostConstruct;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstruct;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

/**
 * Execute {@link ManagedPostConstruct#postConstruct()} on managed instance. Instance post-construction is executed after
 * initialization and configuration, of course only if managed instance implements {@link ManagedPostConstruct} interface.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class PostConstructInstanceProcessor implements IInstancePostConstruct {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(PostConstructInstanceProcessor.class);

	@Override
	public Priority getPriority() {
		return Priority.LIFE_CYCLE;
	}

	/**
	 * Execute post-construct on managed instance. In order to perform instance post-construction, managed instance should
	 * implement {@link ManagedPostConstruct} interface.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 */
	@Override
	public void postConstructInstance(IManagedClass managedClass, Object instance) {
		IManagedMethod method = managedClass.getPostConstructMethod();
		if (method == null) {
			return;
		}
		log.debug("Post-construct managed instance |%s|", instance.getClass());
		try {
			method.invoke(instance);
		} catch (Throwable t) {
			throw new BugError("Managed instance |%s| post-construct fail: %s", instance, t);
		}
	}
}
