package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.util.Classes;

@SuppressWarnings("unused")
public class ManagedMethodSpiConformanceTest {
	private MockManagedClassSPI managedClass;
	private Method setterMethod;
	private Method getterMethod;
	private IManagedMethod setterManagedMethod;
	private IManagedMethod getterManagedMethod;

	@Before
	public void beforeTest() throws NoSuchMethodException, SecurityException {
		managedClass = new MockManagedClassSPI();
		setterMethod = Person.class.getDeclaredMethod("setInfo", String.class, Date.class, int.class);
		getterMethod = Person.class.getDeclaredMethod("getName");
		setterManagedMethod = Classes.newInstance("js.tiny.container.ManagedMethod", managedClass, setterMethod);
		getterManagedMethod = Classes.newInstance("js.tiny.container.ManagedMethod", managedClass, getterMethod);
	}

	@Test
	public void getDeclaringClass() throws NoSuchMethodException, SecurityException {
		assertEquals(managedClass, setterManagedMethod.getDeclaringClass());
		assertEquals(managedClass, getterManagedMethod.getDeclaringClass());
	}

	@Test
	public void getMethod() throws NoSuchMethodException, SecurityException {
		assertEquals(setterMethod, setterManagedMethod.getMethod());
		assertEquals(getterMethod, getterManagedMethod.getMethod());
	}

	@Test
	public void getParameterTypes() {
		assertEquals(0, getterManagedMethod.getParameterTypes().length);
		assertEquals(3, setterManagedMethod.getParameterTypes().length);
		assertEquals(String.class, setterManagedMethod.getParameterTypes()[0]);
		assertEquals(Date.class, setterManagedMethod.getParameterTypes()[1]);
		assertEquals(int.class, setterManagedMethod.getParameterTypes()[2]);
	}

	@Test
	public void getReturnType() {
		assertEquals(String.class, getterManagedMethod.getReturnType());
		assertEquals(void.class, setterManagedMethod.getReturnType());
	}

	@Test
	public void invoke() throws Exception {
		Person person = new Person();
		assertEquals("John Doe", getterManagedMethod.invoke(person));
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Person {
		private Object[] arguments;

		public void setInfo(String name, Date timestamp, int age) {
			arguments = new Object[] { name, timestamp, age };
		}

		public String getName() {
			arguments = new Object[] {};
			return "John Doe";
		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		@Override
		public IContainer getContainer() {
			return null;
		}

		@Override
		public Collection<IContainerService> getServices() {
			return Collections.emptySet();
		}

		@Override
		public Class<?> getImplementationClass() {
			return Person.class;
		}
	}
}
