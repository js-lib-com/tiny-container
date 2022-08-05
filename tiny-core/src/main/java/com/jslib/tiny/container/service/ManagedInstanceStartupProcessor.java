package com.jslib.tiny.container.service;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.tiny.container.spi.IContainer;
import com.jslib.tiny.container.spi.IContainerStartProcessor;
import com.jslib.tiny.container.spi.IManagedClass;

import jakarta.ejb.Startup;

/**
 * Auto-create managed instances marked with {@link Startup} annotation in the order declared with
 * {@link jakarta.annotation.Priority} annotation.
 * 
 * @author Iulian Rotaru
 */
public class ManagedInstanceStartupProcessor implements IContainerStartProcessor {
	private static final Log log = LogFactory.getLog(ManagedInstanceStartupProcessor.class);

	/**
	 * Low priority value used when no priority annotation is declared. It is initialized at a reasonable large integer value
	 * and incremented on every use.
	 */
	private static final AtomicInteger LOW_PRIORITY = new AtomicInteger(Integer.MAX_VALUE >> 1);

	@Override
	public Priority getPriority() {
		return Priority.SINGLETON_START;
	}

	/**
	 * Ensure all managed classes marked with {@link Startup} annotation are instantiated at container start. Takes care to
	 * execute all instance creation services like resources injection and post-construct hook.
	 * 
	 * If startup classes has also {@link jakarta.annotation.Priority} annotation use it to control startup order. Zero is the top
	 * priority and is guaranteed to be executed first. If more classes with the same priority order between them is not
	 * guaranteed. If priority is not declared classes are started last, in no particular order.
	 */
	@Override
	public void onContainerStart(IContainer container) {
		log.trace("onContainerStart(IContainer)");

		SortedMap<Integer, IManagedClass<?>> managedClasses = new TreeMap<>();
		for (IManagedClass<?> managedClass : container.getManagedClasses()) {
			if (managedClass.scanAnnotation(Startup.class) != null) {
				jakarta.annotation.Priority priorityMeta = managedClass.scanAnnotation(jakarta.annotation.Priority.class);
				int priority = priorityMeta != null ? priorityMeta.value() : LOW_PRIORITY.getAndIncrement();
				managedClasses.put(priority, managedClass);
			}
		}

		managedClasses.values().forEach(managedClass -> {
			log.debug("Startup managed instance |%s|.", managedClass);
			managedClass.getInstance();
		});
	}
}
