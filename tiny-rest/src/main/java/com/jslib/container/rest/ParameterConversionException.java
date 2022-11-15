package com.jslib.container.rest;

class ParameterConversionException extends Exception {
	private static final long serialVersionUID = 1349237189017002969L;

	private final AnnotationType annotationType;

	public ParameterConversionException(AnnotationType annotationType, String string, Object... args) {
		super(string != null ? String.format(string, args) : null);
		this.annotationType = annotationType;
	}

	public AnnotationType getAnnotationType() {
		return annotationType;
	}
}
