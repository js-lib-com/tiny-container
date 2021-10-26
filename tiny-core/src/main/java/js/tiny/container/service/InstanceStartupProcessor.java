package js.tiny.container.service;

import java.util.Set;
import java.util.TreeSet;

import javax.ejb.Startup;

import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IManagedClass;
import js.util.Types;

/**
 * Auto-create managed instances marked with {@link Startup} annotation or implementing {@link ManagedLifeCycle} interface.
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
	 * Note that this method does not explicitly execute {@link ManagedLifeCycle#postConstruct()} hooks; this hooks are actually
	 * executed by instance processor, see {link {@link InstancePostConstructProcessor}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onContainerStart(IContainer container) {
		// compare first with second to ensure ascending sorting
		Set<IManagedClass<?>> startupClasses = new TreeSet<>((o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		for (IManagedClass<?> managedClass : container.getManagedClasses()) {
			if (isStartup(managedClass)) {
				startupClasses.add(managedClass);
			}
		}

		for (IManagedClass<?> managedClass : startupClasses) {
			// call getInstance to ensure managed instance with managed life cycle is started
			// if there are more than one single interface peek one, no matter which; the simple way is to peek the first
			// getInstance() will create instance only if not already exist; returned value is ignored

			log.debug("Create managed instance with managed life cycle |%s|.", managedClass.getInterfaceClass());
			container.getInstance((Class<? super Object>) managedClass.getInterfaceClass());
		}
	}

	private static boolean isStartup(IManagedClass<?> managedClass) {
		if (Types.isKindOf(managedClass.getInterfaceClass(), ManagedLifeCycle.class)) {
			return true;
		}
		if (!managedClass.getInstanceType().requiresImplementation()) {
			return false;
		}
		return managedClass.getAnnotation(Startup.class) != null;
	}
}
