package js.tiny.container.cdi;

import js.tiny.container.spi.IManagedClass;

public interface IManagedLoader {

	/**
	 * Get managed class bound to requested interface class or null if none found.
	 * 
	 * @param interfaceClass interface class to retrieve managed class for.
	 * @return managed class bound to requested interface or null.
	 */
	<T> IManagedClass<T> getManagedClass(Class<T> interfaceClass);

}
