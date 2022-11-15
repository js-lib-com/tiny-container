package com.jslib.container.rest;

class ParameterNotFoundException extends Exception {
	private static final long serialVersionUID = -4022795613807659233L;

	public ParameterNotFoundException(String string, Object... args) {
		super(string != null ? String.format(string, args) : null);
	}
}
