package js.tiny.container.security;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for <code>@DenyAll</code> annotation, both Jakarta and Javax packages.
 * 
 * @author Iulian Rotaru
 */
public interface IDenyAll {

	static boolean isAnnotationPresent(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.annotation.security.DenyAll.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.DenyAll.class) != null) {
			return true;
		}
		return false;
	}
	
	static boolean isDenyAll(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.annotation.security.DenyAll.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.DenyAll.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return true;
		}
		return false;
	}

}
