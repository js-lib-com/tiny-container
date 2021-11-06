package js.tiny.container.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import js.lang.BugError;
import js.lang.ManagedLifeCycle;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;
import js.util.Params;

abstract class BaseInstanceLifeCycle {
	protected void scanLifeCycleInterface(IManagedClass<?> managedClass, Class<?> interfaceClass, String attrName) {
		Class<?> implementationClass = managedClass.getImplementationClass();
		if (hasLifeCycleInterface(implementationClass, interfaceClass)) {
			Method method = getInterfaceMethod(implementationClass, interfaceClass);
			managedClass.setAttribute(this, attrName, managedClass.getManagedMethod(method.getName()));
		}
	}

	protected void scanLifeCycleAnnotation(IManagedMethod managedMethod, Class<? extends Annotation> annotationClass, String attrName) {
		Annotation annotation = managedMethod.scanAnnotation(annotationClass);
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

	/**
	 * Gets implementation method declared on interface. Implementation is already verified that it implements the interface. It
	 * is expected that interface to have only a single method.
	 * 
	 * @param implementationClass method implementation class,
	 * @param interfaceClass single method interface implemented by implementation class.
	 * @return implementation method, never null.
	 */
	private static Method getInterfaceMethod(Class<?> implementationClass, Class<?> interfaceClass) {
		Params.isInterface(interfaceClass, "Interface class");
		Method[] methods = interfaceClass.getMethods();
		if (methods.length != 1) {
			throw new BugError("Interface |%s| does not have exactly one method.", interfaceClass);
		}
		// ignore method not found exception since at this point implementation class is known to implement the interface and
		// interface indeed has the method
		return Classes.getMethod(implementationClass, methods[0].getName(), methods[0].getParameterTypes());
	}
}
