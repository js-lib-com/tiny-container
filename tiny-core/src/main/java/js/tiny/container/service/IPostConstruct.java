package js.tiny.container.service;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>annotation.PostConstruct</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IPostConstruct {

	static IPostConstruct scan(IManagedMethod managedMethod) {
		jakarta.annotation.PostConstruct jakartaPostConstruct = managedMethod.scanAnnotation(jakarta.annotation.PostConstruct.class);
		if (jakartaPostConstruct != null) {
			return new IPostConstruct() {
			};
		}

		javax.annotation.PostConstruct javaxPostConstruct = managedMethod.scanAnnotation(javax.annotation.PostConstruct.class);
		if (javaxPostConstruct != null) {
			return new IPostConstruct() {
			};
		}

		return null;
	}
	
}
