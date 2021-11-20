package js.tiny.container.lifecycle;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import js.util.Types;

abstract class BaseInstanceLifecycle {
	protected boolean scanAnnotatedMethod(Map<Class<?>, Method> cache, Class<?> implementationClass, Class<? extends Annotation> annotationClass) {
		for (Method method : implementationClass.getDeclaredMethods()) {
			if (hasLifecycleAnnotation(method, annotationClass)) {
				method.setAccessible(true);
				cache.put(implementationClass, method);
				return true;
			}
		}
		return false;
	}

	private boolean hasLifecycleAnnotation(Method method, Class<? extends Annotation> annotationClass) {
		Annotation annotation = method.getAnnotation(annotationClass);
		if (annotation == null) {
			return false;
		}

		if (Modifier.isStatic(method.getModifiers())) {
			throw new IllegalStateException(format("Method annotated with |%s| should not be static.", annotationClass.getCanonicalName()));
		}
		if (method.getParameterCount() != 0) {
			throw new IllegalStateException(format("Method annotated with |%s| should have no parameter.", annotationClass.getCanonicalName()));
		}
		if (!Types.isVoid(method.getGenericReturnType())) {
			throw new IllegalStateException(format("Method annotated with |%s| should not return any value.", annotationClass.getCanonicalName()));
		}
		if (method.getExceptionTypes().length != 0) {
			throw new IllegalStateException(format("Method annotated with |%s| should not throw any checked exception.", annotationClass.getCanonicalName()));
		}
		return true;
	}
}
