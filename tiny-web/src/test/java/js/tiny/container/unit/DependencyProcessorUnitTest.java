package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.junit.Test;

import js.lang.BugError;
import js.lang.InvocationException;
import js.tiny.container.core.IFactory;
import js.tiny.container.core.Container;
import js.tiny.container.core.InstanceScope;
import js.tiny.container.servlet.AppContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.stub.AppFactoryStub;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.util.Classes;

@SuppressWarnings({ "unused", "unchecked" })
public class DependencyProcessorUnitTest {
	/** Test getDependecyValue utility method does return correct instance without errors. */
	@Test
	public void getDependencyValue_ManagedClass() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		Object value = Classes.invoke(processorClass(), "getDependencyValue", managedClass, Person.class);

		assertNotNull(value);
		assertTrue(value instanceof Person);
	}

	/** AppFactory and its hierarchy cannot be managed class and dependency value is a special case. */
	@Test
	public void getDependencyValue_AppFactory() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);

		for (Class<?> clazz : new Class<?>[] { IFactory.class, AppContext.class, IContainer.class, Container.class }) {
			Object value = Classes.invoke(processorClass(), "getDependencyValue", managedClass, clazz);
			assertNotNull(value);
			assertTrue(value instanceof IFactory);
		}
	}

	@Test
	public void getDependencyValue_Pojo() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		Object value = Classes.invoke(processorClass(), "getDependencyValue", managedClass, Pojo.class);

		assertNotNull(value);
		assertTrue(value instanceof Pojo);
	}

	/** POJO without default constructor should throw bug error. */
	@Test(expected = BugError.class)
	public void getDependencyValue_PojoNoDefaultConstructor() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		Classes.invoke(processorClass(), "getDependencyValue", managedClass, NotDefaultPojo.class);
	}

	/** Get dependencies stack trace for current thread and ensure it remains empty after a number of execution. */
	@Test
	public void getDependencyValue_DependenciesStack() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		Class<?> processorClass = processorClass();
		ThreadLocal<Stack<Integer>> dependenciesStack = Classes.getFieldValue(processorClass, "dependenciesStack");
		assertNotNull(dependenciesStack);
		// ensure thread local stack is created
		getDependencyValue(Person.class);

		Stack<Integer> stackTrace = dependenciesStack.get();
		assertNotNull(stackTrace);

		for (int i = 0; i < 10000; ++i) {
			assertTrue(stackTrace.isEmpty());
			Classes.invoke(processorClass, "getDependencyValue", managedClass, Person.class);
			assertTrue(stackTrace.isEmpty());
		}
	}

	@Test
	public void circularDependencies() throws Exception {
		try {
			getDependencyValue(Circle.class);
			fail("Circular dependency should throw bug error.");
		} catch (InvocationException e) {
			assertTrue(e.getCause().getMessage().contains("Circular dependency"));
		}

		ThreadLocal<Stack<Integer>> dependenciesStack = Classes.getFieldValue(processorClass(), "dependenciesStack");
		assertNotNull(dependenciesStack);
		// thread related stack trace should be removed on circular dependency detected
		assertNull(dependenciesStack.get());
	}

	// rules for dependency injection with scope proxy
	// it is a set of host managed class scope to dependency scope pairs
	private static final Set<Map.Entry<InstanceScope, InstanceScope>> PROXY_SCOPES = new HashSet<>();
	static {
		PROXY_SCOPES.add(new AbstractMap.SimpleEntry<>(InstanceScope.APPLICATION, InstanceScope.SESSION));
		PROXY_SCOPES.add(new AbstractMap.SimpleEntry<>(InstanceScope.APPLICATION, InstanceScope.THREAD));
		PROXY_SCOPES.add(new AbstractMap.SimpleEntry<>(InstanceScope.THREAD, InstanceScope.SESSION));
		PROXY_SCOPES.add(new AbstractMap.SimpleEntry<>(InstanceScope.SESSION, InstanceScope.SESSION));
		PROXY_SCOPES.add(new AbstractMap.SimpleEntry<>(InstanceScope.LOCAL, InstanceScope.SESSION));
	}

	@Test
	public void isProxyRequired_Positive() throws Exception {
		MockManagedClassSPI hostClass = new MockManagedClassSPI(Person.class);
		MockManagedClassSPI dependencyClass = new MockManagedClassSPI(Person.class);
		for (Map.Entry<InstanceScope, InstanceScope> entry : PROXY_SCOPES) {
			hostClass.instanceScope = entry.getKey();
			dependencyClass.instanceScope = entry.getValue();
			assertTrue((boolean) Classes.invoke(processorClass(), "isProxyRequired", hostClass, dependencyClass));
		}
	}

	@Test
	public void isProxyRequired_Negative() throws Exception {
		InstanceScope[] scopes = new InstanceScope[] { InstanceScope.APPLICATION, InstanceScope.THREAD, InstanceScope.SESSION, InstanceScope.LOCAL };

		Set<Map.Entry<InstanceScope, InstanceScope>> noProxyScopes = new HashSet<>();
		for (int i = 0; i < scopes.length; ++i) {
			for (int j = 0; j < scopes.length; ++j) {
				Map.Entry<InstanceScope, InstanceScope> entry = new AbstractMap.SimpleEntry<>(scopes[i], scopes[j]);
				if (!PROXY_SCOPES.contains(entry)) {
					noProxyScopes.add(entry);
				}
			}
		}

		MockManagedClassSPI hostClass = new MockManagedClassSPI(Person.class);
		MockManagedClassSPI dependencyClass = new MockManagedClassSPI(Person.class);
		for (Map.Entry<InstanceScope, InstanceScope> entry : noProxyScopes) {
			hostClass.instanceScope = entry.getKey();
			dependencyClass.instanceScope = entry.getValue();
			assertFalse((boolean) Classes.invoke(processorClass(), "isProxyRequired", hostClass, dependencyClass));
		}
	}

	/**
	 * ScopeProxyHandler is used by dependency processor to adapt dependencies scopes. Scope proxy handler invoke
	 * {@link IFactory#getInstance(Class, Object...)} on every method invocation.
	 */
	@Test
	public void scopeProxyHandler() throws Throwable {
		class Human {
			String name;

			public void setName(String name) {
				this.name = name;
			}
		}

		class MockFactory extends MockContainer {
			Human person = new Human();

			@Override
			public <T> T getInstance(Class<? super T> interfaceClass) {
				assertEquals(Human.class, interfaceClass);
				return (T) person;
			}

		}

		MockFactory appFactory = new MockFactory();
		InvocationHandler scopeProxyHandler = Classes.newInstance("js.tiny.container.cdi.ScopeProxyHandler", appFactory, Human.class);

		Method method = Human.class.getDeclaredMethod("setName", String.class);
		method.setAccessible(true);
		scopeProxyHandler.invoke(new Object(), method, new Object[] { "John Doe" });

//		assertEquals("John Doe", appFactory.person.name);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static Class<?> processorClass() {
		return Classes.forName("js.tiny.container.cdi.DependencyLoader");
	}

	private static <T> T getDependencyValue(Class<T> type) throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		return Classes.invoke(processorClass(), "getDependencyValue", managedClass, type);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Person {
		private IFactory factory;
		private Vehicle car;
		private Pojo pojo;
	}

	private static interface Vehicle {
	}

	private static class Car implements Vehicle {
	}

	private static class Pojo {
	}

	private static class NotDefaultPojo {
		public NotDefaultPojo(int value) {
		}
	}

	private static class Circle {
		public Circle() throws Exception {
			getDependencyValue(Ellipse.class);
		}
	}

	private static class Ellipse {
		public Ellipse() throws Exception {
			getDependencyValue(Circle.class);
		}
	}

	private static class MockContainer extends ContainerStub {
		private Map<Class<?>, IManagedClass> classesPool = new HashMap<>();

		private void registerManagedClass(Class<?> interfaceClass, IManagedClass managedClass) {
			classesPool.put(interfaceClass, managedClass);
		}

		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
			IManagedClass managedClass = classesPool.get(interfaceClass);
			if (managedClass == null) {
				return null;
			}
			return (T) Classes.newInstance(managedClass.getImplementationClass());
		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private static MockContainer container = new MockContainer();
		private Class<?> implementationClass;
		private InstanceScope instanceScope = InstanceScope.APPLICATION;

		public MockManagedClassSPI(Class<?> type) throws Exception {
			this.implementationClass = type;
			container.registerManagedClass(type.getInterfaces().length == 1 ? type.getInterfaces()[0] : type, this);
		}

		@Override
		public IContainer getContainer() {
			return container;
		}

		@Override
		public Class<?> getImplementationClass() {
			return implementationClass;
		}

		@Override
		public InstanceScope getInstanceScope() {
			return instanceScope;
		}
	}
}
