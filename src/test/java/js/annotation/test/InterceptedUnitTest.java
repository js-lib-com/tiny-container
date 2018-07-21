package js.annotation.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import js.annotation.Controller;
import js.annotation.Inject;
import js.annotation.Intercepted;
import js.annotation.Remote;
import js.annotation.Service;
import js.container.ContainerSPI;
import js.container.Interceptor;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.InvocationException;
import js.test.stub.ContainerStub;
import js.util.Classes;
import js.util.Strings;

import org.junit.Test;

@SuppressWarnings("unused")
public class InterceptedUnitTest {
	/** Using {@link Intercepted} annotation on class should be applied only on public method but not on private. */
	@Test
	public void interceptedProxyClass() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.InterceptedUnitTest$MockInterface'  class='js.annotation.test.InterceptedUnitTest$MockClass01' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod01", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedProxyMethod() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.InterceptedUnitTest$MockInterface' class='js.annotation.test.InterceptedUnitTest$MockClass02' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method02", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedRemoteAccessibleClass() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass03' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod03", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedControlerClass() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass04' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod04", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedServiceClass() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass05' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("publicMethod05", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedRemoteAccessibleMethod() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass06' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method06", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedControllerMethod() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass07' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method07", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void interceptedServiceMethod() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass08' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method08", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test(expected = BugError.class)
	public void interceptedPojoClass() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass01' />";
		ManagedClassSPI managedClass = managedClass(descriptor);
	}

	@Test(expected = BugError.class)
	public void interceptedPojoMethod() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InterceptedUnitTest$MockClass02' />";
		ManagedClassSPI managedClass = managedClass(descriptor);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static ManagedClassSPI managedClass(String descriptor) throws Throwable {
		ContainerSPI container = new ContainerStub();
		String xml = Strings.concat("<?xml version='1.0' encoding='UTF-8' ?><managed-classes>", descriptor, "</managed-classes>");
		ConfigBuilder builder = new ConfigBuilder(xml);
		try {
			return Classes.newInstance("js.container.ManagedClass", container, builder.build().getChild("test"));
		} catch (InvocationException e) {
			throw e.getCause();
		}
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

	@Intercepted(MockInterceptor.class)
	private static class MockClass01 implements MockInterface {
		public void publicMethod01() {
		}

		private void privateMethod010() {
		}
	}

	private static class MockClass02 implements MockInterface {
		@Intercepted(MockInterceptor.class)
		public void method02() {
		}
	}

	@Remote
	@Intercepted(MockInterceptor.class)
	private static class MockClass03 {
		public void publicMethod03() {
		}

		private void privateMethod03() {
		}
	}

	@Controller
	@Intercepted(MockInterceptor.class)
	private static class MockClass04 {
		public void publicMethod04() {
		}

		private void privateMethod04() {
		}
	}

	@Service
	@Intercepted(MockInterceptor.class)
	private static class MockClass05 {
		public void publicMethod05() {
		}

		private void privateMethod05() {
		}
	}

	@Remote
	private static class MockClass06 {
		@Intercepted(MockInterceptor.class)
		public void method06() {
		}
	}

	@Controller
	private static class MockClass07 {
		@Intercepted(MockInterceptor.class)
		public void method07() {
		}
	}

	@Service
	private static class MockClass08 {
		@Intercepted(MockInterceptor.class)
		public void method08() {
		}
	}
}
