package com.jslib.tiny.container.ejb;

import java.lang.reflect.Field;

import com.jslib.util.Strings;

public class EjbField {
	private final Field field;
	private final String implementationURL;

	public EjbField(Field field, String implementationURL) {
		this.field = field;
		this.implementationURL = implementationURL;
	}

	public Class<?> getType() {
		return field.getType();
	}

	public String getImplementationURL() {
		return implementationURL;
	}

	public Field getField() {
		return field;
	}

	@Override
	public String toString() {
		return Strings.toString(field.getType().getCanonicalName(), implementationURL);
	}
}
