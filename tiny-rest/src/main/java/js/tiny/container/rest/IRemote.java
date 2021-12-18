package js.tiny.container.rest;

import js.tiny.container.spi.IManagedClass;

/**
 * Meta interface for both Jakarta and Java <code>ejb.Remote</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IRemote {

	Class<?>[] value();

	static IRemote scan(IManagedClass<?> managedClass) {
		jakarta.ejb.Remote jakartaRemote = managedClass.scanAnnotation(jakarta.ejb.Remote.class);
		if (jakartaRemote != null) {
			return () -> jakartaRemote.value();
		}

		javax.ejb.Remote javaxRemote = managedClass.scanAnnotation(javax.ejb.Remote.class);
		if (javaxRemote != null) {
			return () -> javaxRemote.value();
		}

		return null;
	}
}
