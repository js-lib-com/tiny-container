package js.tiny.container.async;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>ejb.Asynchronous</code> annotations. This meta interface is a marker and has
 * no attributes. Scanning is performed on both managed method and declaring managed class.
 * 
 * @author Iulian Rotaru
 */
interface IAsynchronous {

	/**
	 * Scan for <code>ejb.Asynchronous</code> annotation on given managed method and its declaring managed class. This utility
	 * method delegates {@link IManagedMethod#scanAnnotation(Class, js.tiny.container.spi.IManagedMethod.Flags...)} with flag
	 * set to {@link IManagedMethod.Flags#INCLUDE_TYPES} .
	 * 
	 * @param managedMethod managed method to scan.
	 * @return asynchronous meta interface.
	 */
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
