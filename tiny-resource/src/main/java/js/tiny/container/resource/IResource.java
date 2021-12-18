package js.tiny.container.resource;

import java.lang.reflect.Field;

/**
 * Meta interface for both Jakarta and Java <code>annotation.Resource</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IResource {

	String name();

	String lookup();

	static IResource scan(Field field) {
		jakarta.annotation.Resource jakartaResource = field.getAnnotation(jakarta.annotation.Resource.class);
		if (jakartaResource != null) {
			return new IResource() {
				@Override
				public String name() {
					return jakartaResource.name();
				}

				@Override
				public String lookup() {
					return jakartaResource.lookup();
				}
			};
		}

		javax.annotation.Resource javaxResource = field.getAnnotation(javax.annotation.Resource.class);
		if (javaxResource != null) {
			return new IResource() {
				@Override
				public String name() {
					return javaxResource.name();
				}

				@Override
				public String lookup() {
					return javaxResource.lookup();
				}
			};
		}

		return null;
	}
}
