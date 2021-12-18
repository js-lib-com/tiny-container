package js.tiny.container.rest;

import java.lang.annotation.Annotation;

/**
 * Meta interface for both Jakarta and Java <code>ws.rs.HttpMethod</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IHttpMethod {

	String value();

	static IHttpMethod scan(Annotation annotation) {
		jakarta.ws.rs.HttpMethod jakartaHttpMethod = annotation.annotationType().getAnnotation(jakarta.ws.rs.HttpMethod.class);
		if (jakartaHttpMethod != null) {
			return () -> jakartaHttpMethod.value();
		}

		javax.ws.rs.HttpMethod javaxHttpMethod = annotation.annotationType().getAnnotation(javax.ws.rs.HttpMethod.class);
		if (javaxHttpMethod != null) {
			return () -> javaxHttpMethod.value();
		}

		return null;
	}

}
