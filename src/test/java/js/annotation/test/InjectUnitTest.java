package js.annotation.test;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import js.annotation.Inject;
import js.container.ContainerSPI;
import js.container.ManagedClassSPI;
import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.InvocationException;
import js.test.stub.ContainerStub;
import js.util.Classes;
import js.util.Strings;

import org.junit.Test;

@SuppressWarnings("unused")
public class InjectUnitTest {
	@Test
	public void fieldInject() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InjectUnitTest$MockClass1' />";
		ManagedClassSPI managedClass = managedClass(descriptor);

		List<Field> dependencies = list(managedClass.getDependencies());
		assertEquals(1, dependencies.size());
		assertEquals("object1_1", dependencies.get(0).getName());
	}

	/** Using {@link Inject} annotation on final field should rise big error. */
	@Test(expected = BugError.class)
	public void finalFieldInject() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InjectUnitTest$MockClass2' />";
		managedClass(descriptor);
	}

	/** Using {@link Inject} annotation on static field should rise big error. */
	@Test(expected = BugError.class)
	public void staticFieldInject() throws Throwable {
		String descriptor = "<test class='js.annotation.test.InjectUnitTest$MockClass3' />";
		managedClass(descriptor);
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
