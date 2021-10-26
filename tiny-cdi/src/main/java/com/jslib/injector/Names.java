package com.jslib.injector;

import java.lang.annotation.Annotation;

import javax.inject.Named;

/**
 * Utility class for named qualifier annotations.
 * 
 * @author Iulian Rotaru
 */
public class Names {

	/**
	 * Get {@link Named} annotation wrapping given string value. Returned annotation is guaranteed to have hash code and equals
	 * predicate based on string value parameter.
	 * 
	 * @param value string value for {@literal @Named} annotation.
	 * @return {@literal @Named} annotation.
	 */
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
				return value().hashCode();
			}

			@Override
			public boolean equals(Object other) {
				if (other == null || !(other instanceof Named)) {
					return false;
				}
				return this.value().equals(((Named) other).value());
			}

			@Override
			public String toString() {
				return value;
			}
		};
	}

}
