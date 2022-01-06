package js.tiny.container.security;

import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for <code>@PermitAll</code> annotation, both Jakarta and Javax packages.
 * 
 * @author Iulian Rotaru
 */
public interface IPermitAll {

	static boolean isAnnotationPresent(IManagedClass<?> managedClass) {
		if (managedClass.scanAnnotation(jakarta.annotation.security.PermitAll.class) != null) {
			return true;
		}
		if (managedClass.scanAnnotation(javax.annotation.security.PermitAll.class) != null) {
			return true;
		}
		return false;
	}

	static boolean isAnnotationPresent(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.annotation.security.PermitAll.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.PermitAll.class) != null) {
			return true;
		}
		return false;
	}
	
}
