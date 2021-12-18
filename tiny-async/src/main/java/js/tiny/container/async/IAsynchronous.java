package js.tiny.container.async;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>ejb.Asynchronous</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IAsynchronous {

	static IAsynchronous scan(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return new IAsynchronous() {
			};
		}

		if (managedMethod.scanAnnotation(javax.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return new IAsynchronous() {
			};
		}

		return null;
	}

}
