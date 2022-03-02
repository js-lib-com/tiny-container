package js.tiny.container.rest;

import java.lang.annotation.Annotation;

public interface IHeaderParam {

	String value();

	static IHeaderParam cast(Annotation annotation) {
		if (annotation instanceof jakarta.ws.rs.HeaderParam) {
			return () -> ((jakarta.ws.rs.HeaderParam) annotation).value();
		}
		if (annotation instanceof javax.ws.rs.HeaderParam) {
			return () -> ((javax.ws.rs.HeaderParam) annotation).value();
		}
		return null;
	}

}
