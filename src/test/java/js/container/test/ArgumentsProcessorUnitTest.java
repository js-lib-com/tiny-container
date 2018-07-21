package js.container.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import js.container.ContainerSPI;
import js.container.ManagedClassSPI;
import js.core.AppFactory;
import js.lang.VarArgs;
import js.test.stub.ContainerStub;
import js.test.stub.ManagedClassSpiStub;
import js.test.stub.ManagedMethodSpiStub;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

public class ArgumentsProcessorUnitTest {
	private MockManagedClassSPI managedClass;
	private Object processor;

	@Before
	public void beforeTest() {
		managedClass = new MockManagedClassSPI();
		managedClass.implementationClass = Person.class;
		processor = getArgumentsProcessor();
	}

	@Test
	public void constructorArguments() throws Exception {
		managedClass.constructor = Person.class.getConstructor(String.class, int.class, boolean.class);
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>("John Doe", 54, false));

		assertNotNull(args);
		assertEquals(3, args.length);
		assertEquals("John Doe", args[0]);
		assertEquals(54, args[1]);
		assertFalse((boolean) args[2]);
	}

	@Test
	public void constructorArguments_NoArguments() throws Exception {
		managedClass.constructor = Person.class.getConstructor();
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>());

		assertNotNull(args);
		assertEquals(0, args.length);
	}

	@Test
	public void constructorArguments_DependencyInject() throws Exception {
		managedClass.constructor = Person.class.getConstructor(AppFactory.class, Vehicle.class, Pojo.class);
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>());

		assertNotNull(args);
		assertEquals(3, args.length);
		assertTrue(args[0] instanceof MockContainer);
		assertTrue(args[1] instanceof Vehicle);
		assertTrue(args[2] instanceof Pojo);
	}

	@Test
	public void constructorArguments_NoImplementation() throws Exception {
		managedClass.implementationClass = null;
		managedClass.constructor = Person.class.getConstructor(String.class, int.class, boolean.class);
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>("John Doe", 54, false));

		assertNotNull(args);
		assertEquals(3, args.length);
		assertEquals("John Doe", args[0]);
		assertEquals(54, args[1]);
		assertFalse((boolean) args[2]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorArguments_BadArgumentsCount() throws Exception {
		managedClass.constructor = Person.class.getConstructor(String.class, int.class, boolean.class);
		Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>("John Doe"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorArguments_InvalidArgumentType() throws Exception {
		managedClass.constructor = Person.class.getConstructor(String.class, int.class, boolean.class);
		Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>("John Doe", true));
	}

	@Test
	public void constructorArguments_NullArgument() throws Exception {
		managedClass.constructor = Person.class.getConstructor(String.class, int.class, boolean.class);
		Classes.invoke(processor, "preProcessArguments", managedClass, new VarArgs<Object>("John Doe", 54, null));
	}

	@Test
	public void constructorArguments_NullArgumentsArray() throws Exception {
		managedClass.constructor = Person.class.getConstructor();
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedClass, null);
		assertNotNull(args);
		assertEquals(0, args.length);
	}

	@Test
	public void methodArguments() throws Exception {
		MockManagedMethodSPI managedMethod = new MockManagedMethodSPI();
		managedMethod.declaringClass = managedClass;
		managedMethod.method = Person.class.getMethod("setValues", String.class, int.class, boolean.class);
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedMethod, new VarArgs<Object>("John Doe", 54, false));

		assertNotNull(args);
		assertEquals(3, args.length);
		assertEquals("John Doe", args[0]);
		assertEquals(54, args[1]);
		assertFalse((boolean) args[2]);
	}

	@Test
	public void methodArguments_NullArguments() throws Exception {
		MockManagedMethodSPI managedMethod = new MockManagedMethodSPI();
		managedMethod.declaringClass = managedClass;
		managedMethod.method = Person.class.getMethod("setValue");
		Object[] args = Classes.invoke(processor, "preProcessArguments", managedMethod, null);
		assertNotNull(args);
		assertEquals(0, args.length);
	}

	private static Object getArgumentsProcessor() {
		return Classes.newInstance("js.container.ArgumentsProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@SuppressWarnings("unused")
	private static class Person {
		public Person() {
		}

		public Person(String name, int age, boolean married) {
		}

		public Person(AppFactory factory, Vehicle car, Pojo pojo) {
		}

		public void setValue() {
		}

		public void setValues(String name, int age, boolean married) {
		}
	}

	private static interface Vehicle {
	}

	private static class Car implements Vehicle {
	}

	private static class Pojo {
	}

	private static class MockContainer extends ContainerStub {
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
			if (interfaceClass.equals(Vehicle.class)) {
				return (T) new Car();
			}
			return null;
		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private static MockContainer container = new MockContainer();
		private Class<?> implementationClass;
		private Constructor<?> constructor;

		@Override
		public ContainerSPI getContainer() {
			return container;
		}

		@Override
		public Class<?> getImplementationClass() {
			return implementationClass;
		}

		@Override
		public Constructor<?> getConstructor() {
			return constructor;
		}
	}

	private static class MockManagedMethodSPI extends ManagedMethodSpiStub {
		private MockManagedClassSPI declaringClass;
		private Method method;

		@Override
		public ManagedClassSPI getDeclaringClass() {
			return declaringClass;
		}

		@Override
		public Method getMethod() {
			return method;
		}
	}
}
