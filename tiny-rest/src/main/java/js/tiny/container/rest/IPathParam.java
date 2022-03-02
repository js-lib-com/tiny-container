package js.tiny.container.rest;

import java.lang.annotation.Annotation;

public interface IPathParam {

	String value();
	
	static IPathParam cast(Annotation annotation) {
		if (annotation instanceof jakarta.ws.rs.PathParam) {
			return () -> ((jakarta.ws.rs.PathParam) annotation).value();
		}
		if (annotation instanceof javax.ws.rs.PathParam) {
			return () -> ((javax.ws.rs.PathParam) annotation).value();
		}
		return null;
	}

}
