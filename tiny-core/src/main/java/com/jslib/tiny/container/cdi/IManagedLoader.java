package com.jslib.tiny.container.cdi;

import com.jslib.tiny.container.spi.IManagedClass;

/**
 * Retrieve a managed class from container. This interface is designed specifically for {@link ProxyProvider} and is used only
 * for embedded containers when proxy processing is enabled.
 * 
 * @author Iulian Rotaru
 */
public interface IManagedLoader {

	/**
	 * Get managed class bound to requested interface class or null if none found.
	 * 
	 * @param interfaceClass interface class to retrieve managed class for.
	 * @return managed class bound to requested interface or null.
	 */
	<T> IManagedClass<T> getManagedClass(Class<T> interfaceClass);

}
