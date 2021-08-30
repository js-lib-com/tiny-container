package js.tiny.container.annotation.unit;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remote;
import javax.interceptor.Interceptors;

import org.junit.Test;

import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.tiny.container.Container;
import js.tiny.container.Interceptor;
import js.tiny.container.ManagedClass;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.stub.ContainerStub;
import js.util.Classes;
import js.util.Strings;

@SuppressWarnings("unused")
public class InterceptedUnitTest {
	/** Using {@link Interceptors} annotation on class should be applied only on public method but not on private. */
	@Test
	public void interceptedProxyClass() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.InterceptedUnitTest$MockInterface'  class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass01' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod01", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedProxyMethod() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.InterceptedUnitTest$MockInterface' class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass02' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method02", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedRemoteAccessibleClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass03' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod03", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedControlerClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass04' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod04", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedServiceClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass05' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod05", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedRemoteAccessibleMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass06' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method06", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedControllerMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass07' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method07", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedServiceMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass08' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method08", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test(expected = BugError.class)
	public void interceptedPojoClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InterceptedUnitTest$MockClass01' />";
		ManagedClassSPI managedClass = managedClass(descriptor);
	}

	@Test(expected = ConfigException.class)
	public void interceptedPojoMethod() throws Throwable {
		String descriptor = "<test class='	InterceptedUnitTest$MockClass02' />";
		ManagedClassSPI managedClass = managedClass(descriptor);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static ManagedClassSPI managedClass(String descriptor) throws Throwable {
		Container container = new ContainerStub();
		String xml = Strings.concat("<?xml version='1.0' encoding='UTF-8' ?><managed-classes>", descriptor, "</managed-classes>");
		ConfigBuilder builder = new ConfigBuilder(xml);
		return new ManagedClass(container, builder.build().getChild("test"));
	}

	private static <T> List<T> list(Iterable<T> iterable) {
		List<T> list = new ArrayList<>();
		for (T t : iterable) {
			list.add(t);
		}
		return list;
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static interface MockInterface {
	}

	private static class MockInterceptor implements Interceptor {
	}

	@Interceptors(MockInterceptor.class)
	private static class MockClass01 implements MockInterface {
		public void publicMethod01() {
		}

		private void privateMethod010() {
		}
	}

	private static class MockClass02 implements MockInterface {
		@Interceptors(MockInterceptor.class)
		public void method02() {
		}
	}

	@Remote
	@Interceptors(MockInterceptor.class)
	private static class MockClass03 {
		public void publicMethod03() {
		}

		private void privateMethod03() {
		}
	}

	@Remote
	@Interceptors(MockInterceptor.class)
	private static class MockClass04 {
		public void publicMethod04() {
		}

		private void privateMethod04() {
		}
	}

	@Remote
	@Interceptors(MockInterceptor.class)
	private static class MockClass05 {
		public void publicMethod05() {
		}

		private void privateMethod05() {
		}
	}

	@Remote
	private static class MockClass06 {
		@Interceptors(MockInterceptor.class)
		public void method06() {
		}
	}

	@Remote
	private static class MockClass07 {
		@Interceptors(MockInterceptor.class)
		public void method07() {
		}
	}

	@Remote
	private static class MockClass08 {
		@Interceptors(MockInterceptor.class)
		public void method08() {
		}
	}
}
