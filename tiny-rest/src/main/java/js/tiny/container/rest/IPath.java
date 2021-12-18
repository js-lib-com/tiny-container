package js.tiny.container.rest;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>ws.rs.Path</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IPath {

	String value();

	static IPath scan(IManagedClass<?> managedClass) {
		return scan(managedClass::scanAnnotation);
	}

	static IPath scan(IManagedMethod managedMethod) {
		return scan(managedMethod::scanAnnotation);
	}

	static IPath scan(Function<Class<? extends Annotation>, Annotation> scanner) {
		jakarta.ws.rs.Path jakartaPath = (jakarta.ws.rs.Path) scanner.apply(jakarta.ws.rs.Path.class);
		if (jakartaPath != null) {
			return () -> jakartaPath.value();
		}

		javax.ws.rs.Path javaxPath = (javax.ws.rs.Path) scanner.apply(javax.ws.rs.Path.class);
		if (javaxPath != null) {
			return () -> javaxPath.value();
		}

		return null;
	}
}
