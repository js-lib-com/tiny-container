package js.tiny.container.security;

import java.lang.annotation.Annotation;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for <code>@RolesAllowed</code> annotation, both Jakarta and Javax packages.
 * 
 * @author Iulian Rotaru
 */
public interface IRolesAllowed {

	static boolean isAnnotationPresent(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.annotation.security.RolesAllowed.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.RolesAllowed.class) != null) {
			return true;
		}
		return false;
	}

	static Annotation scanAnnotation(IManagedMethod managedMethod) {
		Annotation rolesAllowed = managedMethod.scanAnnotation(jakarta.annotation.security.RolesAllowed.class, IManagedMethod.Flags.INCLUDE_TYPES);
		if (rolesAllowed == null) {
			rolesAllowed = managedMethod.scanAnnotation(javax.annotation.security.RolesAllowed.class, IManagedMethod.Flags.INCLUDE_TYPES);
		}
		return rolesAllowed;
	}
	
}
