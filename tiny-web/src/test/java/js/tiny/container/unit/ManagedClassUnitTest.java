package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
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

@SuppressWarnings("unused")
public class ManagedClassUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	// --------------------------------------------------------------------------------------------
	// CONSTRUCTOR

	@Test
	public void pojoConstructor() throws Throwable {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' />";
		IManagedClass<?> managedClass = getManagedClass(config(config));

		assertEquals(Pojo.class, managedClass.getInterfaceClass());
		assertEquals(PojoImpl.class, managedClass.getImplementationClass());
		assertNotNull(managedClass.getContainer());
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$PojoImpl:js.tiny.container.unit.ManagedClassUnitTest$Pojo:POJO:APPLICATION", managedClass.toString());
		assertEquals(InstanceType.POJO, managedClass.getInstanceType());
		assertEquals(InstanceScope.APPLICATION, managedClass.getInstanceScope());

		Constructor<PojoImpl> constructor = PojoImpl.class.getDeclaredConstructor();
		assertNotNull(constructor);

		Map<Method, IManagedMethod> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertEquals(1, methodsPool.size());
	}

	@Test
	public void containerConstructor() throws Throwable {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' type='PROXY' />";
		IManagedClass<?> managedClass = getManagedClass(config(config));

		assertEquals(Pojo.class, managedClass.getInterfaceClass());
		assertEquals(PojoImpl.class, managedClass.getImplementationClass());
		assertNotNull(managedClass.getContainer());
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$PojoImpl:js.tiny.container.unit.ManagedClassUnitTest$Pojo:PROXY:APPLICATION", managedClass.toString());
		assertEquals(InstanceType.PROXY, managedClass.getInstanceType());
		assertEquals(InstanceScope.APPLICATION, managedClass.getInstanceScope());

		Constructor<PojoImpl> constructor = PojoImpl.class.getDeclaredConstructor();
		assertNotNull(constructor);

		Map<Method, IManagedMethod> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertEquals(1, methodsPool.size());
	}

	@Test
	public void netPojoConstructor() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$NetCar' class='js.tiny.container.unit.ManagedClassUnitTest$NetCarImpl' />";
		Object managedClass = getManagedClass(config(config));

		assertNotNull(managedClass);

		Map<?, ?> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);

		Method method = NetCar.class.getMethod("getModel");
		IManagedMethod managedMethod = (IManagedMethod) methodsPool.get(method.getName());
		assertNotNull(managedMethod);
	}

	@Test
	@Ignore
	public void netContainerConstructor() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$NetCarImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$NetCar' type='PROXY' />";
		Object managedClass = getManagedClass(config(config));

		assertNotNull(managedClass);
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$NetCarImpl:js.tiny.container.unit.ManagedClassUnitTest$NetCar:PROXY:APPLICATION:NET", managedClass.toString());

		Map<?, ?> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertEquals(1, methodsPool.size());
	}

	// --------------------------------------------------------------------------------------------

	@Test
	public void remoteManagedClass() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' type='REMOTE' url='http://services.bbnet.ro/' />";
		Object managedClass = getManagedClass(config(config));

		assertNotNull(managedClass);
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$Pojo:REMOTE:APPLICATION:http://services.bbnet.ro/", managedClass.toString());
		assertEquals("http://services.bbnet.ro/", Classes.getFieldValue(managedClass, "implementationURL"));

		Map<?, ?> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertTrue(methodsPool.isEmpty());
	}

	@Test
	public void remoteManagedClassWithSystemUrl() throws Exception {
		System.setProperty("connector.url", "http://services.bbnet.ro/");
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' type='REMOTE' url='${connector.url}' />";
		Object managedClass = getManagedClass(config(config));

		assertNotNull(managedClass);
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$Pojo:REMOTE:APPLICATION:http://services.bbnet.ro/", managedClass.toString());

		Map<?, ?> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertTrue(methodsPool.isEmpty());
	}

	// --------------------------------------------------------------------------------------------
	// INSANITY CHECK

	@Test(expected = ConfigException.class)
	public void interfaceNotFound() throws Exception {
		String config = "<test interface='js.tiny.container.unit.Fake' class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void implementationNotFound() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' class='js.tiny.container.unit.FakeImpl' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void implementationClassIsInterface() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$Car' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void implementationClassAbstract() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$AbstractCar' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void badType() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' type='BAD_TYPE' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	@Ignore
	public void badScope() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' scope='BAD_SCOPE' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void missingClassAttribute() throws Exception {
		String config = "<test implementation='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void implementationNotInheritInterface() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$Car' class='js.tiny.container.unit.ManagedClassUnitTest$DaoImpl' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void proxyWitoutInterface() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' type='PROXY' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void remoteManagedClassWitoutUrl() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' type='REMOTE' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void remoteManagedClassWithClass() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' type='REMOTE' url='http://services.bbnet.ro/' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void remoteManagedClassWithBadInterfaceAttribute() throws Exception {
		String config = "<test interfacex='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' type='REMOTE' url='http://services.bbnet.ro/' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void remoteManagedClassWithBadInterfaceClass() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' type='REMOTE' url='http://services.bbnet.ro/' />";
		getManagedClass(config(config));
	}

	@Test(expected = BugError.class)
	@Ignore
	public void asyncTransactional() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$AsyncTransactionalImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$AsyncTransactional' type='PROXY' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void managedLifeCycleWithBadScope() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$Car' scope='THREAD' />";
		getManagedClass(config(config));
	}

	@Test(expected = ConfigException.class)
	public void badImplementationClass() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$Car' interface='js.tiny.container.unit.ManagedClassUnitTest$Car' scope='THREAD' />";
		getManagedClass(config(config));
	}

	// --------------------------------------------------------------------------------------------
	// OBJECT

	@Test
	public void objectEquals() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />";
		IManagedClass<?> managedClass = getManagedClass(config(config));
		assertTrue(managedClass.equals(managedClass));
		assertFalse(managedClass.equals(null));
	}

	@Test
	public void objectHashCode() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />";
		assertTrue(getManagedClass(config(config)).hashCode() != 0);
	}

	@Test
	@Ignore
	public void objectToString() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />";
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$CarImpl:js.tiny.container.unit.ManagedClassUnitTest$CarImpl:POJO:APPLICATION:NET", getManagedClass(config(config)).toString());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static IManagedClass<?> getManagedClass(String config) throws Exception {
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

		return new ManagedClass<>(container, classDescriptor);
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

	private static interface Pojo {
		String getString();
	}

	private static final class PojoImpl implements Pojo {
		@Override
		public String getString() {
			return "string";
		}
	}

	@Remote
	@DenyAll
	private static interface Car {
		@PermitAll
		String getManufacturer();

		String getModel();

		String getModel(boolean engine);

		String getEngine();

		CarImpl getImpl();
	}

	private static abstract class AbstractCar implements Car {

	}

	private static class CarImpl implements Car, ManagedLifeCycle {
		private static String MODEL;

		private String engine;
		public boolean afterInstanceCreatedInvoked;
		public boolean beforeInstanceDisposalInvoked;

		@Override
		public void postConstruct() throws Exception {
			this.afterInstanceCreatedInvoked = true;
		}

		@Override
		public void preDestroy() throws Exception {
			this.beforeInstanceDisposalInvoked = true;
		}

		@Override
		public String getManufacturer() {
			return "Opel";
		}

		@Override
		public String getModel() {
			return MODEL;
		}

		@Override
		public String getModel(boolean engine) {
			return engine ? "ECO" : "Corsa";
		}

		@Override
		public String getEngine() {
			return this.engine;
		}

		@Override
		public CarImpl getImpl() {
			return this;
		}
	}

	private static class PojoVarArgs {
		public String[] values;

		public PojoVarArgs(String... values) {
			this.values = values;
		}
	}

	@Remote
	@PermitAll
	private static interface NetCar {
		String getModel();
	}

	private static class NetCarImpl implements NetCar {
		@Override
		public String getModel() {
			return null;
		}
	}

	@Remote
	private static class AsyncReturn {
		@Asynchronous
		public boolean exec() {
			return false;
		}
	}

	private static class AsyncPOJO {
		@Asynchronous
		public boolean exec() {
			return false;
		}
	}

	private static class Driver {
		private int id;
		private String name;

		public String getName() {
			return this.name;
		}
	}

	private static class MultipleConstructors {
		public MultipleConstructors(String name) {
		}

		public MultipleConstructors(int age) {
		}
	}

	private static class MockContainer extends ContainerStub {
	}
}
