package js.tiny.container.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.security.DenyAll;
import javax.ejb.Remote;
import javax.inject.Inject;
import javax.ws.rs.Path;

import org.junit.Ignore;
import org.junit.Test;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.ManagedLifeCycle;
import js.tiny.container.core.Container;
import js.tiny.container.core.ManagedClass;
import js.tiny.container.servlet.TinyConfigBuilder;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
import js.tiny.container.stub.ContainerStub;
import js.util.Classes;

public class ManagedClassSpiConformanceTest {
	@Test
	public void getContainer() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(MockContainer.class, getManagedClass(config(config)).getContainer().getClass());
	}

	@Test
	public void getKey() throws Exception {
		// reset key seed for this test case in order to have predictable sequence
		AtomicInteger keySeed = Classes.getFieldValue(ManagedClass.class, "KEY_SEED");
		keySeed.set(0);

		String config = config("<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />");
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
				"   	<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />" + //
				"   </managed-classes>" + //
				"	<test>" + //
				"		<property name='model' value='Opel Corsa' />" + //
				"	</test>" + //
				"</config>";

		IManagedClass managedClass = getManagedClass(descriptor);
		Config config = managedClass.getConfig();

		assertNotNull(config);
		assertEquals("Opel Corsa", config.getProperty("model"));
	}

	@Test
	public void getInterfceClasses_NoInterfaces() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		Class<?>[] interfaceClasses = getManagedClass(config(config)).getInterfaceClasses();
		assertNotNull(interfaceClasses);
		assertEquals(1, interfaceClasses.length);
		assertEquals(CarImpl.class, interfaceClasses[0]);
	}

	@Test
	public void getInterfceClasses_SingleInterfaces() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		Class<?>[] interfaceClasses = getManagedClass(config(config)).getInterfaceClasses();
		assertNotNull(interfaceClasses);
		assertEquals(1, interfaceClasses.length);
		assertEquals(Car.class, interfaceClasses[0]);
	}

	@Test
	public void getInterfceClasses_MultipleInterfaces() throws Exception {
		String config = "" + //
				"<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl'>" + //
				"	<interface name='js.lang.Configurable' />" + //
				"	<interface name='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' />" + //
				"</test>";
		Class<?>[] interfaceClasses = getManagedClass(config(config)).getInterfaceClasses();
		assertNotNull(interfaceClasses);
		assertEquals(2, interfaceClasses.length);
		assertEquals(Configurable.class, interfaceClasses[0]);
		assertEquals(Car.class, interfaceClasses[1]);
	}

	@Test
	public void getInterfaceClass() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class, getManagedClass(config(config)).getInterfaceClass());
	}

	@Test(expected = BugError.class)
	@Ignore
	public void getInterfaceClass_MultipleInterfaces() throws Exception {
		String config = "" + //
				"<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl'>" + //
				"	<interface name='js.lang.Configurable' />" + //
				"	<interface name='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' />" + //
				"</test>";
		getManagedClass(config(config)).getInterfaceClass();
	}

	@Test
	public void getImplementationClass() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class, getManagedClass(config(config)).getImplementationClass());
	}

	@Test
	public void getConstructor() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class.getDeclaredConstructor(), getManagedClass(config(config)).getConstructor());
	}

	@Test
	public void getManagedMethods_PROXY() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		List<String> methodNames = new ArrayList<>();
		for (IManagedMethod method : getManagedClass(config(config)).getManagedMethods()) {
			methodNames.add(method.getMethod().getName());
		}

		assertEquals(4, methodNames.size());
		assertTrue(methodNames.contains("config"));
		assertTrue(methodNames.contains("postConstruct"));
		assertTrue(methodNames.contains("preDestroy"));
		assertTrue(methodNames.contains("getModel"));
	}

	@Test
	@Ignore
	public void getManagedMethods_POJO() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertFalse(getManagedClass(config(config)).getManagedMethods().iterator().hasNext());
	}

	@Test
	public void getManagedMethod() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		Method method = Car.class.getDeclaredMethod("getModel");
		IManagedMethod managedMethod = getManagedClass(config(config)).getManagedMethod(method.getName());

		assertNotNull(managedMethod);
		assertEquals(method, managedMethod.getMethod());
	}

	@Test
	public void getNetMethod_NotFound() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$NetCar' />";
		assertNull(getManagedClass(config(config)).getManagedMethod("getCounty"));
	}

	@Test
	public void getInstanceScope() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(InstanceScope.APPLICATION, getManagedClass(config(config)).getInstanceScope());
	}

	@Test
	public void getInstanceType() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(InstanceType.POJO, getManagedClass(config(config)).getInstanceType());
	}

	@Test
	public void getImplementationURL() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' type='REMOTE' url='http://server/' />";
		assertEquals("http://server/", getManagedClass(config(config)).getImplementationURL());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static IManagedClass getManagedClass(String config) throws Exception {
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

		return new ManagedClass(container, classDescriptor);
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

	@SuppressWarnings("unused")
	private static class Agency {
		@Inject
		private String person;

		@Inject
		private Car car;
	}

	@Remote
	@Path("net/car")
	private static class NetCar {
		@SuppressWarnings("unused")
		public String getModel() {
			return "ford";
		}

		@DenyAll
		public String getCountry() {
			return "US";
		}
	}

	private static class MockContainer extends ContainerStub {
	}
}
