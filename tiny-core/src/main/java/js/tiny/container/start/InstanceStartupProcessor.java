package js.tiny.container.start;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IManagedClass;

/**
 * Auto-create managed instances marked with {@link Startup} annotation.
 * 
 * @author Iulian Rotaru
 */
public class InstanceStartupProcessor implements IContainerStartProcessor {
	private static final Log log = LogFactory.getLog(InstanceStartupProcessor.class);

	@Override
	public Priority getPriority() {
		return Priority.START;
	}

	/**
	 * Ensure all managed classes marked with 'auto-creation' are instantiated. Invoked at a final stage of container
	 * initialization, this method checks every managed class that has {@link IManagedClass#isAutoInstanceCreation()} flag set
	 * and ensure is instantiated.
	 * <p>
	 * Takes care to instantiate, configure if the case, and execute post-construct in the order from application descriptor.
	 * This is critical for assuring that {@link App} is created first; {@link App} class descriptor is declared first into
	 * application descriptor.
	 * <p>
	 * Note that this method does not explicitly execute {@link PostConstruct} hooks; this hooks are actually executed by
	 * instance processor from life cycle module.
	 */
	@Override
	public void onContainerStart(IContainer container) {
		for (IManagedClass<?> managedClass : container.getManagedClasses()) {
			if (managedClass.getAnnotation(Startup.class) != null) {
				// call getInstance to ensure managed instance with managed lifecycle is started
				// getInstance() will create instance only if not already exist; returned value is ignored
				log.debug("Create managed instance with managed lifecycle |%s|.", managedClass);
				managedClass.getInstance();
			}
		}
	}
}
