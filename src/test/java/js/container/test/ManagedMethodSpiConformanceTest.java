package js.container.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Date;

import js.container.Access;
import js.container.ContainerSPI;
import js.container.ManagedMethodSPI;
import js.test.stub.ManagedClassSpiStub;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class ManagedMethodSpiConformanceTest {
	private MockManagedClassSPI managedClass;
	private Method setterMethod;
	private Method getterMethod;
	private ManagedMethodSPI setterManagedMethod;
	private ManagedMethodSPI getterManagedMethod;

	@Before
	public void beforeTest() throws NoSuchMethodException, SecurityException {
		managedClass = new MockManagedClassSPI();
		setterMethod = Person.class.getDeclaredMethod("setInfo", String.class, Date.class, int.class);
		getterMethod = Person.class.getDeclaredMethod("getName");
		setterManagedMethod = Classes.newInstance("js.container.ManagedMethod", managedClass, setterMethod);
		getterManagedMethod = Classes.newInstance("js.container.ManagedMethod", managedClass, getterMethod);
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

	@Test
	public void getRequestPath() throws Exception {
		Classes.invoke(getterManagedMethod, "setRemotelyAccessible", true);
		Classes.invoke(getterManagedMethod, "setRequestPath", "method/path");
		assertEquals("method/path", getterManagedMethod.getRequestPath());
	}

	@Test
	public void isVoid() {
		assertFalse(getterManagedMethod.isVoid());
		assertTrue(setterManagedMethod.isVoid());
	}

	@Test
	public void isRemotelyAccessible() throws Exception {
		assertFalse(getterManagedMethod.isRemotelyAccessible());
		Classes.invoke(getterManagedMethod, "setRemotelyAccessible", true);
		assertTrue(getterManagedMethod.isRemotelyAccessible());
	}

	@Test
	public void isPublic() throws Exception {
		assertFalse(getterManagedMethod.isPublic());
		Classes.invoke(getterManagedMethod, "setAccess", Access.PUBLIC);
		assertTrue(getterManagedMethod.isPublic());
	}

	@Test
	public void isTransactional() throws Exception {
		assertFalse(getterManagedMethod.isTransactional());
		Classes.invoke(getterManagedMethod, "setTransactional", true);
		assertTrue(getterManagedMethod.isTransactional());
	}

	@Test
	public void isImmutable() throws Exception {
		assertFalse(getterManagedMethod.isImmutable());
		Classes.invoke(getterManagedMethod, "setImmutable", true);
		assertTrue(getterManagedMethod.isImmutable());
	}

	@Test
	public void isAsynchronous() throws Exception {
		assertFalse(getterManagedMethod.isAsynchronous());
		assertEquals("DefaultInvoker", Classes.getFieldValue(getterManagedMethod, "invoker").getClass().getSimpleName());
		Classes.invoke(getterManagedMethod, "setAsynchronous", true);
		assertTrue(getterManagedMethod.isAsynchronous());
		assertEquals("AsyncInvoker", Classes.getFieldValue(getterManagedMethod, "invoker").getClass().getSimpleName());
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
		public ContainerSPI getContainer() {
			return null;
		}

		@Override
		public Class<?> getImplementationClass() {
			return Person.class;
		}
	}
}
