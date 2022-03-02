package js.tiny.container.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import js.tiny.container.spi.IManagedParameter;

public class ManagedParameter implements IManagedParameter {
	private final Parameter parameter;

	public ManagedParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	@Override
	public Type getType() {
		return parameter.getType();
	}

	@Override
	public Annotation[] getAnnotations() {
		return parameter.getAnnotations();
	}

	@Override
	public <A extends Annotation> A scanAnnotation(Class<A> annotationClass) {
		return parameter.getAnnotation(annotationClass);
	}
}
