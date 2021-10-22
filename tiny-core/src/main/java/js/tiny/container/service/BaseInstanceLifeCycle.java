package js.tiny.container.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import js.lang.BugError;
import js.lang.ManagedLifeCycle;
import js.lang.NoSuchBeingException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

abstract class BaseInstanceLifeCycle {
	protected void scanLifeCycleInterface(IManagedClass<?> managedClass, Class<?> interfaceClass, String attrName) {
		Class<?> implementationClass = managedClass.getImplementationClass();
		if (hasLifeCycleInterface(implementationClass, interfaceClass)) {
			Method method = getInterfaceMethod(implementationClass, interfaceClass);
			if (method != null) {
				managedClass.setAttribute(this, attrName, managedClass.getManagedMethod(method.getName()));
			}
		}
	}

	protected void scanLifeCycleAnnotation(IManagedMethod managedMethod, Class<? extends Annotation> annotationClass, String attrName) {
		Annotation annotation = managedMethod.getAnnotation(annotationClass);
		if (annotation != null) {
			IManagedClass<?> managedClass = managedMethod.getDeclaringClass();
			if (managedClass.getAttribute(this, attrName, IManagedMethod.class) != null) {
				throw new BugError("Duplicated %s method |%s|.", annotationClass, managedMethod);
			}
			managedClass.setAttribute(this, attrName, managedMethod);
		}
	}

	private static boolean hasLifeCycleInterface(Class<?> implementationClass, Class<?> interfaceClass) {
		if (implementationClass == null) {
			return false;
		}
		List<Class<?>> interfaces = Arrays.asList(implementationClass.getInterfaces());
		if (interfaces.contains(ManagedLifeCycle.class) || interfaces.contains(interfaceClass)) {
			return true;
		}
		return hasLifeCycleInterface(implementationClass.getSuperclass(), interfaceClass);
	}

	private static Method getInterfaceMethod(Class<?> implementationClass, Class<?> interfaceClass) {
		if (!interfaceClass.isInterface()) {
			throw new BugError("Type |%s| is not an interface.", interfaceClass);
		}
		Method[] methods = interfaceClass.getMethods();
		if (methods.length != 1) {
			throw new BugError("Interface |%s| does not have exactly one method.", interfaceClass);
		}
		try {
			return Classes.getMethod(implementationClass, methods[0].getName(), methods[0].getParameterTypes());
		} catch (NoSuchBeingException e) {
			return null;
		}
	}
}
