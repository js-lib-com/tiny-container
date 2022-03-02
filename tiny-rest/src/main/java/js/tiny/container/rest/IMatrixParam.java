package js.tiny.container.rest;

import java.lang.annotation.Annotation;

public interface IMatrixParam {

	String value();

	static IMatrixParam cast(Annotation annotation) {
		if (annotation instanceof jakarta.ws.rs.MatrixParam) {
			return () -> ((jakarta.ws.rs.MatrixParam) annotation).value();
		}
		if (annotation instanceof javax.ws.rs.MatrixParam) {
			return () -> ((javax.ws.rs.MatrixParam) annotation).value();
		}
		return null;
	}


}
