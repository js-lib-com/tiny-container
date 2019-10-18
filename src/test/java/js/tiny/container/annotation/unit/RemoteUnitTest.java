package js.tiny.container.annotation.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.InvocationException;
import js.tiny.container.ManagedClass;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.annotation.Controller;
import js.tiny.container.annotation.Local;
import js.tiny.container.annotation.Private;
import js.tiny.container.annotation.Public;
import js.tiny.container.annotation.Remote;
import js.tiny.container.annotation.RequestPath;
import js.tiny.container.annotation.Service;
import js.tiny.container.stub.ContainerStub;
import js.util.Strings;

@SuppressWarnings("unused")
public class RemoteUnitTest {
	@Test
	public void remoteClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass01' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("managedMethod01", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("managed-method-0-1", managedMethod.getRequestPath());
	}

	@Test
	public void remoteClassWithClassPath() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass15' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertEquals("mock", managedClass.getRequestPath());
	}

	@Test
	public void remoteMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass02' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method02", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("method-0-2", managedMethod.getRequestPath());
	}

	@Test
	public void pathMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass17' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method/path", managedMethod.getRequestPath());
	}

	@Test
	public void localMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass03' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());
		assertFalse(managedClass.getManagedMethods().iterator().hasNext());
	}

	@Test(expected = BugError.class)
	public void localMethodOnNotRemote() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass04' />";
		managedClass(descriptor);
	}

	@Test(expected = BugError.class)
	public void methodOverloading() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass16' />";
		managedClass(descriptor);
	}

	@Test
	public void remoteClassInheritance() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.RemoteUnitTest$MockInterface01' class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass05' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method05", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("method-0-5", managedMethod.getRequestPath());
	}

	@Test
	public void remoteMethodInheritance() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.RemoteUnitTest$MockInterface02' class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass06' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method02", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("method-0-2", managedMethod.getRequestPath());
	}

	@Test
	public void controllerClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass07' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method07", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("method-0-7", managedMethod.getRequestPath());
	}

	@Test
	public void serviceClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass08' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method08", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("method-0-8", managedMethod.getRequestPath());
	}

	@Test
	public void remotePublicClass() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass09' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method09", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertTrue(managedMethod.isPublic());
		assertEquals("method-0-9", managedMethod.getRequestPath());
	}

	@Test
	public void remotePublicMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass10' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method10", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertTrue(managedMethod.isPublic());
		assertEquals("method-1-0", managedMethod.getRequestPath());
	}

	@Test
	public void remoteClassPrivateMethod() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass11' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		assertTrue(managedClass.isRemotelyAccessible());
		assertNull(managedClass.getRequestPath());

		List<ManagedMethodSPI> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		ManagedMethodSPI managedMethod = managedMethods.get(0);
		assertEquals("method11", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isRemotelyAccessible());
		assertFalse(managedMethod.isPublic());
		assertEquals("method-1-1", managedMethod.getRequestPath());
	}

	@Test(expected = BugError.class)
	public void publicMethodOnNotRemote() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass12' />";
		managedClass(descriptor);
	}

	@Test(expected = BugError.class)
	public void privateMethodOnNotRemote() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass13' />";
		managedClass(descriptor);
	}

	@Test(expected = BugError.class)
	public void pathMethodOnNotRemote() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.RemoteUnitTest$MockClass14' />";
		managedClass(descriptor);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static ManagedClassSPI managedClass(String descriptor) throws Throwable {
		MockContainer container = new MockContainer();
		return managedClass(container, descriptor);
	}

	private static ManagedClassSPI managedClass(MockContainer container, String descriptor) throws Throwable {
		String xml = Strings.concat("<?xml version='1.0' encoding='UTF-8' ?><managed-classes>", descriptor, "</managed-classes>");
		ConfigBuilder builder = new ConfigBuilder(xml);
		try {
			return new ManagedClass(container, builder.build().getChild("test"));
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

	@Remote
	private static interface MockInterface01 {
	}

	private static interface MockInterface02 {
		@Remote
		void method02();
	}

	@Remote
	private static class MockClass01 {
		public void managedMethod01() {
		}

		private void localMethod01() {
		}
	}

	private static class MockClass02 {
		@Remote
		public void method02() {
		}
	}

	@Remote
	private static class MockClass03 {
		@Local
		public void method03() {
		}
	}

	private static class MockClass04 {
		@Local
		public void method04() {
		}
	}

	private static class MockClass05 implements MockInterface01 {
		public void method05() {
		}
	}

	private static class MockClass06 implements MockInterface02 {
		public void method02() {
		}
	}

	@Controller
	private static class MockClass07 {
		public void method07() {
		}
	}

	@Service
	private static class MockClass08 {
		public void method08() {
		}
	}

	@Remote
	@Public
	private static class MockClass09 {
		public void method09() {
		}
	}

	@Remote
	private static class MockClass10 {
		@Public
		public void method10() {
		}
	}

	@Remote
	@Public
	private static class MockClass11 {
		@Private
		public void method11() {
		}
	}

	private static class MockClass12 {
		@Public
		public void method12() {
		}
	}

	private static class MockClass13 {
		@Private
		public void method13() {
		}
	}

	private static class MockClass14 {
		@RequestPath("method")
		public void method14() {
		}
	}

	@Service("mock")
	private static class MockClass15 {
	}

	@Remote
	private static class MockClass16 {
		public void method16(File file) {
		}

		public void method16(URL url) {
		}
	}

	@Remote
	private static class MockClass17 {
		@RequestPath("method/path")
		public void method17() {
		}
	}

	private static class MockContainer extends ContainerStub {

	}
}
