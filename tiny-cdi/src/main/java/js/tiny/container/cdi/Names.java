package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

import javax.inject.Named;

public class Names {

	public static Named named(final String value) {
		return new Named() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return Named.class;
			}

			@Override
			public String value() {
				return value;
			}

			@Override
			public int hashCode() {
				return toString().hashCode();
			}

			@Override
			public boolean equals(Object other) {
				if (other == null || !other.getClass().equals(this.getClass())) {
					return false;
				}
				return this.toString().equals(other.toString());
			}

			@Override
			public String toString() {
				return value;
			}
		};
	}
}
