package js.container.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import js.annotation.Asynchronous;
import js.annotation.Intercepted;
import js.annotation.Remote;
import js.annotation.Transactional;
import js.container.ManagedMethodSPI;
import js.container.PreInvokeInterceptor;
import js.util.Classes;

import org.junit.Test;

/**
 * Test private utility methods used by {@link ManagedClass} to scan annotations.
 * 
 * @author Iulian Rotaru
 */
public class AnnotationsScannerUnitTest {
	@Test
	public void getClassAnnotation() throws Exception {
		assertNotNull(invokeStatic("getAnnotation", AnnotatedClass.class, Transactional.class));
		assertNull(invokeStatic("getAnnotation", AnnotatedClass.class, Remote.class));
	}

	@Test
	public void hasClassAnnotation() throws Exception {
		assertTrue((boolean) invokeStatic("hasAnnotation", AnnotatedClass.class, Transactional.class));
		assertFalse((boolean) invokeStatic("hasAnnotation", AnnotatedClass.class, Remote.class));
	}

	@Test
	public void getInheritedClassAnnotation() throws Exception {
		assertNotNull(invokeStatic("getAnnotation", ClassInheritedAnnotation.class, Remote.class));
		assertNull(invokeStatic("getAnnotation", ClassInheritedAnnotation.class, Transactional.class));
	}

	@Test
	public void hasInheritedClassAnnotation() throws Exception {
		assertTrue((boolean) invokeStatic("hasAnnotation", ClassInheritedAnnotation.class, Remote.class));
		assertFalse((boolean) invokeStatic("hasAnnotation", ClassInheritedAnnotation.class, Transactional.class));
	}

	@Test
	public void getMethodAnnotation() throws Exception {
		Method method = AnnotatedMethod.class.getMethod("exec");
		assertNotNull(invokeStatic("getAnnotation", method, Asynchronous.class));
		assertNull(invokeStatic("getAnnotation", method, Transactional.class));
	}

	@Test
	public void hasMethodAnnotation() throws Exception {
		Method method = AnnotatedMethod.class.getMethod("exec");
		assertTrue((boolean) invokeStatic("hasAnnotation", method, Asynchronous.class));
		assertFalse((boolean) invokeStatic("hasAnnotation", method, Transactional.class));
	}

	@Test
	public void getInheritedMethodAnnotation() throws Exception {
		Method method = MethodInheritedAnnotation.class.getMethod("getManufacturer");
		assertNotNull(invokeStatic("getAnnotation", method, Remote.class));
		assertNull(invokeStatic("getAnnotation", method, Asynchronous.class));
	}

	@Test
	public void hasInheritedMethodAnnotation() throws Exception {
		Method method = MethodInheritedAnnotation.class.getMethod("getManufacturer");
		assertTrue((boolean) invokeStatic("hasAnnotation", method, Remote.class));
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

	@Test
	public void getInterceptorClass() throws Exception {
		Class<?> interceptor = (Class<?>) invokeStatic("getInterceptorClass", AnnotatedClass.class);
		assertNotNull(interceptor);
		assertEquals(InterceptorClass.class, interceptor);
	}

	@Test
	public void getInterceptorClass_MissingAnnotation() throws Exception {
		assertNull(invokeStatic("getInterceptorClass", AnnotatedMethod.class));
	}

	@Test
	public void getInterceptorMethod() throws Exception {
		Method method = AnnotatedMethod.class.getDeclaredMethod("exec");
		Class<?> interceptor = (Class<?>) invokeStatic("getInterceptorClass", method);
		assertNotNull(interceptor);
		assertEquals(InterceptorClass.class, interceptor);
	}

	@Test
	public void getInterceptorMethod_MissingAnnotation() throws Exception {
		Method method = AnnotatedInterface.class.getDeclaredMethod("getModel");
		assertNull(invokeStatic("getInterceptorClass", method));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static Object invokeStatic(String methodName, Object... args) throws Exception {
		Class<?> clazz = Classes.forName("js.container.ManagedClass");
		return Classes.invoke(clazz, methodName, args);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@Transactional
	@Intercepted(InterceptorClass.class)
	private static class AnnotatedClass {

	}

	private static class AnnotatedMethod {
		@Asynchronous
		@Intercepted(InterceptorClass.class)
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

	private static interface AnnotatedInterfaceMethod {
		@Remote
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
		public void preInvoke(ManagedMethodSPI managedMethod, Object[] args) throws Exception {
		}
	}
}
