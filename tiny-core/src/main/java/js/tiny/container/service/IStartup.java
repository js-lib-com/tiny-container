package js.tiny.container.service;

import js.tiny.container.spi.IManagedClass;

/**
 * Meta interface for both Jakarta and Java <code>ejb.Startup</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IStartup {

	static IStartup scan(IManagedClass<?> managedClass) {
		jakarta.ejb.Startup jakartaStartup = managedClass.scanAnnotation(jakarta.ejb.Startup.class);
		if (jakartaStartup != null) {
			return new IStartup() {
			};
		}

		javax.ejb.Startup javaxStartup = managedClass.scanAnnotation(javax.ejb.Startup.class);
		if (javaxStartup != null) {
			return new IStartup() {
			};
		}

		return null;
	}
}
