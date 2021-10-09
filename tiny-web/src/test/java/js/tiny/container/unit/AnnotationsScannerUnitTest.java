package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.interceptor.Interceptors;

import org.junit.Test;

import js.tiny.container.ManagedClass;
import js.tiny.container.interceptor.PreInvokeInterceptor;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

/**
 * Test private utility methods used by {@link ManagedClass} to scan annotations.
 * 
 * @author Iulian Rotaru
 */
public class AnnotationsScannerUnitTest {
	@Test
	public void hasClassAnnotation() throws Exception {
		assertFalse((boolean) invokeStatic("hasAnnotation", AnnotatedClass.class, Remote.class));
	}

	@Test
	public void hasInheritedClassAnnotation() throws Exception {
		assertTrue((boolean) invokeStatic("hasAnnotation", ClassInheritedAnnotation.class, Remote.class));
	}

	@Test
	public void hasMethodAnnotation() throws Exception {
		Method method = AnnotatedMethod.class.getMethod("exec");
		assertTrue((boolean) invokeStatic("hasAnnotation", method, Asynchronous.class));
	}

	@Test
	public void hasInheritedMethodAnnotation() throws Exception {
		Method method = MethodInheritedAnnotation.class.getMethod("getManufacturer");
		assertTrue((boolean) invokeStatic("hasAnnotation", method, PermitAll.class));
		assertFalse((boolean) invokeStatic("hasAnnotation", method, Asynchronous.class));
	}

	@Test
	public void getClassMethod() throws Exception {
		Method method = AnnotatedMethod.class.getMethod("exec");
		method = (Method) invokeStatic("getInterfaceMethod", method);
		assertNotNull(method);
		assertEquals(AnnotatedMethod.class, method.getDeclaringClass());
	}

	@Test
	public void getInterfaceMethod() throws Exception {
		Method method = ClassInheritedAnnotation.class.getMethod("getModel");
		method = (Method) invokeStatic("getInterfaceMethod", method);
		assertNotNull(method);
		assertEquals(AnnotatedInterface.class, method.getDeclaringClass());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static Object invokeStatic(String methodName, Object... args) throws Exception {
		return Classes.invoke(ManagedClass.class, methodName, args);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@Interceptors(InterceptorClass.class)
	private static class AnnotatedClass {

	}

	private static class AnnotatedMethod {
		@Asynchronous
		@Interceptors(InterceptorClass.class)
		public boolean exec() {
			return false;
		}
	}

	@Remote
	private static interface AnnotatedInterface {
		String getModel();
	}

	private static class ClassInheritedAnnotation implements AnnotatedInterface {
		@Override
		public String getModel() {
			return null;
		}
	}

	@Remote
	private static interface AnnotatedInterfaceMethod {
		@PermitAll
		String getManufacturer();
	}

	private static class MethodInheritedAnnotation implements AnnotatedInterfaceMethod {
		@Override
		public String getManufacturer() {
			return null;
		}
	}

	private static class InterceptorClass implements PreInvokeInterceptor {
		@Override
		public void preInvoke(IManagedMethod managedMethod, Object[] args) throws Exception {
		}
	}
}
