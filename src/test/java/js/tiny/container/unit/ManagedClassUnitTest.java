package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.inject.Inject;

import org.junit.BeforeClass;
import org.junit.Test;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.ManagedLifeCycle;
import js.lang.VarArgs;
import js.tiny.container.Container;
import js.tiny.container.InstanceScope;
import js.tiny.container.InstanceType;
import js.tiny.container.ManagedClass;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.core.Factory;
import js.tiny.container.servlet.TinyConfigBuilder;
import js.tiny.container.stub.ContainerStub;
import js.transaction.TransactionContext;
import js.transaction.Transactional;
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
		ManagedClassSPI managedClass = getManagedClass(config(config));

		assertEquals(Pojo.class, managedClass.getInterfaceClasses()[0]);
		assertEquals(PojoImpl.class, managedClass.getImplementationClass());
		assertNotNull(managedClass.getContainer());
		assertNull(managedClass.getConfig());
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$PojoImpl:js.tiny.container.unit.ManagedClassUnitTest$Pojo:POJO:APPLICATION:LOCAL", managedClass.toString());
		assertEquals(InstanceType.POJO, managedClass.getInstanceType());
		assertEquals(InstanceScope.APPLICATION, managedClass.getInstanceScope());
		assertFalse(managedClass.isRemotelyAccessible());
		assertFalse(managedClass.isTransactional());

		Constructor<PojoImpl> constructor = PojoImpl.class.getDeclaredConstructor();
		assertNotNull(constructor);
		assertEquals(constructor, managedClass.getConstructor());

		Collection<?> dependencies = (Collection<?>) managedClass.getDependencies();
		assertNotNull(dependencies);
		assertEquals(0, dependencies.size());

		Map<Method, ManagedMethodSPI> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertEquals(0, methodsPool.size());
	}

	@Test
	public void transactionalConstructor() throws Throwable {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$DaoImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$Dao' type='PROXY' />";
		ManagedClassSPI managedClass = getManagedClass(config(config));

		assertEquals(Dao.class, managedClass.getInterfaceClasses()[0]);
		assertEquals(DaoImpl.class, managedClass.getImplementationClass());
		assertNotNull(managedClass.getContainer());
		assertNull(managedClass.getConfig());
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$DaoImpl:js.tiny.container.unit.ManagedClassUnitTest$Dao:PROXY:APPLICATION:LOCAL", managedClass.toString());
		assertEquals(InstanceType.PROXY, managedClass.getInstanceType());
		assertEquals(InstanceScope.APPLICATION, managedClass.getInstanceScope());
		assertFalse(managedClass.isRemotelyAccessible());
		assertFalse(managedClass.isTransactional());
	}

	@Test
	public void containerConstructor() throws Throwable {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' type='PROXY' />";
		ManagedClassSPI managedClass = getManagedClass(config(config));

		assertEquals(Pojo.class, managedClass.getInterfaceClasses()[0]);
		assertEquals(PojoImpl.class, managedClass.getImplementationClass());
		assertNotNull(managedClass.getContainer());
		assertNull(managedClass.getConfig());
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$PojoImpl:js.tiny.container.unit.ManagedClassUnitTest$Pojo:PROXY:APPLICATION:LOCAL", managedClass.toString());
		assertEquals(InstanceType.PROXY, managedClass.getInstanceType());
		assertEquals(InstanceScope.APPLICATION, managedClass.getInstanceScope());
		assertFalse(managedClass.isRemotelyAccessible());
		assertFalse(managedClass.isTransactional());

		Constructor<PojoImpl> constructor = PojoImpl.class.getDeclaredConstructor();
		assertNotNull(constructor);
		assertEquals(constructor, managedClass.getConstructor());

		Collection<?> dependencies = (Collection<?>) managedClass.getDependencies();
		assertNotNull(dependencies);
		assertEquals(0, dependencies.size());

		Map<Method, ManagedMethodSPI> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
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
		ManagedMethodSPI managedMethod = (ManagedMethodSPI) methodsPool.get(method);
		assertNotNull(managedMethod);
	}

	@Test
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
	public void configSections() throws Exception {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />" + //
				"   </managed-classes>" + //
				"	<test>" + //
				"		<property name='engine' value='ECO' />" + //
				"	</test>" + //
				"</config>";

		ManagedClassSPI managedClass = getManagedClass(descriptor);
		Config config = managedClass.getConfig();

		assertNotNull(config);
		assertEquals("ECO", config.getProperty("engine"));
	}

	@Test
	public void staticFieldInitializing() throws Exception {
		String descriptor = "<?xml version='1.0' ?>" + //
				"<config>" + //
				"   <managed-classes>" + //
				"   	<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />" + //
				"   </managed-classes>" + //
				"	<test>" + //
				"		<static-field name='MODEL' value='Opel Corsa' />" + //
				"	</test>" + //
				"</config>";

		getManagedClass(descriptor);
		assertEquals("Opel Corsa", CarImpl.MODEL);
	}

	@Test
	public void remoteManagedClass() throws Exception {
		String config = "<test interface='js.tiny.container.unit.ManagedClassUnitTest$Pojo' type='REMOTE' url='http://services.bbnet.ro/' />";
		Object managedClass = getManagedClass(config(config));

		assertNotNull(managedClass);
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$Pojo:REMOTE:APPLICATION:LOCAL:http://services.bbnet.ro/", managedClass.toString());
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
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$Pojo:REMOTE:APPLICATION:LOCAL:http://services.bbnet.ro/", managedClass.toString());

		Map<?, ?> methodsPool = Classes.getFieldValue(managedClass, "methodsPool");
		assertNotNull(methodsPool);
		assertTrue(methodsPool.isEmpty());
	}

	@Test
	public void newInstanceVarArgs() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$PojoVarArgs' />";
		TestContext.start(config(config));
		PojoVarArgs pojo = Factory.getInstance(PojoVarArgs.class, new VarArgs<String>("one", "two", "three"));
		assertEquals("one", pojo.values[0]);
		assertEquals("two", pojo.values[1]);
		assertEquals("three", pojo.values[2]);
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
	public void missingInterfaceName() throws Exception {
		String config = "" + //
				"<test class='js.tiny.container.unit.ManagedClassSpiConformanceTest$CarImpl'>" + //
				"	<interface names='js.lang.Configurable' />" + //
				"</test>";
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
	public void asyncTransactional() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$AsyncTransactionalImpl' interface='js.tiny.container.unit.ManagedClassUnitTest$AsyncTransactional' type='PROXY' />";
		getManagedClass(config(config));
	}

	@Test(expected = BugError.class)
	public void asyncReturn() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$AsyncReturn' />";
		getManagedClass(config(config));
	}

	@Test(expected = BugError.class)
	public void asyncPOJO() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$AsyncPOJO' />";
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
	// MANAGED CLASS UTILITY METHODS

	@Test(expected = BugError.class)
	public void getDeclaredConstructor_BadImplementationClass() throws Exception {
		Classes.invoke(ManagedClass.class, "getDeclaredConstructor", Car.class);
	}

	@Test(expected = BugError.class)
	public void getDeclaredConstructor_MultipleConstructors() throws Exception {
		Classes.invoke(ManagedClass.class, "getDeclaredConstructor", MultipleConstructors.class);
	}

	// --------------------------------------------------------------------------------------------
	// OBJECT

	@Test
	public void objectEquals() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />";
		ManagedClassSPI managedClass = getManagedClass(config(config));
		assertTrue(managedClass.equals(managedClass));
		assertFalse(managedClass.equals(null));
	}

	@Test
	public void objectHashCode() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />";
		assertTrue(getManagedClass(config(config)).hashCode() != 0);
	}

	@Test
	public void objectToString() throws Exception {
		String config = "<test class='js.tiny.container.unit.ManagedClassUnitTest$CarImpl' />";
		assertEquals("test:js.tiny.container.unit.ManagedClassUnitTest$CarImpl:js.tiny.container.unit.ManagedClassUnitTest$CarImpl:POJO:APPLICATION:NET", getManagedClass(config(config)).toString());
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

	private static interface Pojo {
		String getString();
	}

	private static final class PojoImpl implements Pojo {
		@Override
		public String getString() {
			return "string";
		}
	}

	private static interface Dao {

	}

	@Transactional
	private static class DaoImpl implements Dao {

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

	private static class CarImpl implements Car, Configurable, ManagedLifeCycle {
		private static String MODEL;

		@Inject
		private TransactionContext database;

		private String engine;
		public boolean afterInstanceCreatedInvoked;
		public boolean beforeInstanceDisposalInvoked;

		@Override
		public void config(Config configSection) throws ConfigException {
			assertNotNull(configSection);
			this.engine = configSection.getProperty("engine");
		}

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

	@Transactional
	private static interface AsyncTransactional {
		@Asynchronous
		void exec();
	}

	private static final class AsyncTransactionalImpl implements AsyncTransactional {
		public void exec() {
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
