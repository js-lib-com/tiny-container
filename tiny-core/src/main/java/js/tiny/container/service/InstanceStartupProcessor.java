package js.tiny.container.service;

import java.util.SortedMap;
import java.util.TreeMap;

import javax.ejb.Startup;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IManagedClass;

/**
 * Auto-create managed instances marked with {@link Startup} annotation in the order declared with
 * {@link javax.annotation.Priority} annotation.
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
	 * Ensure all managed classes marked with {@link Startup} annotation are instantiated at container start. Takes care to
	 * execute all instance creation services like resources injection and post-construct hook.
	 * 
	 * If startup classes has also {@link javax.annotation.Priority} annotation use it to control startup order. Zero is the top
	 * priority and is guaranteed to be executed first. If more classes with the same priority order between them is not
	 * guaranteed. If priority is not declared classes are started last, in no particular order.
	 */
	@Override
	public void onContainerStart(IContainer container) {
		SortedMap<Integer, IManagedClass<?>> managedClasses = new TreeMap<>();
		for (IManagedClass<?> managedClass : container.getManagedClasses()) {
			if (managedClass.scanAnnotation(Startup.class) != null) {
				javax.annotation.Priority priorityAnnotation = managedClass.scanAnnotation(javax.annotation.Priority.class);
				int priority = priorityAnnotation != null ? priorityAnnotation.value() : Integer.MAX_VALUE;
				managedClasses.put(priority, managedClass);
			}
		}

		managedClasses.values().forEach(managedClass -> {
			log.debug("Startup managed instance |%s|.", managedClass);
			managedClass.getInstance();
		});
	}
}
