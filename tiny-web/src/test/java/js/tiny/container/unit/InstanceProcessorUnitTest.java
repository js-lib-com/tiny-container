package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import js.lang.BugError;
import js.lang.Config;
import js.lang.InvocationException;
import js.lang.ManagedPostConstruct;
import js.tiny.container.core.Container;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePostConstructProcessor;
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
		List<IInstancePostConstructProcessor> processors = Classes.getFieldValue(container, Container.class, "instancePostConstructProcessors");

		assertNotNull(processors);
		assertEquals("InstanceFieldsInjectionProcessor", processors.get(0).getClass().getSimpleName());
		assertEquals("InstanceFieldsInitializationProcessor", processors.get(1).getClass().getSimpleName());
		assertEquals("ConfigurableInstanceProcessor", processors.get(2).getClass().getSimpleName());
		assertEquals("PostConstructInstanceProcessor", processors.get(3).getClass().getSimpleName());
		assertEquals("LoggerInstanceProcessor", processors.get(4).getClass().getSimpleName());
	}

	// --------------------------------------------------------------------------------------------
	// LOGGER INSTANCE PROCESSOR

	@Test
	public void logger() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.instanceScope = InstanceScope.APPLICATION;
		managedClass.instanceType = InstanceType.PROXY;
		IInstancePostConstructProcessor processor = getLoggerInstanceProcessor();

		Person person = new Person();
		processor.onInstancePostConstruct(managedClass, person);
	}

	@Test
	public void logger_MultipleInterfaces() {
		MockManagedClassSPI<Joker> managedClass = new MockManagedClassSPI<>(Joker.class);
		managedClass.instanceScope = InstanceScope.APPLICATION;
		managedClass.instanceType = InstanceType.PROXY;
		IInstancePostConstructProcessor processor = getLoggerInstanceProcessor();

		Joker joker = new Joker();
		processor.onInstancePostConstruct(managedClass, joker);
	}

	@Test
	public void logger_NoDump() {
		MockManagedClassSPI<Person> managedClass = new MockManagedClassSPI<>(Person.class);
		managedClass.instanceScope = InstanceScope.LOCAL;
		managedClass.instanceType = InstanceType.POJO;
		IInstancePostConstructProcessor processor = getLoggerInstanceProcessor();

		Person person = new Person();
		processor.onInstancePostConstruct(managedClass, person);
	}

	private static IInstancePostConstructProcessor getLoggerInstanceProcessor() {
		return Classes.newInstance("js.tiny.container.service.LoggerInstanceProcessor");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Person {
		private IContainer factory;
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

	private static class Joker implements ManagedPostConstruct {
		private Config config;
		private int postConstructProbe;
		private boolean invalid;
		private boolean exception;

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
		public <T> T getInstance(Class<T> interfaceClass) {
			if (interfaceClass.getSimpleName().equals("TransactionalResource")) {
				Class<?> clazz = Classes.forName("js.container.TransactionalResourceImpl");
				return (T) Classes.newInstance(clazz, this);
			}
			throw new UnsupportedOperationException("getInstance(Class<? super T>, Object...)");
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getOptionalInstance(Class<T> interfaceClass) {
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
		public Integer getKey() {
			return 1;
		}

		@Override
		public Class<T> getInterfaceClass() {
			return interfaceClasses[0];
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
