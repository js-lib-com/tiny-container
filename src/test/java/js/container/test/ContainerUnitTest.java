package js.container.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import js.annotation.Public;
import js.annotation.Remote;
import js.container.Container;
import js.container.ContainerSPI;
import js.container.InstanceFactory;
import js.container.InstanceKey;
import js.container.InstanceProcessor;
import js.container.InstanceScope;
import js.container.InstanceType;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.container.ScopeFactory;
import js.container.TransactionalResource;
import js.converter.Converter;
import js.converter.ConverterException;
import js.converter.ConverterRegistry;
import js.core.App;
import js.core.AppContext;
import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.InstanceInvocationHandler;
import js.lang.ManagedLifeCycle;
import js.mvc.ViewManager;
import js.net.EventStream;
import js.net.EventStreamManager;
import js.servlet.RequestContext;
import js.test.stub.AppContextStub;
import js.test.stub.ContainerStub;
import js.transaction.TransactionContext;
import js.transaction.TransactionManager;
import js.unit.TestContext;
import js.util.Classes;

import org.junit.BeforeClass;
import org.junit.Test;

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

		Map<InstanceScope, ScopeFactory> scopeFactories = Classes.getFieldValue(container, Container.class, "scopeFactories");
		assertNotNull(scopeFactories);
		assertEquals(3, scopeFactories.size());
		assertClass("ApplicationScopeFactory", scopeFactories.get(InstanceScope.APPLICATION));
		assertClass("ThreadScopeFactory", scopeFactories.get(InstanceScope.THREAD));
		assertNull(scopeFactories.get(InstanceScope.LOCAL));
		assertNotNull(Classes.getFieldValue(container, Container.class, "scopeMutex"));

		Map<InstanceType, InstanceFactory> instanceFactories = Classes.getFieldValue(container, Container.class, "instanceFactories");
		assertNotNull(instanceFactories);
		assertEquals(4, instanceFactories.size());
		assertClass("LocalInstanceFactory", instanceFactories.get(InstanceType.POJO));
		assertClass("LocalInstanceFactory", instanceFactories.get(InstanceType.PROXY));
		assertClass("ServiceInstanceFactory", instanceFactories.get(InstanceType.SERVICE));
		assertClass("RemoteInstanceFactory", instanceFactories.get(InstanceType.REMOTE));

		List<InstanceProcessor> instanceProcessors = Classes.getFieldValue(container, Container.class, "instanceProcessors");
		assertNotNull(instanceProcessors);
		assertEquals(6, instanceProcessors.size());
		assertClass("InstanceFieldsInjectionProcessor", instanceProcessors.get(0));
		assertClass("InstanceFieldsInitializationProcessor", instanceProcessors.get(1));
		assertClass("ConfigurableInstanceProcessor", instanceProcessors.get(2));
		assertClass("PostConstructInstanceProcessor", instanceProcessors.get(3));
		assertClass("CronMethodsProcessor", instanceProcessors.get(4));
		assertClass("LoggerInstanceProcessor", instanceProcessors.get(5));

		assertNotNull(Classes.getFieldValue(container, Container.class, "argumentsProcessor"));
		assertClass("ArgumentsProcessor", Classes.getFieldValue(container, Container.class, "argumentsProcessor"));

		Map<Class<?>, ManagedClassSPI> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertTrue(classesPool.isEmpty());
	}

	/** Constructor initialization with custom scope factory. */
	@Test
	public void constructorCustomScopeFactory() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader() {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (name.contains("ScopeFactory")) {
					name = "js/container/test/scope-factory";
				}
				return super.getResources(name);
			}
		});
		try {
			Container container = new ContainerStub();
			Map<InstanceScope, ScopeFactory> scopeFactories = Classes.getFieldValue(container, Container.class, "scopeFactories");
			assertNotNull(scopeFactories.get(new InstanceScope("MOCK")));
			assertClass("MockScopeFactory", scopeFactories.get(new InstanceScope("MOCK")));
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	/** Constructor initialization with custom instance factory. */
	@Test
	public void constructorCustomInstanceFactory() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader() {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (name.contains("InstanceFactory")) {
					name = "js/container/test/instance-factory";
				}
				return super.getResources(name);
			}
		});
		try {
			Container container = new ContainerStub();
			Map<InstanceType, InstanceFactory> instanceFactories = Classes.getFieldValue(container, Container.class, "instanceFactories");
			assertNotNull(instanceFactories.get(new InstanceType("MOCK")));
			assertClass("MockInstanceFactory", instanceFactories.get(new InstanceType("MOCK")));
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
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
				"		<test class='js.container.test.ContainerUnitTest$NetCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		Container container = new MockContainer();
		container.config(builder.build());

		Map<Class<?>, ManagedClassSPI> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertFalse(classesPool.isEmpty());
		assertNotNull(classesPool.get(NetCar.class));
	}

	/** Test that managed keys sequence respects managed classes order from descriptors. */
	@Test
	public void config_ManagedKeysSequence() throws Exception {
		resetKeySeed();

		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<test class='js.container.test.ContainerUnitTest$NetCar' />" + //
				"		<app interface='js.core.App' class='js.container.test.ContainerUnitTest$MockApp' />" + //
				"	</managed-classes>" + //
				"</config>";
		Object container = TestContext.start(config);
		Map<Class<?>, ManagedClassSPI> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");

		// order depends on lib-descriptor; keep this test case and lib-descriptor in sync

		assertEquals("0", classesPool.get(App.class).getKey());
		assertEquals("1", classesPool.get(AppContext.class).getKey());
		// assertEquals("2", classesPool.get(Observer.class).getKey());
		assertEquals("3", classesPool.get(RequestContext.class).getKey());
		assertEquals("4", classesPool.get(TransactionManager.class).getKey());
		assertEquals("5", classesPool.get(TransactionalResource.class).getKey());
		assertEquals("5", classesPool.get(TransactionContext.class).getKey());
		assertEquals("6", classesPool.get(ViewManager.class).getKey());
		// assertEquals("7", classesPool.get(Captcha.class).getKey());
		assertEquals("8", classesPool.get(EventStreamManager.class).getKey());
		assertEquals("9", classesPool.get(EventStream.class).getKey());
		// ...
		assertEquals("13", classesPool.get(NetCar.class).getKey());
	}

	@Test
	public void config_AppSubclass() throws ConfigException {
		String config = "" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<app class='js.core.App' />" + //
				"		<app interface='js.core.App' class='js.container.test.ContainerUnitTest$MockApp' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		Container container = new MockContainer();
		container.config(builder.build());

		Map<Class<?>, ManagedClassSPI> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertFalse(classesPool.isEmpty());
		assertEquals(1, classesPool.size());

		ManagedClassSPI app = classesPool.get(App.class);
		assertNotNull(app);
		assertEquals(App.class, app.getInterfaceClass());
		assertEquals(MockApp.class, app.getImplementationClass());
	}

	@Test
	public void config_ManagedClassOverwritten() throws Exception {
		resetKeySeed();

		String descriptor = "" + //
				"<config>" + //
				"   <managed-classes>" + //
				"		<car class='js.container.test.ContainerUnitTest$Car' scope='APPLICATION' />" + //
				"		<car class='js.container.test.ContainerUnitTest$Car' scope='THREAD' type='POJO' />" + //
				"   </managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);
		Container container = new ContainerStub();
		container.config(builder.build());

		Map<Class<?>, ManagedClassSPI> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertEquals(1, classesPool.size());

		ManagedClassSPI managedClass = classesPool.get(Car.class);
		assertNotNull(managedClass);
		assertEquals("0", managedClass.getKey());
		assertEquals(InstanceScope.THREAD, managedClass.getInstanceScope());
		assertEquals(InstanceType.POJO, managedClass.getInstanceType());
	}

	/** Invalid managed scope should throw configuration exception. */
	@Test(expected = ConfigException.class)
	public void config_InvalidScopeInstance() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' scope='INVALID' />";
		TestContext.start(config(descriptor));
	}

	/** Invalid managed type should throw configuration exception. */
	@Test(expected = ConfigException.class)
	public void config_InvalidTypeInstance() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' type='INVALID' />";
		TestContext.start(config(descriptor));
	}

	@Test
	public void start() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<car class='js.container.test.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		class MockContainer extends ContainerStub {
			private Class<?> interfaceClass;

			@Override
			public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
				return null;
			}

			@Override
			public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
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
				"		<app class='js.core.App' />" + //
				"		<observer class='js.container.Observer' />" + //
				"		<net-car class='js.container.test.ContainerUnitTest$NetCar' />" + //
				"		<managed-car class='js.container.test.ContainerUnitTest$ManagedCar' />" + //
				"		<app interface='js.core.App' class='js.container.test.ContainerUnitTest$MockApp' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		class MockContainer extends ContainerStub {
			List<Class<?>> instantiatedClasses = new ArrayList<>();

			@Override
			public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
				instantiatedClasses.add(interfaceClass);
				return null;
			}
		}

		MockContainer container = new MockContainer();
		container.config(builder.build());
		container.start();

		assertEquals(3, container.instantiatedClasses.size());
		assertEquals("App", container.instantiatedClasses.get(0).getSimpleName());
		assertEquals("Observer", container.instantiatedClasses.get(1).getSimpleName());
		assertEquals("ManagedCar", container.instantiatedClasses.get(2).getSimpleName());
	}

	@Test
	public void destroy() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<car class='js.container.test.ContainerUnitTest$ManagedCar' />" + //
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
				"		<app class='js.core.App' />" + //
				"		<app-context interface='js.core.AppContext' class='js.container.test.ContainerUnitTest$MockAppContext' />" + //
				"		<observer class='js.container.Observer' />" + //
				"		<net-car class='js.container.test.ContainerUnitTest$NetCar' />" + //
				"		<managed-car class='js.container.test.ContainerUnitTest$ManagedCar' />" + //
				"		<app interface='js.core.App' class='js.container.test.ContainerUnitTest$MockApp' />" + //
				"	</managed-classes>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(config);

		Container container = new ContainerStub();
		container.config(builder.build());
		container.start();

		MockApp app = container.getInstance(App.class);
		ManagedCar car = container.getInstance(ManagedCar.class);

		container.destroy();

		assertTrue(car.destroyTimestamp > 0);
		assertTrue(app.destroyTimestamp >= car.destroyTimestamp);
	}

	/** Exception on pre-destroy execution should not be signaled to container destroy but just dumped to logger. */
	@Test
	public void preDestroyException() throws Exception {
		String config = "<?xml version='1.0' encoding='UTF-8' ?>" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<car class='js.container.test.ContainerUnitTest$ManagedCar' />" + //
				"	</managed-classes>" + //
				"</config>";
		Container container = (Container) TestContext.start(config);

		ManagedCar mock = container.getInstance(ManagedCar.class);
		mock.preDestroyException = true;

		container.destroy();
	}

	/** Successful and overriding scope factory registration. */
	@Test
	public void registerScopeFactory() {
		final InstanceScope MOCK = new InstanceScope("MOCK");

		class MockContainer extends ContainerStub {
			@Override
			public void registerScopeFactory(ScopeFactory scopeFactory) {
				super.registerScopeFactory(scopeFactory);
			}
		}

		MockContainer container = new MockContainer();
		container.registerScopeFactory(new MockScopeFactory());

		try {
			container.registerScopeFactory(new MockScopeFactory());
			fail("Overriding instance scope should throw exception.");
		} catch (BugError unused) {
		}

		Map<InstanceScope, ScopeFactory> scopeFactories = Classes.getFieldValue(container, Container.class, "scopeFactories");
		assertNotNull(scopeFactories.get(MOCK));
		assertTrue(scopeFactories.get(MOCK) instanceof MockScopeFactory);
	}

	/** Successful and overriding instance factory registration. */
	@Test
	public void registerInstanceFactory() {
		final InstanceType MOCK = new InstanceType("MOCK");

		class MockContainer extends ContainerStub {
			@Override
			public void registerInstanceFactory(InstanceType instanceType, InstanceFactory instanceFactory) {
				super.registerInstanceFactory(instanceType, instanceFactory);
			}
		}

		MockContainer container = new MockContainer();
		container.registerInstanceFactory(MOCK, new MockInstanceFactory());

		try {
			container.registerInstanceFactory(MOCK, new MockInstanceFactory());
			fail("Overriding instance type should throw exception.");
		} catch (BugError unused) {
		}

		Map<InstanceType, InstanceFactory> instanceFactories = Classes.getFieldValue(container, Container.class, "instanceFactories");
		assertNotNull(instanceFactories.get(MOCK));
		assertTrue(instanceFactories.get(MOCK) instanceof MockInstanceFactory);
	}

	/** Successful and overriding instance processor registration. */
	@Test
	public void registerInstanceProcessor() {
		class MockInstanceProcessor implements InstanceProcessor {
			@Override
			public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
			}
		}

		class MockContainer extends ContainerStub {
			@Override
			public void registerInstanceProcessor(InstanceProcessor instanceProcessor) {
				super.registerInstanceProcessor(instanceProcessor);
			}
		}

		MockContainer container = new MockContainer();
		List<InstanceProcessor> instanceProcessors = Classes.getFieldValue(container, Container.class, "instanceProcessors");
		assertEquals(6, instanceProcessors.size());
		container.registerInstanceProcessor(new MockInstanceProcessor());

		try {
			container.registerInstanceProcessor(new MockInstanceProcessor());
			fail("Overriding instance processor should throw exception.");
		} catch (BugError unused) {
		}

		assertNotNull(instanceProcessors.get(6));
		assertTrue(instanceProcessors.get(6) instanceof MockInstanceProcessor);
	}

	// --------------------------------------------------------------------------------------------
	// INSTANCE RETRIEVAL ALGORITHM

	@Test
	public void getInstance() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNotNull(container.getInstance(Car.class));
	}

	@Test
	public void getInstance_PROXY() throws Exception {
		String descriptor = "<car interface='js.container.test.ContainerUnitTest$CarInterface' class='js.container.test.ContainerUnitTest$Car' type='PROXY' />";
		Container container = (Container) TestContext.start(config(descriptor));
		CarInterface car = container.getInstance(CarInterface.class);
		assertNotNull(car);
		assertTrue(Proxy.isProxyClass(car.getClass()));
		assertTrue(car instanceof Proxy);
		assertTrue(Proxy.getInvocationHandler(car) instanceof InstanceInvocationHandler);
	}

	@Test
	public void getInstance_LOCAL() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' scope='LOCAL' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNotNull(container.getInstance(Car.class));
	}

	public void getInstance_REMOTE() throws Exception {
		String descriptor = "<car interface='js.container.test.ContainerUnitTest$CarInterface' type='REMOTE' url='http://server/' />";
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
	public void getInstanceByName() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));

		Car opel = container.getInstance("opel", Car.class);
		assertNotNull(opel);

		Car ford = container.getInstance("ford", Car.class);
		assertNotNull(ford);
		assertFalse(opel == ford);
	}

	@Test(expected = BugError.class)
	public void getInstanceByName_NoManagedClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance("opel", Car.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceByName_NullName() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance((String) null, Car.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceByName_EmptyName() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance("", Car.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceByName_NullInterfaceClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getInstance("opel", (Class) null);
	}

	@Test
	public void getOptionalInstance() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' />";
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
		String descriptor = "<car interface='js.container.test.ContainerUnitTest$Car' type='SERVICE' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertNull(container.getOptionalInstance(Car.class));
	}

	@Test
	public void getInstanceByManagedClass() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		Map<Class<?>, ManagedClassSPI> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(container.getInstance(classesPool.get(Car.class)));
	}

	public void getRemoteInstance() throws Exception {
		Container container = (Container) TestContext.start();
		CarInterface car = container.getRemoteInstance("http://server", CarInterface.class);
		assertNotNull(car);
		assertTrue(Proxy.isProxyClass(car.getClass()));
		assertTrue(car instanceof Proxy);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getRemoteInstance_NullImplementationURL() throws Exception {
		Container container = (Container) TestContext.start();
		container.getRemoteInstance(null, CarInterface.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getRemoteInstance_NullInterfaceClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getRemoteInstance("http://server", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getRemoteInstance_BadInterfaceClass() throws Exception {
		Container container = (Container) TestContext.start();
		container.getRemoteInstance("http://server", Car.class);
	}

	@Test(expected = BugError.class)
	public void getRemoteIntance_NoInstanceFactory() throws Exception {
		Container container = (Container) TestContext.start();
		Map<InstanceType, InstanceFactory> instanceFactories = Classes.getFieldValue(container, Container.class, "instanceFactories");
		instanceFactories.remove(InstanceType.REMOTE);
		container.getRemoteInstance("http://server", CarInterface.class);
	}

	@Test(expected = BugError.class)
	public void getRemoteIntance_NoRemoteFactory() throws Exception {
		Container container = (Container) TestContext.start();
		Map<InstanceType, InstanceFactory> instanceFactories = Classes.getFieldValue(container, Container.class, "instanceFactories");
		instanceFactories.put(InstanceType.REMOTE, new MockInstanceFactory());
		container.getRemoteInstance("http://server", CarInterface.class);
	}

	// --------------------------------------------------------------------------------------------
	// CONTAINER SPI

	/** Abstract container is always authenticated. */
	@Test
	public void isAuthenticated() throws Exception {
		Container container = new ContainerStub();
		assertTrue(container.isAuthenticated());
	}

	@Test
	public void getManagedClasses() throws Exception {
		ContainerSPI container = (ContainerSPI) TestContext.start();
		int classesCount = 0;
		for (ManagedClassSPI managedClass : container.getManagedClasses()) {
			++classesCount;
		}
		// this hard coded value depends on lib-descriptor.xml
		assertEquals(15, classesCount);
	}

	@Test
	public void getManagedMethods() throws Exception {
		ContainerSPI container = (ContainerSPI) TestContext.start();
		int methodsCount = 0;
		for (ManagedMethodSPI managedMethod : container.getManagedMethods()) {
			++methodsCount;
			System.out.println(managedMethod);
		}
		// this hard coded value depends on library classes declared as managed
		// it counts only methods from classes of PROXY type
		// curent methods are:
		// js.http.captcha.Captcha#verifyResponse(int,String)
		// js.http.captcha.Captcha#getChallenge(int)
		// js.http.captcha.Captcha#getImage(String)
		assertEquals(3, methodsCount);
	}

	@Test(expected = BugError.class)
	public void getManagedMethods_NoManagedClasses() {
		Container container = new ContainerStub();
		container.getManagedMethods().iterator();
	}

	@Test
	public void isManagedClass() throws Exception {
		String descriptor = "<car class='js.container.test.ContainerUnitTest$Car' />";
		Container container = (Container) TestContext.start(config(descriptor));
		assertTrue(container.isManagedClass(Car.class));
		assertFalse(container.isManagedClass(Pojo.class));
	}

	@Test(expected = BugError.class)
	public void getNotRegisteredManagedInstance() throws Exception {
		ContainerSPI container = (ContainerSPI) TestContext.start();
		container.getInstance("object", Object.class);
	}

	@Test
	public void converterInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <converters>" + //
				"   	<type class='java.lang.Object' converter='js.container.test.ContainerUnitTest$ObjectConverter' />" + //
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
				"   	<type class='java.lang.FakeObject' converter='js.container.test.ContainerUnitTest$ObjectConverter' />" + //
				"   </converters>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void missingConverterInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <converters>" + //
				"   	<type class='java.lang.Object' converter='js.container.test.ContainerUnitTest$FakeObjectConverter' />" + //
				"   </converters>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test
	public void managedClassStaticInitialization() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
				"   	<net-car class='js.container.test.ContainerUnitTest$NetCar' />" + //
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
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
				"   	<net-car class='js.container.test.ContainerUnitTest$NetCar' />" + //
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
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
				"   	<net-car class='js.container.test.ContainerUnitTest$NetCar' />" + //
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
				"   	<car clas='js.container.test.ContainerUnitTest$Car' />" + //
				"   </managed-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_MissingName() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
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
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
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
				"   	<car class='js.container.test.ContainerUnitTest$FakeCar' />" + //
				"   </managed-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void managedClassStaticInitialization_MissingField() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
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
				"   	<car class='js.container.test.ContainerUnitTest$Car' />" + //
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
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
				"   	<another-pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
				"   	<another-pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
				"   	<another-pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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
				"   	<pojo clas='js.container.test.ContainerUnitTest$Pojo' />" + //
				"   </pojo-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_MissingName() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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
				"   	<pojo class='js.container.test.ContainerUnitTest$FakePojo' />" + //
				"   </pojo-classes>" + //
				"</config>";
		TestContext.start(CONFIG);
	}

	@Test(expected = ConfigException.class)
	public void pojoStaticInitialization_MissingField() throws Exception {
		String CONFIG = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <pojo-classes>" + //
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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
				"   	<pojo class='js.container.test.ContainerUnitTest$Pojo' />" + //
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

	private static void resetKeySeed() throws ClassNotFoundException {
		// reset key seed for this test case in order to have predictable sequence
		Class<?> managedClassClass = Class.forName("js.container.ManagedClass");
		AtomicInteger keySeed = Classes.getFieldValue(managedClassClass, "KEY_SEED");
		keySeed.set(0);
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

		@Public
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
		public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
			return null;
		}
	}

	private static class MockApp extends App {
		private long destroyTimestamp;

		public MockApp(AppContext context) {
			super(context);
		}

		@Override
		public void preDestroy() throws Exception {
			destroyTimestamp = System.nanoTime();
		}
	}

	private static class MockAppContext extends AppContextStub {
		@Override
		public String getAppName() {
			return "Test App";
		}

	}

	// must be public to be instantiable by service loader
	public static class MockScopeFactory implements ScopeFactory {
		@Override
		public InstanceScope getInstanceScope() {
			return new InstanceScope("MOCK");
		}

		@Override
		public Object getInstance(InstanceKey instanceKey) {
			return null;
		}

		@Override
		public void persistInstance(InstanceKey instanceKey, Object instance) {
		}

		@Override
		public void clear() {
		}
	}

	// must be public to be instantiable by service loader
	public static class MockInstanceFactory implements InstanceFactory {
		@Override
		public InstanceType getInstanceType() {
			return new InstanceType("MOCK");
		}

		@Override
		public <T> T newInstance(ManagedClassSPI managedClass, Object... args) {
			return null;
		}
	}
}
