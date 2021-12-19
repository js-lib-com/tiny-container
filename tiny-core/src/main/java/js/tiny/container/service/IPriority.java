package js.tiny.container.service;

import js.tiny.container.spi.IManagedClass;

/**
 * Meta interface for both Jakarta and Java <code>annotation.Priority</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IPriority {

	int value();

	static IPriority scan(IManagedClass<?> managedClass) {
		jakarta.annotation.Priority jakartaPriority = managedClass.scanAnnotation(jakarta.annotation.Priority.class);
		if (jakartaPriority != null) {
			return () -> jakartaPriority.value();
		}

		javax.annotation.Priority javaxPriority = managedClass.scanAnnotation(javax.annotation.Priority.class);
		if (javaxPriority != null) {
			return () -> javaxPriority.value();
		}

		return null;
	}
}
