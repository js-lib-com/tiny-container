package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.DenyAll;
import javax.ejb.Remote;
import javax.inject.Inject;
import javax.ws.rs.Path;

import org.junit.Ignore;
import org.junit.Test;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.tiny.container.cdi.Binding;
import js.tiny.container.core.Container;
import js.tiny.container.core.ManagedClass;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ContainerStub;

@Ignore
public class ManagedClassSpiConformanceTest {
	@Test
	public void getContainer() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(MockContainer.class, getManagedClass(config(config)).getContainer().getClass());
	}

	@Test
	@Ignore
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
	@Ignore
	public void getImplementationClass() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' />";
		assertEquals(CarImpl.class, getManagedClass(config(config)).getImplementationClass());
	}

	@Test
	@Ignore
	public void getManagedMethods_PROXY() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		List<String> methodNames = new ArrayList<>();
		for (IManagedMethod method : getManagedClass(config(config)).getManagedMethods()) {
			methodNames.add(method.getMethod().getName());
		}

		assertEquals(3, methodNames.size());
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
	@Ignore
	public void getManagedMethod() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassSpiConformanceTest$Car' class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl' type='PROXY' />";
		Method method = CarImpl.class.getDeclaredMethod("getModel");
		IManagedMethod managedMethod = getManagedClass(config(config)).getManagedMethod(method.getName());

		assertNotNull(managedMethod);
		assertEquals(method, managedMethod.getMethod());
	}

	@Test
	public void getNetMethod_NotFound() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$NetCar' />";
		assertNull(getManagedClass(config(config)).getManagedMethod("getCounty"));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static IManagedClass<?> getManagedClass(String config) throws Exception {
		ConfigBuilder builder = new ConfigBuilder(config);
		Config appDescriptor = builder.build();
		Container container = new MockContainer();

		Config classDescriptor = null;
		for (Config managedClasses : appDescriptor.findChildren("managed-classes")) {
			classDescriptor = managedClasses.getChild("test");
			if (classDescriptor != null) {
				break;
			}
		}
		
		Class<?> interfaceClass = classDescriptor.getAttribute("interface", Class.class);
		Class<?> implementationClass = classDescriptor.getAttribute("class", Class.class);
		
		Binding<?> binding = new Binding(interfaceClass, implementationClass);
		return new ManagedClass<>(container, binding);
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

	private static class CarImpl implements Car {
		@PostConstruct
		public void postConstruct() throws Exception {
		}

		@PreDestroy
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
