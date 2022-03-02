package js.tiny.container.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IManagedParameter;

/**
 * Meta interface for both Jakarta and Java <code>ws.rs.core.Context</code> annotations.
 * 
 * @author Iulian Rotaru
 */
public interface IContext {

	static boolean scan(IManagedMethod managedMethod) {
		jakarta.ws.rs.core.Context jakartaContext = managedMethod.scanAnnotation(jakarta.ws.rs.core.Context.class);
		if (jakartaContext != null) {
			return true;
		}

		javax.ws.rs.core.Context javaxContext = managedMethod.scanAnnotation(javax.ws.rs.core.Context.class);
		if (javaxContext != null) {
			return true;
		}

		return false;
	}

	static boolean scan(IManagedParameter managedParameter) {
		jakarta.ws.rs.core.Context jakartaContext = managedParameter.scanAnnotation(jakarta.ws.rs.core.Context.class);
		if (jakartaContext != null) {
			return true;
		}

		javax.ws.rs.core.Context javaxContext = managedParameter.scanAnnotation(javax.ws.rs.core.Context.class);
		if (javaxContext != null) {
			return true;
		}

		return false;
	}

	static boolean scan(Field field) {
		jakarta.ws.rs.core.Context jakartaContext = field.getAnnotation(jakarta.ws.rs.core.Context.class);
		if (jakartaContext != null) {
			return true;
		}

		javax.ws.rs.core.Context javaxContext = field.getAnnotation(javax.ws.rs.core.Context.class);
		if (javaxContext != null) {
			return true;
		}

		return false;
	}

	static IContext cast(Annotation annotation) {
		if (annotation instanceof jakarta.ws.rs.core.Context) {
			return new IContext() {
			};
		}
		if (annotation instanceof javax.ws.rs.core.Context) {
			return new IContext() {
			};
		}
		return null;
	}

}
