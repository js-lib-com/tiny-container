package com.jslib.container.rest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.ServiceConfigurationException;

interface IMemberInjector {

	void assertValid();

	Class<?> type();

	void inject(Object instance, Object value) throws Throwable;

	// --------------------------------------------------------------------------------------------
	
	class FieldInjector implements IMemberInjector {
		private final Field field;

		public FieldInjector(Field field) {
			this.field = field;
			this.field.setAccessible(true);
		}

		@Override
		public void assertValid() {
			if (Modifier.isStatic(field.getModifiers())) {
				throw new ServiceConfigurationException("Injection field should not be static. See |%s|.", field);
			}
			if (Modifier.isFinal(field.getModifiers())) {
				throw new ServiceConfigurationException("Injection field should not be final. See |%s|.", field);
			}
		}

		@Override
		public Class<?> type() {
			return field.getType();
		}

		@Override
		public void inject(Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
			field.set(instance, value);
		}

		@Override
		public String toString() {
			return field.toString();
		}
	}

	class MethodInjector implements IMemberInjector {
		private final IManagedMethod method;

		public MethodInjector(IManagedMethod method) {
			this.method = method;
		}

		@Override
		public void assertValid() {
			if (method.isStatic()) {
				throw new ServiceConfigurationException("Injection method should not be static. See |%s|.", method);
			}
			if (method.getExceptionTypes().length > 0) {
				throw new ServiceConfigurationException("Injection method should not have checked exceptions. See |%s|.", method);
			}
			if (method.getParameterTypes().length != 1) {
				throw new ServiceConfigurationException("Injection method should have exactly one parameter. See |%s|.", method);
			}
		}

		@Override
		public Class<?> type() {
			return (Class<?>) method.getParameterTypes()[0];
		}

		@Override
		public void inject(Object instance, Object value) throws Throwable {
			method.invoke(instance, value);
		}

		@Override
		public String toString() {
			return method.toString();
		}
	}
}
