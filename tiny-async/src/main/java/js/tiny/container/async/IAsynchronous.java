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
	 * Detect if <code>@Asynchronous</code> annotation is present on managed method or its declaring class. This utility method
	 * delegates {@link IManagedMethod#scanAnnotation(Class, js.tiny.container.spi.IManagedMethod.Flags...)} with flag set to
	 * {@link IManagedMethod.Flags#INCLUDE_TYPES} .
	 * 
	 * @param managedMethod managed method to scan.
	 * @return true if <code>@Asynchronous</code> annotation is present on managed method or its declaring class.
	 */
	static boolean isAnnotationPresent(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return true;
		}

		if (managedMethod.scanAnnotation(javax.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return true;
		}

		return false;
	}

}
