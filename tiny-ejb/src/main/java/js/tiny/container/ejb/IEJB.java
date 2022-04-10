package js.tiny.container.ejb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public interface IEJB {

	static IEJB scan(Field field) {
		Annotation ejb = field.getAnnotation(jakarta.ejb.EJB.class);
		if (ejb == null) {
			ejb = field.getAnnotation(javax.ejb.EJB.class);
		}
		if (ejb == null) {
			return null;
		}
		return new IEJB() {
		};
	}

}
