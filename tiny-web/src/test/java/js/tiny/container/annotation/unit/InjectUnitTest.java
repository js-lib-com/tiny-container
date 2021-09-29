package js.tiny.container.annotation.unit;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.InvocationException;
import js.tiny.container.Container;
import js.tiny.container.ManagedClass;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.stub.ContainerStub;
import js.util.Strings;

@SuppressWarnings("unused")
public class InjectUnitTest {
	@Test
	public void fieldInject() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InjectUnitTest$MockClass1' />";
		IManagedClass managedClass = managedClass(descriptor);

		List<Field> dependencies = list(managedClass.getDependencies());
		assertEquals(1, dependencies.size());
		assertEquals("object1_1", dependencies.get(0).getName());
	}

	/** Using {@link Inject} annotation on final field should rise big error. */
	@Test(expected = BugError.class)
	public void finalFieldInject() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InjectUnitTest$MockClass2' />";
		managedClass(descriptor);
	}

	/** Using {@link Inject} annotation on static field should rise big error. */
	@Test(expected = BugError.class)
	public void staticFieldInject() throws Throwable {
		String descriptor = "<test class='js.tiny.container.annotation.unit.InjectUnitTest$MockClass3' />";
		managedClass(descriptor);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static IManagedClass managedClass(String descriptor) throws Throwable {
		Container container = new ContainerStub();
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

	private static class MockClass1 {
		@Inject
		private Object object1_1;
		private Object object1_2;
	}

	private static class MockClass2 {
		@Inject
		private final Object object2 = null;
	}

	private static class MockClass3 {
		@Inject
		private static Object object3;
	}
}
