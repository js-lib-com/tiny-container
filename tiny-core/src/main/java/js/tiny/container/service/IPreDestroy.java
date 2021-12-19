package js.tiny.container.service;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>annotation.PreDestroy</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IPreDestroy {

	static IPreDestroy scan(IManagedMethod managedMethod) {
		jakarta.annotation.PreDestroy jakartaPreDestroy = managedMethod.scanAnnotation(jakarta.annotation.PreDestroy.class);
		if (jakartaPreDestroy != null) {
			return new IPreDestroy() {
			};
		}

		javax.annotation.PreDestroy javaxPreDestroy = managedMethod.scanAnnotation(javax.annotation.PreDestroy.class);
		if (javaxPreDestroy != null) {
			return new IPreDestroy() {
			};
		}

		return null;
	}
	
}
