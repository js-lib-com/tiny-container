package com.jslib.tiny.container.contextparam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.jslib.converter.Converter;
import com.jslib.tiny.container.spi.IClassPostLoadedProcessor;
import com.jslib.tiny.container.spi.IManagedClass;

/**
 * Injects context parameters into class static, non final fields. Type of the field could be anything as long there is a
 * {@link Converter} registered.
 * 
 * This processor is executed immediately after class loading but before class to become available to container. Class fields
 * designed to be initialized should be annotated with the non standard {@link ContextParam} annotation.
 * 
 * @author Iulian Rotaru
 */
public class ClassContextParam extends BaseContextParam implements IClassPostLoadedProcessor {
	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> boolean onClassPostLoaded(IManagedClass<T> managedClass) {
		for (Field field : managedClass.getImplementationClass().getDeclaredFields()) {
			if (field.getAnnotation(ContextParam.class) != null && Modifier.isStatic(field.getModifiers())) {
				setField(field, null);
			}
		}
		// this processor acts on class static field and does not use instance or method services
		return false;
	}
}
