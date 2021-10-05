package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.InvocationException;
import js.lang.ManagedPostConstruct;
import js.tiny.container.Container;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;
import js.tiny.container.core.AppFactory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePostProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.transaction.TransactionManager;
import js.util.Classes;
import js.util.Types;

@SuppressWarnings("unused")
public class InstanceProcessorUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	/** Ensure built-in instance processors are registered into the proper order. */
	@Test
	public void containerRegistration() throws Exception {
		Object container = TestContext.start();
		List<IInstancePostProcessor> processors = Classes.getFieldValue(container, Container.class, "instanceProcessors");

		assertNotNull(processors);
		assertEquals(7, processors.size());
		assertEquals("CalendarTimerService", processors.get(0).getClass().getSimpleName());
		assertEquals("ContextParamProcessor", processors.get(1).getClass().getSimpleName());
		assertEquals("InstanceFieldsInjectionProcessor", processors.get(2).getClass().getSimpleName());
		assertEquals("InstanceFieldsInitializationProcessor", processors.get(3).getClass().getSimpleName());
		assertEquals("ConfigurableInstanceProcessor", processors.get(4).getClass().getSimpleName());
		assertEquals("PostConstructInstanceProcessor", processors.get(5).getClass().getSimpleName());
		assertEquals("LoggerInstanceProcessor", processors.get(6).getClass().getSimpleName());
	}

	// --------------------------------------------------------------------------------------------
	// INSTANCE FIELDS INJECTION PROCESSOR

	@Test
	public void instanceFieldsInjection() {
		MockManagedClassSPI hostClass = new MockManagedClassSPI(Person.class);
		MockManagedClassSPI carClass = new MockManagedClassSPI(Car.class);
		IInstancePostProcessor processor = getInstanceFieldsInjectionProcessor();

		Person person = new Person();
		processor.postProcessInstance(hostClass, person);

		assertNotNull(person.factory);
		assertTrue(person.factory instanceof MockContainer);
		assertNotNull(person.car);
		assertFalse(Proxy.isProxyClass(person.car.getClass()));
		assertTrue(person.car instanceof Vehicle);
		assertNotNull(person.pojo);
		assertFalse(Proxy.isProxyClass(person.pojo.getClass()));
	}

	@Test
	public void instanceFieldsInjection_Session() {
		MockManagedClassSPI hostClass = new MockManagedClassSPI(Person.class);
		MockManagedClassSPI carClass = new MockManagedClassSPI(Car.class);
		carClass.instanceScope = InstanceScope.SESSION;
		IInstancePostProcessor processor = getInstanceFieldsInjectionProcessor();

		Person person = new Person();
		processor.postProcessInstance(hostClass, person);

		assertNotNull(person.factory);
		assertTrue(person.factory instanceof MockContainer);
		assertNotNull(person.car);
		assertTrue(Proxy.isProxyClass(person.car.getClass()));
		assertTrue(person.car instanceof Vehicle);
		assertNotNull(person.pojo);
		assertFalse(Proxy.isProxyClass(person.pojo.getClass()));
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void instanceFieldsInjection_NullInstance() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		IInstancePostProcessor processor = getInstanceFieldsInjectionProcessor();
		processor.postProcessInstance(managedClass, null);
	}

	private static IInstancePostProcessor getInstanceFieldsInjectionProcessor() {
		return Classes.newInstance("js.tiny.container.InstanceFieldsInjectionProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// INSTANCE FIELDS INITIALIZATION PROCESSOR

	@Test
	public void instanceFieldsInitialization() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='name' value='John Doe' />" + //
				"	<instance-field name='age' value='54' />" + //
				"	<instance-field name='maried' value='false' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI managedClass = new MockManagedClassSPI(PersonFields.class);
		managedClass.config = builder.build();
		IInstancePostProcessor processor = getInstanceFieldsInitializationProcessor();

		PersonFields person = new PersonFields();
		processor.postProcessInstance(managedClass, person);

		assertNotNull(person);
		assertEquals("John Doe", person.name);
		assertEquals(54, person.age);
		assertFalse(person.maried);
	}

	@Test
	public void instanceFieldsInitialization_NoConfig() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(PersonFields.class);
		IInstancePostProcessor processor = getInstanceFieldsInitializationProcessor();

		PersonFields person = new PersonFields();
		processor.postProcessInstance(managedClass, person);

		assertNotNull(person);
		assertNull(person.name);
		assertEquals(0, person.age);
		assertFalse(person.maried);
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void instanceFieldsInitialization_NullInstance() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		IInstancePostProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.postProcessInstance(managedClass, null);
	}

	/** Assigning a non-numerical string to an integer field should throw converter exception. */
	@Test(expected = ConverterException.class)
	public void instanceFieldsInitialization_ConverterException() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='age' value='John Doe' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI managedClass = new MockManagedClassSPI(PersonFields.class);
		managedClass.config = builder.build();
		IInstancePostProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.postProcessInstance(managedClass, new PersonFields());
	}

	/** Attempting to initialize a field of a type for which there is no converter should rise converter exception. */
	@Test(expected = ConverterException.class)
	public void instanceFieldsInitialization_NotValueTypeField() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='pojo' value='John Doe' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.config = builder.build();
		IInstancePostProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.postProcessInstance(managedClass, new Person());
	}

	@Test(expected = BugError.class)
	public void instanceFieldsInitialization_NotPOJO() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='name' value='John Doe' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI managedClass = new MockManagedClassSPI(PersonFields.class);
		managedClass.config = builder.build();
		managedClass.instanceType = InstanceType.SERVICE;
		IInstancePostProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.postProcessInstance(managedClass, new PersonFields());
	}

	private static IInstancePostProcessor getInstanceFieldsInitializationProcessor() {
		return Classes.newInstance("js.tiny.container.InstanceFieldsInitializationProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// CONFIGURABLE INSTANCE PROCESSOR

	@Test
	public void configurable() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.config = new Config("test");
		IInstancePostProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		processor.postProcessInstance(managedClass, joker);

		assertNotNull(joker.config);
		assertEquals("test", joker.config.getName());
	}

	/** Configuration processor on instance without Configurable interface should do nothing and throw none. */
	@Test
	public void configurable_NoConfigurable() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.config = new Config("test");
		IInstancePostProcessor processor = getConfigurableInstanceProcessor();

		Person person = new Person();
		processor.postProcessInstance(managedClass, person);
	}

	/** Managed class without configuration object should not execute instance configuration. */
	@Test
	public void configurable_NoConfigObject() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		IInstancePostProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		processor.postProcessInstance(managedClass, joker);

		assertNull(joker.config);
	}

	/** Invalid configuration object should throw bug error. */
	@Test(expected = BugError.class)
	public void configurable_Invalid() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.config = new Config("test");
		IInstancePostProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		joker.invalid = true;
		processor.postProcessInstance(managedClass, joker);
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void configurable_NullInstance() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.config = new Config("test");
		IInstancePostProcessor processor = getConfigurableInstanceProcessor();
		processor.postProcessInstance(managedClass, null);
	}

	/** Exception on configuration execution should throw bug error. */
	@Test(expected = BugError.class)
	public void configurable_Exception() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.config = new Config("test");
		IInstancePostProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		joker.exception = true;
		processor.postProcessInstance(managedClass, joker);
	}

	private static IInstancePostProcessor getConfigurableInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.ConfigurableInstanceProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// POST_CONSTRUCT INSTANCE PROCESSOR

	@Test
	public void postConstruct() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Joker.class);
		IInstancePostProcessor processor = getPostConstructInstanceProcessor();

		Joker joker = new Joker();
		processor.postProcessInstance(managedClass, joker);

		assertEquals(1, joker.postConstructProbe);
	}

	@Test
	public void postConstruct_NoManagedPostConstruct() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		IInstancePostProcessor processor = getPostConstructInstanceProcessor();

		Person person = new Person();
		processor.postProcessInstance(managedClass, person);
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void postConstruct_NullInstance() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		IInstancePostProcessor processor = getPostConstructInstanceProcessor();
		processor.postProcessInstance(managedClass, null);
	}

	@Test(expected = BugError.class)
	public void postConstruct_Exception() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Joker.class);
		IInstancePostProcessor processor = getPostConstructInstanceProcessor();

		Joker joker = new Joker();
		joker.exception = true;
		processor.postProcessInstance(managedClass, joker);
	}

	private static IInstancePostProcessor getPostConstructInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.PostConstructInstanceProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// LOGGER INSTANCE PROCESSOR

	@Test
	public void logger() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.instanceScope = InstanceScope.APPLICATION;
		managedClass.instanceType = InstanceType.PROXY;
		IInstancePostProcessor processor = getLoggerInstanceProcessor();

		Person person = new Person();
		processor.postProcessInstance(managedClass, person);
	}

	@Test
	public void logger_MultipleInterfaces() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Joker.class);
		managedClass.instanceScope = InstanceScope.APPLICATION;
		managedClass.instanceType = InstanceType.PROXY;
		IInstancePostProcessor processor = getLoggerInstanceProcessor();

		Joker joker = new Joker();
		processor.postProcessInstance(managedClass, joker);
	}

	@Test
	public void logger_NoDump() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI(Person.class);
		managedClass.instanceScope = InstanceScope.LOCAL;
		managedClass.instanceType = InstanceType.POJO;
		IInstancePostProcessor processor = getLoggerInstanceProcessor();

		Person person = new Person();
		processor.postProcessInstance(managedClass, person);
	}

	private static IInstancePostProcessor getLoggerInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.LoggerInstanceProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Person {
		private AppFactory factory;
		private Vehicle car;
		private Pojo pojo;
	}

	private static class PersonFields {
		private String name;
		private int age;
		private boolean maried;
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

	private static class Joker implements Configurable, ManagedPostConstruct {
		private Config config;
		private int postConstructProbe;
		private boolean invalid;
		private boolean exception;

		@Override
		public void config(Config config) throws Exception {
			if (invalid) {
				throw new ConfigException("invalid");
			}
			if (exception) {
				throw new IOException("exception");
			}
			this.config = config;
		}

		@Override
		public void postConstruct() throws Exception {
			if (exception) {
				throw new IOException("exception");
			}
			++postConstructProbe;
		}

		public static Method getPostConstructMethod() {
			try {
				return Joker.class.getMethod("postConstruct");
			} catch (NoSuchMethodException | SecurityException e) {
				throw new BugError(e);
			}
		}
	}

	private static class MockContainer extends ContainerStub {
		private Map<Class<?>, IManagedClass> classesPool = new HashMap<>();

		private void registerManagedClass(Class<?> interfaceClass, IManagedClass managedClass) {
			classesPool.put(interfaceClass, managedClass);
		}

		@Override
		public IManagedClass getManagedClass(Class<?> interfaceClass) {
			return classesPool.get(interfaceClass);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
			if (interfaceClass.getSimpleName().equals("TransactionalResource")) {
				Class<?> clazz = Classes.forName("js.container.TransactionalResourceImpl");
				return (T) Classes.newInstance(clazz, this);
			}
			throw new UnsupportedOperationException("getInstance(Class<? super T>, Object...)");
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
			if (Types.isKindOf(interfaceClass, TransactionManager.class)) {
				return (T) Classes.loadService(interfaceClass);
			}
			IManagedClass managedClass = classesPool.get(interfaceClass);
			if (managedClass == null) {
				return null;
			}
			return (T) Classes.newInstance(managedClass.getImplementationClass());
		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private static MockContainer container = new MockContainer();
		private Config config;
		private List<Field> dependencies = new ArrayList<>();
		private Class<?>[] interfaceClasses;
		private Class<?> implementationClass;
		private InstanceScope instanceScope = InstanceScope.APPLICATION;
		private InstanceType instanceType = InstanceType.POJO;
		private boolean transactional;

		public MockManagedClassSPI(Class<?> type) {
			this.interfaceClasses = type.getInterfaces();
			if (this.interfaceClasses.length == 0) {
				this.interfaceClasses = new Class<?>[] { type };
			}
			this.implementationClass = type;
			container.registerManagedClass(this.interfaceClasses[0], this);
			for (Field field : type.getDeclaredFields()) {
				dependencies.add(field);
			}
		}

		@Override
		public IContainer getContainer() {
			return container;
		}

		@Override
		public Config getConfig() {
			return config;
		}

		@Override
		public Class<?>[] getInterfaceClasses() {
			return interfaceClasses;
		}

		@Override
		public Class<?> getImplementationClass() {
			return implementationClass;
		}

		@Override
		public Iterable<Field> getDependencies() {
			return dependencies;
		}

		@Override
		public InstanceScope getInstanceScope() {
			return instanceScope;
		}

		@Override
		public InstanceType getInstanceType() {
			return instanceType;
		}

		@Override
		public boolean isRemotelyAccessible() {
			return false;
		}

		@Override
		public IManagedMethod getPostConstructMethod() {
			return implementationClass.equals(Joker.class) ? new MockManagedMethodSPI(Joker.getPostConstructMethod()) : null;
		}

		@Override
		public IManagedMethod getPreDestroyMethod() {
			return null;
		}
	}

	private static class MockManagedMethodSPI extends ManagedMethodSpiStub {
		private final Method method;;

		public MockManagedMethodSPI(Method method) {
			super();
			this.method = method;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T invoke(Object object, Object... arguments) throws IllegalArgumentException, InvocationException, AuthorizationException {
			try {
				return (T) method.invoke(object, arguments);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new BugError(e);
			}
		}
	}
}
