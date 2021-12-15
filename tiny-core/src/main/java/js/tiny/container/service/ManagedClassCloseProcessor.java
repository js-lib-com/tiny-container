package js.tiny.container.service;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerCloseProcessor;
import js.tiny.container.spi.IManagedClass;

public class ManagedClassCloseProcessor implements IContainerCloseProcessor {
	private static final Log log = LogFactory.getLog(ManagedClassCloseProcessor.class);

	/**
	 * Low priority value used when no priority annotation is declared. It is initialized at a reasonable large integer value
	 * and incremented on every use.
	 */
	private static final AtomicInteger LOW_VALUE = new AtomicInteger(Integer.MAX_VALUE >> 1);

	@Override
	public Priority getPriority() {
		return Priority.SINGLETON_CLOSE;
	}

	@Override
	public void onContainerClose(IContainer container) {
		log.trace("onContainerClose(IContainer)");

		SortedMap<Integer, IManagedClass<?>> managedClasses = new TreeMap<>(Collections.reverseOrder());

		for (IManagedClass<?> managedClass : container.getManagedClasses()) {
			javax.annotation.Priority priorityAnnotation = managedClass.scanAnnotation(javax.annotation.Priority.class);
			int priority = priorityAnnotation != null ? priorityAnnotation.value() : LOW_VALUE.getAndIncrement();
			managedClasses.put(priority, managedClass);
		}

		managedClasses.values().forEach(IManagedClass::close);
	}
}
