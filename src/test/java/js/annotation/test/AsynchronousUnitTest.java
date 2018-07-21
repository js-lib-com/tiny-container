package js.annotation.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import js.annotation.Asynchronous;
import js.annotation.Controller;
import js.annotation.Remote;
import js.annotation.Service;
import js.annotation.Transactional;
import js.container.ContainerSPI;
import js.container.InstanceType;
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
public class AsynchronousUnitTest {
	/** Valid asynchronous method on managed class of {@link InstanceType#PROXY} type. */
	@Test
	public void asynchronousProxyType() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.AsynchronousUnitTest$MockInterface1' class='js.annotation.test.AsynchronousUnitTest$MockClass1' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method1", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	/** Annotation inherited from interface. */
	@Test
	public void asynchronousInherit() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.AsynchronousUnitTest$MockInterface2' class='js.annotation.test.AsynchronousUnitTest$MockClass2' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method2", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	/** Valid asynchronous method on managed class declared as remote accessible with {@link Remote} annotation. */
	@Test
	public void asynchronousRemoteAccessible() throws Throwable {
		String descriptor = "<test class='js.annotation.test.AsynchronousUnitTest$MockClass3' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method3", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	/** Valid asynchronous method on managed class declared as remote accessible with {@link Controller} annotation. */
	@Test
	public void asynchronousController() throws Throwable {
		String descriptor = "<test class='js.annotation.test.AsynchronousUnitTest$MockClass4' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method4", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	/**
	 * Valid asynchronous method on managed class declared as remote accessible with {@link Service} annotation. As side effect
	 * test also that asynchronous annotation on private method is ignored.
	 */
	@Test
	public void asynchronousService() throws Throwable {
		String descriptor = "<test class='js.annotation.test.AsynchronousUnitTest$MockClass5' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());
		assertEquals("method5", managedMethods.get(0).getMethod().getName());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	/** Asynchronous annotation on managed class of {@link InstanceType#REMOTE} type is ignored. */
	@Test
	public void asynchronousRemoteType() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.AsynchronousUnitTest$MockInterface2' type='REMOTE' url='http://localhost/' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(0, managedMethods.size());
	}

	/** Asynchronous annotation on managed class of {@link InstanceType#SERVICE} type is ignored. */
	@Test
	public void asynchronousServiceType() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.AsynchronousUnitTest$MockInterface2' type='SERVICE' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(0, managedMethods.size());
	}

	@Test(expected = BugError.class)
	public void asynchronousPojoLocal() throws Throwable {
		String descriptor = "<test class='js.annotation.test.AsynchronousUnitTest$MockClass1' />";
		ManagedClassSPI managedClass = managedClass(descriptor);
	}

	@Test(expected = BugError.class)
	public void asynchronousTransactional() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.AsynchronousUnitTest$MockInterface1' class='js.annotation.test.AsynchronousUnitTest$MockClass6' type='PROXY' />";
		ManagedClassSPI managedClass = managedClass(descriptor);
	}

	@Test(expected = BugError.class)
	public void asynchronousNonVoid() throws Throwable {
		String descriptor = "<test interface='js.annotation.test.AsynchronousUnitTest$MockInterface1' class='js.annotation.test.AsynchronousUnitTest$MockClass7' type='PROXY' />";
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

	private static interface MockInterface1 {
	}

	private static interface MockInterface2 {
		@Asynchronous
		void method2();
	}

	private static class MockClass1 implements MockInterface1 {
		@Asynchronous
		public void method1() {
		}
	}

	private static class MockClass2 implements MockInterface2 {
		public void method2() {
		}
	}

	@Remote
	private static class MockClass3 {
		@Asynchronous
		public void method3() {
		}
	}

	@Controller
	private static class MockClass4 {
		@Asynchronous
		public void method4() {
		}
	}

	@Service
	private static class MockClass5 {
		@Asynchronous
		public void method5() {
		}

		@Asynchronous
		private void privateMethod5() {
		}
	}

	@Transactional
	private static class MockClass6 implements MockInterface1 {
		@Asynchronous
		public void method6() {
		}
	}

	private static class MockClass7 implements MockInterface1 {
		@Asynchronous
		public int method7() {
			return 0;
		}
	}
}
