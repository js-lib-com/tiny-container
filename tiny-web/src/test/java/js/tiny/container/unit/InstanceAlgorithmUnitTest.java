package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import js.lang.Config;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;

public class InstanceAlgorithmUnitTest {
	private MockManagedClassSPI managedClass;
	private IContainer container;

	@Before
	public void beforeTest() {
		container = new MockContainer();
		managedClass = new MockManagedClassSPI(container);
	}

	@Test
	public void getLocalPojo() {
		managedClass.implementationClass = Person.class;
		managedClass.scope = InstanceScope.LOCAL;
		managedClass.type = InstanceType.POJO;

		Person person = container.getInstance(managedClass);
		assertNotNull(person);
	}

	@Test
	public void getApplicationPojo() {
		managedClass.implementationClass = Car.class;
		managedClass.scope = InstanceScope.APPLICATION;
		managedClass.type = InstanceType.POJO;

		Vehicle car1 = container.getInstance(managedClass);
		Vehicle car2 = container.getInstance(managedClass);

		assertNotNull(car1);
		assertNotNull(car2);
		assertFalse(car1 instanceof Proxy);
		assertFalse(car2 instanceof Proxy);
		assertEquals(car1, car2);
	}

	@Test
	public void getApplicationProxy() {
		managedClass.implementationClass = Car.class;
		managedClass.scope = InstanceScope.APPLICATION;
		managedClass.type = InstanceType.PROXY;

		Vehicle car1 = container.getInstance(managedClass);
		Vehicle car2 = container.getInstance(managedClass);

		assertNotNull(car1);
		assertNotNull(car2);
		assertTrue(car1 instanceof Proxy);
		assertTrue(car2 instanceof Proxy);
	}

	public void getApplicationService() {
		managedClass.implementationClass = Car.class;
		managedClass.scope = InstanceScope.APPLICATION;
		managedClass.type = InstanceType.SERVICE;

		Vehicle car1 = container.getInstance(managedClass);
		Vehicle car2 = container.getInstance(managedClass);

		assertNotNull(car1);
		assertNotNull(car2);
		assertTrue(car1 instanceof Proxy);
		assertTrue(car2 instanceof Proxy);
	}

	public void getApplicationRemote() {
		managedClass.implementationClass = Car.class;
		managedClass.scope = InstanceScope.APPLICATION;
		managedClass.type = InstanceType.REMOTE;

		Vehicle car1 = container.getInstance(managedClass);
		Vehicle car2 = container.getInstance(managedClass);

		assertNotNull(car1);
		assertNotNull(car2);
		assertTrue(car1 instanceof Proxy);
		assertTrue(car2 instanceof Proxy);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	public static class Person {
		public Person() {
		}
	}

	public static interface Vehicle {
	}

	public static class Car implements Vehicle {

	}

	private static class MockContainer extends ContainerStub {
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private final IContainer container;
		private Class<?> implementationClass;
		private InstanceScope scope;
		private InstanceType type;
		private boolean transactional;
		private boolean remote;

		public MockManagedClassSPI(IContainer container) {
			this.container = container;
		}

		@Override
		public IContainer getContainer() {
			return container;
		}

		@Override
		public Iterable<IManagedMethod> getManagedMethods() {
			return Collections.emptyList();
		}

		@Override
		public Integer getKey() {
			return 1;
		}

		@Override
		public Config getConfig() {
			return null;
		}

		@Override
		public Class<?>[] getInterfaceClasses() {
			return implementationClass.getInterfaces();
		}

		@Override
		public Class<?> getInterfaceClass() {
			return implementationClass.getInterfaces()[0];
		}

		@Override
		public Class<?> getImplementationClass() {
			return implementationClass;
		}

		@Override
		public Constructor<?> getConstructor() {
			try {
				return implementationClass.getConstructor();
			} catch (NoSuchMethodException e) {
			} catch (SecurityException e) {
			}
			return null;
		}

		@Override
		public Iterable<Field> getDependencies() {
			return Collections.emptyList();
		}

		@Override
		public IManagedMethod getPostConstructMethod() {
			return null;
		}

		@Override
		public IManagedMethod getPreDestroyMethod() {
			return null;
		}

		@Override
		public InstanceScope getInstanceScope() {
			return scope;
		}

		@Override
		public InstanceType getInstanceType() {
			return type;
		}

		@Override
		public boolean isTransactional() {
			return transactional;
		}

		@Override
		public String getTransactionalSchema() {
			return null;
		}

		@Override
		public boolean isRemotelyAccessible() {
			return remote;
		}

		@Override
		public String getImplementationURL() {
			return "http://localhost/";
		}
	}
}
