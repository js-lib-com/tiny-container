package js.tiny.container.core.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Enumeration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.unit.TestContext;

@Ignore
public class AppFactoryUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	// --------------------------------------------------------------------------------------------
	// GET INSTANCE

	@Test
	public void getInstance_ConstructorArguments() throws Exception {
		String descriptor = "<person class='js.tiny.container.core.unit.AppFactoryUnitTest$Person' scope='LOCAL' />";
		IContainer factory = TestContext.start(config(descriptor));
		Person person = factory.getInstance(Person.class);
		assertNotNull(person);
	}

	@Test
	public void getInstance_ApplicationScope() throws Exception {
		String descriptor = "<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='APPLICATION' />";
		IContainer factory = TestContext.start(config(descriptor));

		Car car1 = factory.getInstance(Car.class);
		Car car2 = factory.getInstance(Car.class);

		assertNotNull(car1);
		assertNotNull(car2);
		assertTrue(car1 == car2);
	}

	/**
	 * Two references of the same managed class with APPLICATION scope should be equal even if created from different threads.
	 */
	@Test
	public void getInstance_CrossThreadApplicationScope() throws Exception {
		String descriptor = "<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='APPLICATION' />";
		final IContainer factory = TestContext.start(config(descriptor));

		class TestRunnable implements Runnable {
			private Car car;

			@Override
			public void run() {
				car = factory.getInstance(Car.class);
			}
		}

		TestRunnable runnable = new TestRunnable();
		Thread thread = new Thread(runnable);
		thread.start();
		join(thread);

		// since Car scope is APPLICATION instance created from main thread is the same as the one created from separated thread
		assertEquals(runnable.car, factory.getInstance(Car.class));
	}

	/** Instances of the same managed class with LOCAL scope should always be different. */
	@Test
	public void getInstance_LocalScope() throws Exception {
		String descriptor = "<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='LOCAL' />";
		IContainer factory = TestContext.start(config(descriptor));

		Car car1 = factory.getInstance(Car.class);
		Car car2 = factory.getInstance(Car.class);

		assertNotNull(car1);
		assertNotNull(car2);
		// local scope creates a new instance on every call
		assertTrue(car1 != car2);
	}

	@Test
	public void getInstance_PojoType() throws Exception {
		String descriptor = "<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' type='POJO' />";
		IContainer factory = TestContext.start(config(descriptor));
		Car car = factory.getInstance(Car.class);
		assertFalse(Proxy.isProxyClass(car.getClass()));
	}

	@Test
	public void getInstance_ContainerType() throws Exception {
		String descriptor = "<car interface='js.tiny.container.core.unit.AppFactoryUnitTest$Product' class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' type='PROXY' />";
		IContainer factory = TestContext.start(config(descriptor));
		Product product = factory.getInstance(Product.class);
		assertTrue(Proxy.isProxyClass(product.getClass()));
	}

	public void getInstance_RemoteType() throws Exception {
		String descriptor = "<product interface='js.tiny.container.core.unit.AppFactoryUnitTest$Product' type='REMOTE' url='http://localhost/' />";
		IContainer factory = TestContext.start(config(descriptor));

		Product product = factory.getInstance(Product.class);
		assertNotNull(product);
		assertTrue(Proxy.isProxyClass(product.getClass()));
	}

	@Test
	public void getInstance_ServiceType() throws Exception {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader() {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (name.contains("$Product")) {
					name = "js/tiny/container/core/unit/AppFactoryUnitTest$Product";
				}
				return super.getResources(name);
			}
		});

		Product product = null;
		try {
			String descriptor = "<product interface='js.tiny.container.core.unit.AppFactoryUnitTest$Product' type='SERVICE' />";
			IContainer factory = TestContext.start(config(descriptor));
			product = factory.getInstance(Product.class);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

		assertNotNull(product);
		assertFalse(Proxy.isProxyClass(product.getClass()));
		assertTrue(product instanceof Car);
	}

	/** Constructor exception should rise invocation exception. */
	@Test(expected = InvocationException.class)
	public void getInstance_ConstructorInvocationException() throws Exception {
		String descriptor = "<person class='js.tiny.container.core.unit.AppFactoryUnitTest$ExceptionalObject' scope='LOCAL' />";
		IContainer factory = TestContext.start(config(descriptor));
		factory.getInstance(ExceptionalObject.class);
	}

	@Test
	public void getInstance_ApplicationMultipleInterfaces() throws Exception {
		String descriptor = "" + //
				"<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car'>" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Product' />" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Vehicle' />" + //
				"</car>";
		IContainer factory = TestContext.start(config(descriptor));

		Product product = factory.getInstance(Product.class);
		Vehicle vehicle = factory.getInstance(Vehicle.class);
		assertEquals(product, vehicle);
		assertEquals("AS1234", product.getSerial());
		assertEquals("Opel Corsa", vehicle.getModel());
	}

	@Test
	public void getInstance_ThreadMultipleInterfaces() throws Exception {
		String descriptor = "" + //
				"<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='THREAD'>" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Product' />" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Vehicle' />" + //
				"</car>";
		IContainer factory = TestContext.start(config(descriptor));

		Product product = factory.getInstance(Product.class);
		Vehicle vehicle = factory.getInstance(Vehicle.class);
		assertEquals(product, vehicle);
		assertEquals("AS1234", product.getSerial());
		assertEquals("Opel Corsa", vehicle.getModel());
	}

	/** Create new local instance for a managed class with multiple interfaces even if invoked with different interface. */
	@Test
	public void getInstance_LocalMultipleInterfaces() throws Exception {
		String descriptor = "" + //
				"<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='LOCAL'>" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Product' />" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Vehicle' />" + //
				"</car>";
		IContainer factory = TestContext.start(config(descriptor));

		Product product = factory.getInstance(Product.class);
		Vehicle vehicle = factory.getInstance(Vehicle.class);
		assertFalse(product == vehicle);
		assertEquals("AS1234", product.getSerial());
		assertEquals("Opel Corsa", vehicle.getModel());
	}

	@Test
	public void getInstance_AccessViaImplementation() throws Exception {
		String descriptor = "" + //
				"<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car'>" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Car' />" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Product' />" + //
				"	<interface name='js.tiny.container.core.unit.AppFactoryUnitTest$Vehicle' />" + //
				"</car>";
		IContainer factory = TestContext.start(config(descriptor));

		Car car = factory.getInstance(Car.class);
		Product product = factory.getInstance(Product.class);
		Vehicle vehicle = factory.getInstance(Vehicle.class);
		assertEquals(car, product);
		assertEquals(product, vehicle);
		assertEquals("AS1234", car.getSerial());
		assertEquals("AS1234", product.getSerial());
		assertEquals("Opel Corsa", car.getModel());
		assertEquals("Opel Corsa", vehicle.getModel());
	}

	/** Attempting to get instance for a not registered managed class should rise bug exception. */
	@Test(expected = BugError.class)
	public void getInstance_NotRegisteredManagedClass() throws Exception {
		String descriptor = "";
		IContainer factory = TestContext.start(config(descriptor));
		factory.getInstance(Car.class);
	}

	/** Instantiating not provided service should throw exception. */
	@Test(expected = NoProviderException.class)
	public void getInstance_NotProvidedService() throws Exception {
		String descriptor = "<product interface='js.tiny.container.core.unit.AppFactoryUnitTest$Vehicle' type='SERVICE' />";
		IContainer factory = TestContext.start(config(descriptor));
		factory.getInstance(Vehicle.class);
	}

	/** Null interface class should throw illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void getInstance_NullInterfaceClass() throws Exception {
		String descriptor = "<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='LOCAL' />";
		IContainer factory = TestContext.start(config(descriptor));
		factory.getInstance((Class)null);
	}

	// --------------------------------------------------------------------------------------------
	// GET OPTIONAL INSTANCE

	@Test
	public void getOptionalInstance() throws Exception {
		String descriptor = "<car class='js.tiny.container.core.unit.AppFactoryUnitTest$Car' scope='LOCAL' />";
		IContainer factory = TestContext.start(config(descriptor));
		assertNotNull(factory.getOptionalInstance(Car.class));
	}

	/** Attempting to get optional instance for a not registered managed class should return null. */
	@Test
	public void getOptionalInstance_NotRegisteredManagedClass() throws Exception {
		String descriptor = "";
		IContainer factory = TestContext.start(config(descriptor));
		assertNull(factory.getOptionalInstance(Car.class));
	}

	/** Attempting to get optional instance for a not provided service should return null. */
	@Test
	public void getOptionalInstance_NotProvidedService() throws Exception {
		String descriptor = "<product interface='js.tiny.container.core.unit.AppFactoryUnitTest$Vehicle' type='SERVICE' />";
		IContainer factory = TestContext.start(config(descriptor));
		assertNull(factory.getOptionalInstance(Vehicle.class));
	}

	/** Constructor exception should rise invocation exception. */
	@Test(expected = InvocationException.class)
	public void getOptionalInstance_ConstructorInvocationException() throws Exception {
		String descriptor = "<person class='js.tiny.container.core.unit.AppFactoryUnitTest$ExceptionalObject' scope='LOCAL' />";
		IContainer factory = TestContext.start(config(descriptor));
		factory.getOptionalInstance(ExceptionalObject.class);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String config(String managedClassDescriptor) {
		String config = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"       %s" + //
				"   </managed-classes>" + //
				"</config>";
		return String.format(config, managedClassDescriptor);
	}

	/**
	 * Wait for thread to finish. This method is necessary only when run tests from Maven, Surefire plugin. Apparently there is
	 * a bug when current thread keeps interrupted flag and, when reused, thread.join() throws InterruptedException. This
	 * behavior is not consistent; it depends on operating system - for example on Windows is working well, and apparently on
	 * Maven / Surefire version. Also on a virtual machine on Windows host tests are working properly.
	 */
	private static void join(Thread thread) {
		long timestamp = System.currentTimeMillis() + 2000;
		for (;;) {
			long delay = timestamp - System.currentTimeMillis();
			if (delay <= 0) {
				break;
			}
			try {
				thread.join(delay);
			} catch (Throwable unused) {
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	public static interface Product {
		String getSerial();
	}

	public static interface Vehicle {
		String getModel();
	}

	public static class Car implements Vehicle, Product {
		private String name;

		public Car() {
		}

		public Car(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String getModel() {
			return "Opel Corsa";
		}

		@Override
		public String getSerial() {
			return "AS1234";
		}
	}

	public static class Person {
		private String name;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static class ExceptionalObject {
		public ExceptionalObject() throws IOException {
			throw new IOException();
		}
	}
}
