package js.container.test;

import static org.hamcrest.Matchers.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import js.annotation.Inject;
import js.annotation.Local;
import js.annotation.Service;
import js.container.Container;
import js.container.InstanceScope;
import js.container.InstanceType;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.InvocationException;
import js.lang.ManagedLifeCycle;
import js.servlet.TinyConfigBuilder;
import js.test.stub.ContainerStub;
import js.transaction.Transactional;
import js.unit.TestConfigBuilder;
import js.util.Classes;

public class ManagedClassSpiConformanceTest {
	@Test
	public void getContainer() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(MockContainer.class, getManagedClass(config(config)).getContainer().getClass());
	}

	@Test
	public void getKey() throws Exception {
		// reset key seed for this test case in order to have predictable sequence
		Class<?> managedClassClass = Class.forName("js.container.ManagedClass");
		AtomicInteger keySeed = Classes.getFieldValue(managedClassClass, "KEY_SEED");
		keySeed.set(0);

		String config = config("<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />");
		List<Integer> keys = new ArrayList<>(4);
		for (int i = 0; i < keys.size(); ++i) {
			keys.add(getManagedClass(config).getKey());
		}

		List<Integer> sortedKeys = new ArrayList<>(keys.size());
		Collections.copy(sortedKeys, keys);
		Collections.sort(sortedKeys);
		
		assertThat(keys, equalTo(sortedKeys));
	}

	@Test
	public void getConfig() throws Exception {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />" + //
				"   </managed-classes>" + //
				"	<test>" + //
				"		<property name='model' value='Opel Corsa' />" + //
				"	</test>" + //
				"</config>";

		ManagedClassSPI managedClass = getManagedClass(descriptor);
		Config config = managedClass.getConfig();

		assertNotNull(config);
		assertEquals("Opel Corsa", config.getProperty("model"));
	}

	@Test
	public void getInterfceClasses_NoInterfaces() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		Class<?>[] interfaceClasses = getManagedClass(config(config)).getInterfaceClasses();
		assertNotNull(interfaceClasses);
		assertEquals(1, interfaceClasses.length);
		assertEquals(CarImpl.class, interfaceClasses[0]);
	}

	@Test
	public void getInterfceClasses_SingleInterfaces() throws Exception {
		String config = "<test interface='js.container.test.ManagedClassSpiConformanceTest$Car' class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		Class<?>[] interfaceClasses = getManagedClass(config(config)).getInterfaceClasses();
		assertNotNull(interfaceClasses);
		assertEquals(1, interfaceClasses.length);
		assertEquals(Car.class, interfaceClasses[0]);
	}

	@Test
	public void getInterfceClasses_MultipleInterfaces() throws Exception {
		String config = "" + //
				"<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl'>" + //
				"	<interface name='js.lang.Configurable' />" + //
				"	<interface name='js.container.test.ManagedClassSpiConformanceTest$Car' />" + //
				"</test>";
		Class<?>[] interfaceClasses = getManagedClass(config(config)).getInterfaceClasses();
		assertNotNull(interfaceClasses);
		assertEquals(2, interfaceClasses.length);
		assertEquals(Configurable.class, interfaceClasses[0]);
		assertEquals(Car.class, interfaceClasses[1]);
	}

	@Test
	public void getInterfaceClass() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class, getManagedClass(config(config)).getInterfaceClass());
	}

	@Test(expected = BugError.class)
	public void getInterfaceClass_MultipleInterfaces() throws Exception {
		String config = "" + //
				"<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl'>" + //
				"	<interface name='js.lang.Configurable' />" + //
				"	<interface name='js.container.test.ManagedClassSpiConformanceTest$Car' />" + //
				"</test>";
		getManagedClass(config(config)).getInterfaceClass();
	}

	@Test
	public void getImplementationClass() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class, getManagedClass(config(config)).getImplementationClass());
	}

	@Test
	public void getConstructor() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class.getDeclaredConstructor(), getManagedClass(config(config)).getConstructor());
	}

	@Test
	public void getDependencies() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$Agency' />";
		List<Field> dependencies = new ArrayList<>();
		for (Field dependency : getManagedClass(config(config)).getDependencies()) {
			dependencies.add(dependency);
		}

		assertEquals(2, dependencies.size());
		assertTrue(dependencies.contains(Agency.class.getDeclaredField("person")));
		assertTrue(dependencies.contains(Agency.class.getDeclaredField("car")));
	}

	@Test
	public void getManagedMethods_PROXY() throws Exception {
		String config = "<test interface='js.container.test.ManagedClassSpiConformanceTest$Car' class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		List<String> methodNames = new ArrayList<>();
		for (ManagedMethodSPI method : getManagedClass(config(config)).getManagedMethods()) {
			methodNames.add(method.getMethod().getName());
		}

		assertEquals(4, methodNames.size());
		assertTrue(methodNames.contains("config"));
		assertTrue(methodNames.contains("postConstruct"));
		assertTrue(methodNames.contains("preDestroy"));
		assertTrue(methodNames.contains("getModel"));
	}

	@Test
	public void getManagedMethods_POJO() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertFalse(getManagedClass(config(config)).getManagedMethods().iterator().hasNext());
	}

	@Test
	public void getManagedMethod() throws Exception {
		String config = "<test interface='js.container.test.ManagedClassSpiConformanceTest$Car' class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		Method method = Car.class.getDeclaredMethod("getModel");
		ManagedMethodSPI managedMethod = getManagedClass(config(config)).getManagedMethod(method);

		assertNotNull(managedMethod);
		assertEquals(method, managedMethod.getMethod());
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_NoSuchMethodException() throws Exception {
		String config = "<test interface='js.container.test.ManagedClassSpiConformanceTest$Car' class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		Method method = Object.class.getDeclaredMethod("toString");
		getManagedClass(config(config)).getManagedMethod(method);
	}

	@Test(expected = BugError.class)
	public void getManagedMethod_NotPROXY() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		Method method = CarImpl.class.getDeclaredMethod("getModel");
		getManagedClass(config(config)).getManagedMethod(method);
	}

	@Test
	public void getNetMethod() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$NetCar' />";
		ManagedMethodSPI netMethod = getManagedClass(config(config)).getNetMethod("getModel");
		assertNotNull(netMethod);
		assertEquals("getModel", netMethod.getMethod().getName());
	}

	@Test
	public void getNetMethod_NotFound() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$NetCar' />";
		assertNull(getManagedClass(config(config)).getNetMethod("getCounty"));
	}

	@Test
	public void getInstanceScope() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(InstanceScope.APPLICATION, getManagedClass(config(config)).getInstanceScope());
	}

	@Test
	public void getInstanceType() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(InstanceType.POJO, getManagedClass(config(config)).getInstanceType());
	}

	@Test
	public void isTransactional() throws Exception {
		String config = "<test interface='js.container.test.ManagedClassSpiConformanceTest$Car' class='js.container.test.ManagedClassSpiConformanceTest$TransactionalCar' type='PROXY' />";
		assertTrue(getManagedClass(config(config)).isTransactional());
	}

	@Test
	public void isRemote() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$NetCar' />";
		assertTrue(getManagedClass(config(config)).isRemotelyAccessible());
	}

	@Test
	public void getRequestPath() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$NetCar' />";
		assertEquals("net/car", getManagedClass(config(config)).getRequestPath());
	}

	@Test(expected = BugError.class)
	public void getRequestPath_NotRemote() throws Exception {
		String config = "<test class='js.container.test.ManagedClassSpiConformanceTest$CarImpl' />";
		getManagedClass(config(config)).getRequestPath();
	}

	@Test
	public void getImplementationURL() throws Exception {
		String config = "<test interface='js.container.test.ManagedClassSpiConformanceTest$Car' type='REMOTE' url='http://server/' />";
		assertEquals("http://server/", getManagedClass(config(config)).getImplementationURL());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static ManagedClassSPI getManagedClass(String config) throws Exception {
		TinyConfigBuilder builder = new TestConfigBuilder(config);
		Config appDescriptor = builder.build();
		Container container = new MockContainer();

		Config classDescriptor = null;
		for (Config managedClasses : appDescriptor.findChildren("managed-classes")) {
			classDescriptor = managedClasses.getChild("test");
			if (classDescriptor != null) {
				break;
			}
		}

		try {
			return Classes.newInstance("js.container.ManagedClass", container, classDescriptor);
		} catch (InvocationException e) {
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			}
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			throw e;
		}
	}

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

	private static interface Car {
		String getModel();
	}

	private static class CarImpl implements Car, Configurable, ManagedLifeCycle {
		@Override
		public void config(Config configSection) throws ConfigException {
		}

		@Override
		public void postConstruct() throws Exception {
		}

		@Override
		public void preDestroy() throws Exception {
		}

		@Override
		public String getModel() {
			return "opel";
		}
	}

	@Transactional
	private static class TransactionalCar implements Car {
		@Override
		public String getModel() {
			return null;
		}
	}

	private static class Agency {
		@Inject
		private String person;

		@Inject
		private Car car;
	}

	@Service("net/car")
	private static class NetCar {
		@SuppressWarnings("unused")
		public String getModel() {
			return "ford";
		}

		@Local
		public String getCountry() {
			return "US";
		}
	}

	private static class MockContainer extends ContainerStub {
	}
}
