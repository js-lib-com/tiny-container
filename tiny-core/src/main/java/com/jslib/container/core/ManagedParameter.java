package com.jslib.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.jslib.container.spi.IManagedParameter;

public class ManagedParameter implements IManagedParameter {
	private final Parameter interfaceParameter;
	/**
	 * Optional implementation parameter, not null only if declaring managed class has both interface and implementation. Used
	 * by parameter annotations scanning.
	 */
	private final Parameter implementationParameter;

	public ManagedParameter(Parameter interfaceParameter, Parameter implementationParameter) {
		this.interfaceParameter = interfaceParameter;
		this.implementationParameter = implementationParameter;
	}

	@Override
	public Type getType() {
		return interfaceParameter.getType();
	}

	@Override
	public Annotation[] getAnnotations() {
		if (implementationParameter == null) {
			return interfaceParameter.getAnnotations();
		}

		List<Annotation> annotations = new ArrayList<>();
		for (Annotation annotation : interfaceParameter.getAnnotations()) {
			annotations.add(annotation);
		}
		for (Annotation annotation : implementationParameter.getAnnotations()) {
			annotations.add(annotation);
		}
		return annotations.toArray(new Annotation[annotations.size()]);
	}

	@Override
	public <A extends Annotation> A scanAnnotation(Class<A> annotationClass) {
		A annotation = interfaceParameter.getAnnotation(annotationClass);
		if (annotation != null) {
			return annotation;
		}
		if (implementationParameter != null) {
			return implementationParameter.getAnnotation(annotationClass);
		}
		return null;
	}
}
