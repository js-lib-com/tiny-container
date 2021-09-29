package js.tiny.container.annotation.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.tiny.container.Container;
import js.tiny.container.InstanceType;
import js.tiny.container.ManagedClass;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ContainerStub;
import js.transaction.Immutable;
import js.transaction.Mutable;
import js.transaction.Transactional;
import js.util.Strings;

@SuppressWarnings("unused")
public class TransactionalUnitTest {
	/** Transactional annotated class is delegated to all public methods. */
	@Test
	public void transactionalClass() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass1' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method1", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertFalse(managedMethod.isImmutable());
	}

	/** Transactional annotated method. */
	@Test
	public void transactionalMethod() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass2' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method2", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertFalse(managedMethod.isImmutable());
	}

	/** Transactional class inherited from interface. */
	@Test
	public void transactionalClassInherit() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface2' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass3' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method2", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertFalse(managedMethod.isImmutable());
	}

	/** Transational method inherited from interface. */
	@Test
	public void transactionalMethodInherit() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface3' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass4' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method3", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertFalse(managedMethod.isImmutable());
	}

	/** Class transactional annotation on {@link InstanceType#POJO} should throw bug error. */
	@Test(expected = BugError.class)
	public void transactionalClassPOJO() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass1' />";
		managedClass(descriptor);
	}

	/** Method transactional annotation on {@link InstanceType#POJO} should throw bug error. */
	@Test(expected = BugError.class)
	public void transactionalMethodPOJO() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass2' />";
		managedClass(descriptor);
	}

	/** Transactional annotation on protected or private method is silently ignored. */
	@Test
	public void privateTransactionalMethod() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass5' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertFalse(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(0, managedMethods.size());
	}

	@Test
	public void immutableTransactionalClass() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass6' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method6", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertTrue(managedMethod.isImmutable());
	}

	@Test
	public void immutableTransactionalMethod() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass7' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method7", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertTrue(managedMethod.isImmutable());
	}

	@Test
	public void mutableMethodOverwrite() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass8' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
		assertTrue(managedClass.isTransactional());

		List<IManagedMethod> managedMethods = list(managedClass.getManagedMethods());
		assertEquals(1, managedMethods.size());

		IManagedMethod managedMethod = managedMethods.get(0);
		assertEquals("method8", managedMethod.getMethod().getName());
		assertTrue(managedMethod.isTransactional());
		assertFalse(managedMethod.isImmutable());
	}

	@Test(expected = BugError.class)
	public void immutableNonTransactionalClass() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass9' type='PROXY' />";
		IManagedClass managedClass = managedClass(descriptor);
	}

	/** Immutable annotation on not transactional method is ignored and should not rise exception. */
	@Test
	public void immutableNonTransactionalMethod() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass10' type='PROXY' />";
		managedClass(descriptor);
	}

	/** Mutable annotation on not transactional method is ignored and should not rise exception. */
	@Test
	public void mutableNonTransactionalMethod() throws Throwable {
		String descriptor = "<test interface='js.tiny.container.annotation.unit.TransactionalUnitTest$MockInterface1' class='js.tiny.container.annotation.unit.TransactionalUnitTest$MockClass11' type='PROXY' />";
		managedClass(descriptor);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static IManagedClass managedClass(String descriptor) throws Throwable {
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

	private static interface MockInterface1 {
	}

	@Transactional
	private static interface MockInterface2 {
		void method2();
	}

	private static interface MockInterface3 {
		@Transactional
		void method3();
	}

	@Transactional
	private static class MockClass1 implements MockInterface1 {
		public void method1() {
		}
	}

	private static class MockClass2 implements MockInterface1 {
		@Transactional
		public void method2() {
		}
	}

	private static class MockClass3 implements MockInterface2 {
		public void method2() {
		}
	}

	private static class MockClass4 implements MockInterface3 {
		public void method3() {
		}
	}

	private static class MockClass5 implements MockInterface1 {
		@Transactional
		private void privateMethod5() {

		}

		@Transactional
		protected void protectedMethod5() {
		}
	}

	@Transactional
	@Immutable
	private static class MockClass6 implements MockInterface1 {
		public void method6() {
		}
	}

	@Transactional
	private static class MockClass7 implements MockInterface1 {
		@Immutable
		public void method7() {
		}
	}

	@Transactional
	@Immutable
	private static class MockClass8 implements MockInterface1 {
		@Mutable
		public void method8() {
		}
	}

	@Immutable
	private static class MockClass9 implements MockInterface1 {
		public void method9() {
		}
	}

	private static class MockClass10 implements MockInterface1 {
		@Immutable
		public void method10() {
		}
	}

	private static class MockClass11 implements MockInterface1 {
		@Mutable
		public void method11() {
		}
	}
}
