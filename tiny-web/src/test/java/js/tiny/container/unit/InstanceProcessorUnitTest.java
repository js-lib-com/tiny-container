package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.InvocationException;
import js.lang.ManagedPostConstruct;
import js.tiny.container.core.Container;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IFactory;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.util.Classes;

@SuppressWarnings("unused")
public class InstanceProcessorUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	/** Ensure built-in instance processors are registered into the proper order. */
	@Test
	@Ignore
	public void containerRegistration() throws Exception {
		Object container = TestContext.start();
		List<IInstancePostConstructionProcessor> processors = Classes.getFieldValue(container, Container.class, "instancePostConstructProcessors");

		assertNotNull(processors);
		assertEquals("InstanceFieldsInjectionProcessor", processors.get(0).getClass().getSimpleName());
		assertEquals("InstanceFieldsInitializationProcessor", processors.get(1).getClass().getSimpleName());
		assertEquals("ConfigurableInstanceProcessor", processors.get(2).getClass().getSimpleName());
		assertEquals("PostConstructInstanceProcessor", processors.get(3).getClass().getSimpleName());
		assertEquals("LoggerInstanceProcessor", processors.get(4).getClass().getSimpleName());
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

		MockManagedClassSPI<PersonFields> managedClass = new MockManagedClassSPI<>(PersonFields.class);
		managedClass.config = builder.build();
		IInstancePostConstructionProcessor processor = getInstanceFieldsInitializationProcessor();

		PersonFields person = new PersonFields();
		processor.onInstancePostConstruction(managedClass, person);

		assertNotNull(person);
		assertEquals("John Doe", person.name);
		assertEquals(54, person.age);
		assertFalse(person.maried);
	}

	@Test
	public void instanceFieldsInitialization_NoConfig() {
		MockManagedClassSPI<PersonFields> managedClass = new MockManagedClassSPI<>(PersonFields.class);
		IInstancePostConstructionProcessor processor = getInstanceFieldsInitializationProcessor();

		PersonFields person = new PersonFields();
		processor.onInstancePostConstruction(managedClass, person);

		assertNotNull(person);
		assertNull(person.name);
		assertEquals(0, person.age);
		assertFalse(person.maried);
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void instanceFieldsInitialization_NullInstance() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		IInstancePostConstructionProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.onInstancePostConstruction(managedClass, null);
	}

	/** Assigning a non-numerical string to an integer field should throw converter exception. */
	@Test(expected = ConverterException.class)
	public void instanceFieldsInitialization_ConverterException() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='age' value='John Doe' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI<PersonFields> managedClass = new MockManagedClassSPI<>(PersonFields.class);
		managedClass.config = builder.build();
		IInstancePostConstructionProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.onInstancePostConstruction(managedClass, new PersonFields());
	}

	/** Attempting to initialize a field of a type for which there is no converter should rise converter exception. */
	@Test(expected = ConverterException.class)
	public void instanceFieldsInitialization_NotValueTypeField() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='pojo' value='John Doe' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.config = builder.build();
		IInstancePostConstructionProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.onInstancePostConstruction(managedClass, new Person());
	}

	@Test(expected = BugError.class)
	public void instanceFieldsInitialization_NotPOJO() throws ConfigException {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<person>" + //
				"	<instance-field name='name' value='John Doe' />" + //
				"</person>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		MockManagedClassSPI<PersonFields> managedClass = new MockManagedClassSPI<>(PersonFields.class);
		managedClass.config = builder.build();
		managedClass.instanceType = InstanceType.SERVICE;
		IInstancePostConstructionProcessor processor = getInstanceFieldsInitializationProcessor();
		processor.onInstancePostConstruction(managedClass, new PersonFields());
	}

	private static IInstancePostConstructionProcessor getInstanceFieldsInitializationProcessor() {
		return Classes.newInstance("js.tiny.container.service.InstanceFieldsInitializationProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// CONFIGURABLE INSTANCE PROCESSOR

	@Test
	public void configurable() {
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.config = new Config("test");
		IInstancePostConstructionProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		processor.onInstancePostConstruction(managedClass, joker);

		assertNotNull(joker.config);
		assertEquals("test", joker.config.getName());
	}

	/** Configuration processor on instance without Configurable interface should do nothing and throw none. */
	@Test
	public void configurable_NoConfigurable() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.config = new Config("test");
		IInstancePostConstructionProcessor processor = getConfigurableInstanceProcessor();

		Person person = new Person();
		processor.onInstancePostConstruction(managedClass, person);
	}

	/** Managed class without configuration object should not execute instance configuration. */
	@Test
	public void configurable_NoConfigObject() {
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		IInstancePostConstructionProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		processor.onInstancePostConstruction(managedClass, joker);

		assertNull(joker.config);
	}

	/** Invalid configuration object should throw bug error. */
	@Test(expected = BugError.class)
	public void configurable_Invalid() {
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.config = new Config("test");
		IInstancePostConstructionProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		joker.invalid = true;
		processor.onInstancePostConstruction(managedClass, joker);
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void configurable_NullInstance() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.config = new Config("test");
		IInstancePostConstructionProcessor processor = getConfigurableInstanceProcessor();
		processor.onInstancePostConstruction(managedClass, null);
	}

	/** Exception on configuration execution should throw bug error. */
	@Test(expected = BugError.class)
	public void configurable_Exception() {
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.config = new Config("test");
		IInstancePostConstructionProcessor processor = getConfigurableInstanceProcessor();

		Joker joker = new Joker();
		joker.exception = true;
		processor.onInstancePostConstruction(managedClass, joker);
	}

	private static IInstancePostConstructionProcessor getConfigurableInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.service.ConfigurableInstanceProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// POST_CONSTRUCT INSTANCE PROCESSOR

	@Test
	public void postConstruct() {
		MockManagedMethodSPI managedMethod = new MockManagedMethodSPI(Joker.getPostConstructMethod());
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.attribute = managedMethod;
		IInstancePostConstructionProcessor processor = getPostConstructInstanceProcessor();

		Joker joker = new Joker();
		processor.onInstancePostConstruction(managedClass, joker);

		assertEquals(1, joker.postConstructProbe);
	}

	@Test
	public void postConstruct_NoManagedPostConstruct() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		IInstancePostConstructionProcessor processor = getPostConstructInstanceProcessor();

		Person person = new Person();
		processor.onInstancePostConstruction(managedClass, person);
	}

	/** Null instance arguments should not throw exception. */
	@Test
	public void postConstruct_NullInstance() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		IInstancePostConstructionProcessor processor = getPostConstructInstanceProcessor();
		processor.onInstancePostConstruction(managedClass, null);
	}

	@Test(expected = BugError.class)
	public void postConstruct_Exception() {
		MockManagedMethodSPI managedMethod = new MockManagedMethodSPI(Joker.getPostConstructMethod());
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.attribute = managedMethod;
		IInstancePostConstructionProcessor processor = getPostConstructInstanceProcessor();

		Joker joker = new Joker();
		joker.exception = true;
		processor.onInstancePostConstruction(managedClass, joker);
	}

	private static IInstancePostConstructionProcessor getPostConstructInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.service.InstancePostConstructProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// LOGGER INSTANCE PROCESSOR

	@Test
	public void logger() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.instanceScope = InstanceScope.APPLICATION;
		managedClass.instanceType = InstanceType.PROXY;
		IInstancePostConstructionProcessor processor = getLoggerInstanceProcessor();

		Person person = new Person();
		processor.onInstancePostConstruction(managedClass, person);
	}

	@Test
	public void logger_MultipleInterfaces() {
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.instanceScope = InstanceScope.APPLICATION;
		managedClass.instanceType = InstanceType.PROXY;
		IInstancePostConstructionProcessor processor = getLoggerInstanceProcessor();

		Joker joker = new Joker();
		processor.onInstancePostConstruction(managedClass, joker);
	}

	@Test
	public void logger_NoDump() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.instanceScope = InstanceScope.LOCAL;
		managedClass.instanceType = InstanceType.POJO;
		IInstancePostConstructionProcessor processor = getLoggerInstanceProcessor();

		Person person = new Person();
		processor.onInstancePostConstruction(managedClass, person);
	}

	private static IInstancePostConstructionProcessor getLoggerInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.service.LoggerInstanceProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Person {
		private IFactory factory;
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
		private Map<Class<?>, IManagedClass<?>> classesPool = new HashMap<>();

		private void registerManagedClass(Class<?> interfaceClass, IManagedClass<?> managedClass) {
			classesPool.put(interfaceClass, managedClass);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
			return (IManagedClass<T>) classesPool.get(interfaceClass);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getInstance(Class<? super T> interfaceClass) {
			if (interfaceClass.getSimpleName().equals("TransactionalResource")) {
				Class<?> clazz = Classes.forName("js.container.TransactionalResourceImpl");
				return (T) Classes.newInstance(clazz, this);
			}
			throw new UnsupportedOperationException("getInstance(Class<? super T>, Object...)");
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
			IManagedClass<T> managedClass = (IManagedClass<T>) classesPool.get(interfaceClass);
			if (managedClass == null) {
				return null;
			}
			return (T) Classes.newInstance(managedClass.getImplementationClass());
		}
	}

	private static class MockManagedClassSPI<T> extends ManagedClassSpiStub<T> {
		private static MockContainer container = new MockContainer();
		private Config config;
		private List<Field> dependencies = new ArrayList<>();
		private Class<T>[] interfaceClasses;
		private Class<T> implementationClass;
		private InstanceScope instanceScope = InstanceScope.APPLICATION;
		private InstanceType instanceType = InstanceType.POJO;
		private boolean transactional;
		private Object attribute;

		@SuppressWarnings("unchecked")
		public MockManagedClassSPI(Class<T> type) {
			this.interfaceClasses = (Class<T>[]) type.getInterfaces();
			if (this.interfaceClasses.length == 0) {
				this.interfaceClasses = new Class[] { type };
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
		public Integer getKey() {
			return 1;
		}

		@Override
		public Class<T>[] getInterfaceClasses() {
			return interfaceClasses;
		}

		@Override
		public Class<T> getImplementationClass() {
			return implementationClass;
		}

		@Override
		public InstanceScope getInstanceScope() {
			return instanceScope;
		}

		@Override
		public InstanceType getInstanceType() {
			return instanceType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <A> A getAttribute(Object context, String name, Class<A> type) {
			return (A)attribute;
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
