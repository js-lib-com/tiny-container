package js.tiny.container.rest;

import java.lang.annotation.Annotation;

public interface IQueryParam {

	String value();

	static IQueryParam cast(Annotation annotation) {
		if (annotation instanceof jakarta.ws.rs.QueryParam) {
			return () -> ((jakarta.ws.rs.QueryParam) annotation).value();
		}
		if (annotation instanceof javax.ws.rs.QueryParam) {
			return () -> ((javax.ws.rs.QueryParam) annotation).value();
		}
		return null;
	}


}
