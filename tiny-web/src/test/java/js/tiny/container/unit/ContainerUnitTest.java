package js.tiny.container.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.security.PermitAll;
import javax.ejb.Remote;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import js.converter.Converter;
import js.converter.ConverterException;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.InstanceInvocationHandler;
import js.lang.ManagedLifeCycle;
import js.tiny.container.core.Container;
import js.tiny.container.core.InstanceKey;
import js.tiny.container.core.ManagedClass;
import js.tiny.container.net.EventStream;
import js.tiny.container.net.EventStreamManager;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
import js.tiny.container.stub.ContainerStub;
import js.util.Classes;

@SuppressWarnings({ "unchecked", "rawtypes", "unused", "hiding" })
public class ContainerUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	// --------------------------------------------------------------------------------------------
	// CONTAINER LIFE CYCLE

	/** Check built-in factories and processors initialization. */
	@Test
	public void constructor() {
		Container container = new ContainerStub();
		Map<Class<?>, IManagedClass> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertTrue(classesPool.isEmpty());
	}

	private static void assertClass(String expected, Object object) {
		assertEquals(expected, object.getClass().getSimpleName());
	}

	/** Check classes pool initialization. */
	@Test
	public void config() throws ConfigException {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<test class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		Container container = new MockContainer();
		container.config(builder.build());

		Map<Class<?>, IManagedClass> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertFalse(classesPool.isEmpty());
		assertNotNull(classesPool.get(NetCar.class));
	}

	/** Test that managed keys sequence respects managed classes order from descriptors. */
	@Test
	@Ignore
	public void config_ManagedKeysSequence() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<test class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		Object container = TestContext.start(config);
		Map<Class<?>, IManagedClass> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");

		// order depends on lib-descriptor; keep this test case and lib-descriptor in sync

		Class<?>[] expectedOrder = new Class<?>[] { RequestContext.class, EventStreamManager.class, EventStream.class, NetCar.class };
		for (int i = 1; i < expectedOrder.length; ++i) {
			Integer previousKey = classesPool.get(expectedOrder[i - 1]).getKey();
			Integer currentKey = classesPool.get(expectedOrder[i]).getKey();
			// keys can be equal when two classes implements the same interface
			// current key is after previous key; exception above condition
			assertThat(currentKey, greaterThanOrEqualTo(previousKey));
		}
	}

	@Test
	public void config_ManagedClassOverwritten() throws Exception {
		AtomicInteger keySeed = Classes.getFieldValue(ManagedClass.class, "KEY_SEED");

		String descriptor = "" + //
				"<config>" + //
				"   <managed-classes>" + //
				"		<car class='js.tiny.container.unit.ContainerUnitTest$Car' scope='APPLICATION' />" + //
				"		<car class='js.tiny.container.unit.ContainerUnitTest$Car' scope='THREAD' type='POJO' />" + //
				"   </managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);
		Container container = new ContainerStub();

		Integer key = null;
		synchronized (keySeed) {
			// when tests run on multiple threads, e.g. Maven, is possible for key to be incremented concurrently
			// so that key class can be greater than current seed value
			key = keySeed.get();
			container.config(builder.build());
		}

		Map<Class<?>, IManagedClass> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertEquals(1, classesPool.size());

		IManagedClass managedClass = classesPool.get(Car.class);
		assertNotNull(managedClass);
		assertThat(managedClass.getKey(), greaterThanOrEqualTo(key));
		assertThat(managedClass.getInstanceScope(), equalTo(InstanceScope.THREAD));
		assertThat(managedClass.getInstanceType(), equalTo(InstanceType.POJO));
	}

	/** Invalid managed scope should throw configuration exception. */
	@Test(expected = IllegalStateException.class)
	public void config_InvalidScopeInstance() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' scope='INVALID' />";
		TestContext.start(config(descriptor));
	}

	/** Invalid managed type should throw configuration exception. */
	@Test(expected = ConfigException.class)
	public void config_InvalidTypeInstance() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' type='INVALID' />";
		TestContext.start(config(descriptor));
	}

	@Test
	public void start() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<car class='js.tiny.container.unit.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		class MockContainer extends ContainerStub {
			private Class<?> interfaceClass;

			@Override
			public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
				return null;
			}

			@Override
			public <T> T getInstance(Class<? super T> interfaceClass) {
				this.interfaceClass = interfaceClass;
				return null;
			}

		}

		MockContainer container = new MockContainer();
		container.config(builder.build());
		container.start();

		assertEquals(ManagedCar.class, container.interfaceClass);
	}

	@Test
	public void start_ManagedClassOrder() throws ConfigException {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<net-car class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"		<managed-car class='js.tiny.container.unit.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		class MockContainer extends ContainerStub {
			List<Class<?>> instantiatedClasses = new ArrayList<>();

			@Override
			public <T> T getInstance(Class<? super T> interfaceClass) {
				instantiatedClasses.add(interfaceClass);
				return null;
			}
		}

		MockContainer container = new MockContainer();
		container.config(builder.build());
		container.start();

		assertEquals(1, container.instantiatedClasses.size());
		assertEquals("ManagedCar", container.instantiatedClasses.get(0).getSimpleName());
	}

	@Test
	public void destroy() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<car class='js.tiny.container.unit.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		Container container = (Container) TestContext.start(config);

		ManagedCar car = container.getInstance(ManagedCar.class);
		assertEquals(1, car.postConstructProbe);
		assertEquals(0, car.preDestroyProbe);

		container.destroy();
		assertEquals(1, car.postConstructProbe);
		assertEquals(1, car.preDestroyProbe);
	}

	@Test
	public void destroy_ManagedClassReverseOrder() throws ConfigException {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<net-car class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"		<managed-car class='js.tiny.container.unit.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		Container container = new ContainerStub();
		container.config(builder.build());
		container.start();

		ManagedCar car = container.getInstance(ManagedCar.class);

		container.destroy();

		assertTrue(car.destroyTimestamp > 0);
	}

	/** Exception on pre-destroy execution should not be signaled to container destroy but just dumped to logger. */
	@Test
	public void preDestroyException() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<car class='js.tiny.container.unit.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		Container container = (Container) TestContext.start(config);

		ManagedCar mock = container.getInstance(ManagedCar.class);
		mock.preDestroyException = true;

		container.destroy();
	}

	// --------------------------------------------------------------------------------------------
	// INSTANCE RETRIEVAL ALGORITHM

	@Test
	public void getInstance() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNotNull(container.getInstance(Car.class));
	}

	@Test
	public void getInstance_PROXY() throws Exception {
		String descriptor = "<car interface='js.tiny.container.unit.ContainerUnitTest$CarInterface' class='js.tiny.container.unit.ContainerUnitTest$Car' type='PROXY' />";
		Container container = (Container) TestContext.start(config(descriptor));
		CarInterface car = container.getInstance(CarInterface.class);
		assertNotNull(car);
		assertTrue(Proxy.isProxyClass(car.getClass()));
		assertTrue(car instanceof Proxy);
		assertTrue(Proxy.getInvocationHandler(car) instanceof InstanceInvocationHandler);
	}

	@Test
	public void getInstance_LOCAL() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' scope='LOCAL' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNotNull(container.getInstance(Car.class));
	}

	public void getInstance_REMOTE() throws Exception {
		String descriptor = "<car interface='js.tiny.container.unit.ContainerUnitTest$CarInterface' type='REMOTE' url='http://server/' />";
		Container container = (Container) TestContext.start(config(descriptor));
		CarInterface car = container.getInstance(CarInterface.class);
		assertNotNull(car);
		assertTrue(Proxy.isProxyClass(car.getClass()));
		assertTrue(car instanceof Proxy);
	}

	@Test(expected = BugError.class)
	public void getInstance_NoMnagedClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance(Car.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstance_NullInterfaceClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance((Class) null);
	}

	@Test
	@Ignore
	public void getInstanceByName() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));

		Car opel = container.getInstance(Car.class, "opel");
		assertNotNull(opel);

		Car ford = container.getInstance(Car.class, "ford");
		assertNotNull(ford);
		assertFalse(opel == ford);
	}

	@Test(expected = BugError.class)
	public void getInstanceByName_NoManagedClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance(Car.class, "opel");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceByName_NullName() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance(Car.class, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceByName_EmptyName() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance(Car.class, "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceByName_NullInterfaceClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance(null, "opel");
	}

	@Test
	public void getOptionalInstance() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNotNull(container.getOptionalInstance(Car.class));
	}

	@Test
	public void getOptionalInstance_NoManagedClass() throws Exception {
		Container container = (Container) TestContext.start();
		assertNull(container.getOptionalInstance(Car.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getOptionalInstance_NullInterfaceClass() throws Exception {
		Container container = (Container) TestContext.start();
		assertNull(container.getOptionalInstance(null));
	}

	@Test
	public void getOptionalInstance_NoProvider() throws Exception {
		String descriptor = "<car interface='js.tiny.container.unit.ContainerUnitTest$Car' type='SERVICE' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNull(container.getOptionalInstance(Car.class));
	}

	@Test
	public void getInstanceByManagedClass() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		Map<Class<?>, IManagedClass> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(container.getInstance(classesPool.get(Car.class)));
	}

	// --------------------------------------------------------------------------------------------
	// CONTAINER SPI

	@Test
	public void getManagedClasses() throws Exception {
		IContainer container = (IContainer) TestContext.start();
		int classesCount = 0;
		for (IManagedClass managedClass : container.getManagedClasses()) {
			++classesCount;
		}
		// this hard coded value depends on lib-descriptor.xml
		assertEquals(5, classesCount);
	}

	@Test
	public void getManagedMethods() throws Exception {
		IContainer container = (IContainer) TestContext.start();
		int methodsCount = 0;
		for (IManagedMethod managedMethod : container.getManagedMethods()) {
			++methodsCount;
			System.out.println(managedMethod);
		}
		// this hard coded value depends on library classes declared as managed
		// it counts only methods from classes of PROXY type
		// curent methods are:
		// js.tiny.container.http.captcha.Captcha#verifyResponse(int,String)
		// js.tiny.container.http.captcha.Captcha#getChallenge(int)
		// js.tiny.container.http.captcha.Captcha#getChallenges()
		//assertEquals(0, methodsCount);
	}

	@Test(expected = BugError.class)
	public void getManagedMethods_NoManagedClasses() {
		Container container = new ContainerStub();
		container.getManagedMethods().iterator();
	}

	@Test
	public void isManagedClass() throws Exception {
		String descriptor = "<car class='js.tiny.container.unit.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertTrue(container.isManagedClass(Car.class));
		assertFalse(container.isManagedClass(Pojo.class));
	}

	@Test(expected = BugError.class)
	public void getNotRegisteredManagedInstance() throws Exception {
		IContainer container = (IContainer) TestContext.start();
		container.getInstance(Object.class, "object");
	}

	@Test
	public void converterInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <converters>" + //
				"   	<type class='java.lang.Object' converter='js.tiny.container.unit.ContainerUnitTest$ObjectConverter' />" + //
				"   </converters>" + //
				"</config>";
		TestContext.start(CONFIG);
		assertTrue(ConverterRegistry.hasType(Object.class));
	}

	@Test(expected = ConfigException.class)
	public void missingClassOnConverterInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <converters>" + //
				"   	<type class='java.lang.FakeObject' converter='js.tiny.container.unit.ContainerUnitTest$ObjectConverter' />" + //
				"   </converters>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void missingConverterInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <converters>" + //
				"   	<type class='java.lang.Object' converter='js.tiny.container.unit.ContainerUnitTest$FakeObjectConverter' />" + //
				"   </converters>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test
	public void managedClassStaticInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   	<net-car class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field name='FILE' value='/var/lib/tomcat/' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG);
		assertEquals(new File("/var/lib/tomcat"), Car.FILE);
	}

	@Test
	public void managedClassStaticInitialization_SystemProperty() throws Exception {
		System.setProperty("file.property", "/var/lib/tomcat/");
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   	<net-car class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field name='FILE' value='${file.property}' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG);
		System.clearProperty("file.property");
		assertEquals(new File("/var/lib/tomcat"), Car.FILE);
	}

	@Test
	public void managedClassStaticInitialization_ContextProperty() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("file.property", "/var/lib/tomcat/");
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   	<net-car class='js.tiny.container.unit.ContainerUnitTest$NetCar' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field name='FILE' value='${file.property}' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG, properties);
		assertEquals(new File("/var/lib/tomcat"), Car.FILE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void managedClassStaticInitialization_InvalidClassAttribute() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car clas='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   </managed-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_MissingName() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field names='FILES' value='/var/lib/tomcat/' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_MissingValue() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field name='FILES' values='/var/lib/tomcat/' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_MissingClass() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$FakeCar' />" + //
				"   </managed-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_MissingField() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field name='FILES' value='/var/lib/tomcat/' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_InstanceField() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.tiny.container.unit.ContainerUnitTest$Car' />" + //
				"   </managed-classes>" + //
				"   <car>" + //
				"   	<static-field name='name' value='Opel Corsa' />" + //
				"   </car>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test
	public void pojoStaticInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   	<another-pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field name='FILE' value='/var/lib/tomcat/' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG);
		assertEquals(new File("/var/lib/tomcat"), Pojo.FILE);
	}

	@Test
	public void pojoStaticInitialization_SystemProperty() throws Exception {
		System.setProperty("file.property", "/var/lib/tomcat/");
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   	<another-pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field name='FILE' value='${file.property}' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG);
		System.clearProperty("file.property");
		assertEquals(new File("/var/lib/tomcat"), Pojo.FILE);
	}

	@Test
	public void pojoStaticInitialization_ContextProperty() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("file.property", "/var/lib/tomcat/");
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   	<another-pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field name='FILE' value='${file.property}' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG, properties);
		assertEquals(new File("/var/lib/tomcat"), Pojo.FILE);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_InvalidClassAttribute() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo clas='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_MissingName() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field names='FILE' value='/var/lib/tomcat/' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_MissingValue() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field name='FILE' values='/var/lib/tomcat/' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_MissingClass() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$FakePojo' />" + //
				"   </pojo-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_MissingField() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field name='FILES' value='/var/lib/tomcat/' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_InstanceField() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.tiny.container.unit.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"   <pojo>" + //
				"   	<static-field name='url' value='http://server/' />" + //
				"   </pojo>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test
	public void instanceKeyValue() {
		assertEquals("key", new InstanceKey("key").getValue());
		assertEquals("key", new InstanceKey("key").toString());
	}

	@Test
	public void instanceKeyEquality() {
		InstanceKey key1 = new InstanceKey("key");
		assertTrue(key1.equals(key1));
		InstanceKey key2 = new InstanceKey("key");
		assertTrue(key1.equals(key2));

		key1 = new InstanceKey("key1");
		key2 = new InstanceKey("key2");
		assertFalse(key1.equals(key2));

		assertFalse(key1.equals(null));
		assertFalse(key1.equals(new Object()));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String config(String classDescriptor) {
		String config = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"       %s" + //
				"   </managed-classes>" + //
				"</config>";
		return String.format(config, classDescriptor);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Pojo {
		private static File FILE;
		private URL url;
	}

	private static interface CarInterface {
		String getName();
	}

	private static class Car implements CarInterface {
		private static File FILE;
		private String name;

		public String getName() {
			return name;
		}
	}

	@Remote
	private static class NetCar {
		private String name;

		@PermitAll
		public String getName() {
			return name;
		}
	}

	private static class ManagedCar implements ManagedLifeCycle {
		private long destroyTimestamp;
		private boolean preDestroyException;
		private int postConstructProbe;
		private int preDestroyProbe;
		private String name;

		@Override
		public void postConstruct() throws Exception {
			++postConstructProbe;
		}

		@Override
		public void preDestroy() throws Exception {
			destroyTimestamp = System.nanoTime();
			if (preDestroyException) {
				throw new Exception("exception");
			}
			++preDestroyProbe;
		}

		public String getName() {
			return name;
		}
	}

	private static class ObjectConverter implements Converter {
		@Override
		public <T> T asObject(String string, Class<T> valueType) throws IllegalArgumentException, ConverterException {
			return null;
		}

		@Override
		public String asString(Object object) throws ConverterException {
			return null;
		}
	}

	private static class MockContainer extends ContainerStub {
		@Override
		public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
			return null;
		}
	}
}
