package js.container.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import js.container.Container;
import js.container.InstanceFactory;
import js.container.InstanceType;
import js.container.ManagedClassSPI;
import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;
import js.rmi.RemoteFactory;
import js.rmi.RemoteFactoryProvider;
import js.rmi.UnsupportedProtocolException;
import js.test.stub.ManagedClassSpiStub;
import js.unit.TestContext;
import js.util.Classes;

/**
 * Unit tests for built-in instance factories. Built-in instance factories are those hard coded by container:
 * {@link LocalInstanceFactory} and {@link ServiceInstanceFactory}.
 * 
 * @author Iulian Rotaru
 */
@SuppressWarnings("unused")
public class InstanceFactoryUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@Test
	public void containerRegistration() throws Exception {
		Object container = TestContext.start();
		Map<InstanceType, InstanceFactory> instanceFactories = Classes.getFieldValue(container, Container.class, "instanceFactories");

		assertNotNull(instanceFactories);
		assertNotNull(instanceFactories.get(InstanceType.POJO));
		assertNotNull(instanceFactories.get(InstanceType.PROXY));
		assertNotNull(instanceFactories.get(InstanceType.SERVICE));

		assertTrue(instanceFactories.get(InstanceType.POJO) instanceof InstanceFactory);
		assertTrue(instanceFactories.get(InstanceType.PROXY) instanceof InstanceFactory);
		assertTrue(instanceFactories.get(InstanceType.SERVICE) instanceof InstanceFactory);
	}

	@Test
	public void instanceTypeValue() {
		assertEquals("POJO", InstanceType.POJO.getValue());
		assertEquals("PROXY", InstanceType.PROXY.getValue());
		assertEquals("SERVICE", InstanceType.SERVICE.getValue());
		assertEquals("REMOTE", InstanceType.REMOTE.getValue());
	}

	@Test
	public void instanceTypeHashCode() {
		assertEquals(31 + "TYPE".hashCode(), new InstanceType("TYPE").hashCode());
		assertEquals(31, new InstanceType().hashCode());
	}

	@Test
	public void instanceTypeEquality() {
		InstanceType type1 = new InstanceType("TYPE");
		assertTrue(type1.equals(type1));
		InstanceType type2 = new InstanceType("TYPE");
		assertTrue(type1.equals(type2));

		type1 = new InstanceType("TYPE1");
		type2 = new InstanceType("TYPE2");
		assertFalse(type1.equals(type2));

		assertFalse(type1.equals(null));
		assertFalse(type1.equals(new Object()));
		assertFalse(type1.equals(new InstanceType()));
		assertFalse(new InstanceType().equals(type1));
		assertTrue(new InstanceType().equals(new InstanceType()));
	}

	@Test
	public void instanceTypeConverter() {
		Converter converter = ConverterRegistry.getConverter();
		assertEquals("POJO", converter.asString(InstanceType.POJO));
		assertEquals(InstanceType.POJO, converter.asObject("POJO", InstanceType.class));
	}

	@Test
	public void instanceTypePredicates() {
		assertTrue(InstanceType.POJO.isPOJO());
		assertTrue(InstanceType.PROXY.isPROXY());
		assertTrue(InstanceType.SERVICE.isSERVICE());
		assertTrue(InstanceType.REMOTE.isREMOTE());
	}

	// --------------------------------------------------------------------------------------------
	// LOCAL INSTANCE FACTORY

	@Test
	public void localInstanceFactory() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getConstructor();
		managedClass.constructor.setAccessible(true);

		InstanceFactory factory = getLocalInstanceFactory();
		Person person = factory.newInstance(managedClass);
		assertNotNull(person);
		assertNull(person.name);
	}

	@Test
	public void localInstanceFactory_Arguments() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getConstructor(String.class);
		managedClass.constructor.setAccessible(true);

		InstanceFactory factory = getLocalInstanceFactory();
		Person person = factory.newInstance(managedClass, "John Doe");
		assertNotNull(person);
		assertEquals("John Doe", person.name);
	}

	@Test
	public void localInstanceFactory_NewInstanceOnEveryCall() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getConstructor();
		managedClass.constructor.setAccessible(true);

		final int TESTS_COUNT = 100000;
		Set<Object> instances = new HashSet<>();

		InstanceFactory factory = getLocalInstanceFactory();
		for (int i = 0; i < TESTS_COUNT; ++i) {
			assertTrue(instances.add(factory.newInstance(managedClass)));
		}
		assertEquals(TESTS_COUNT, instances.size());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void localInstanceFactory_InstanceType() {
		getLocalInstanceFactory().getInstanceType();
	}

	/** Null constructor should throw bug error. */
	@Test(expected = BugError.class)
	public void localInstanceFactory_NullConstructor() {
		ManagedClassSPI managedClass = new MockManagedClassSPI_NullConstructor();
		InstanceFactory factory = getLocalInstanceFactory();
		factory.newInstance(managedClass);
	}

	/** Abstract constructor should throw bug error. */
	@Test(expected = BugError.class)
	public void localInstanceFactory_AbstractConstructor() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = XMan.class.getConstructor();
		managedClass.constructor.setAccessible(true);

		InstanceFactory factory = getLocalInstanceFactory();
		factory.newInstance(managedClass);
	}

	/** Not accessible constructor should throw bug error. */
	@Test(expected = BugError.class)
	public void localInstanceFactory_NotAccessibleConstructor() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getDeclaredConstructor(int.class);

		InstanceFactory factory = getLocalInstanceFactory();
		factory.newInstance(managedClass);
	}

	/** Bad number of arguments should throw illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void localInstanceFactory_BadNumberOfArguments() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getConstructor();
		managedClass.constructor.setAccessible(true);

		InstanceFactory factory = getLocalInstanceFactory();
		factory.newInstance(managedClass, new Date());
	}

	/** Bad type argument should throw illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void localInstanceFactory_BadTypeArgument() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getConstructor(String.class);
		managedClass.constructor.setAccessible(true);

		InstanceFactory factory = getLocalInstanceFactory();
		factory.newInstance(managedClass, new Date());
	}

	/** Constructor exception should throw invocation exception. */
	@Test(expected = InvocationException.class)
	public void localInstanceFactory_ConstructorException() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.constructor = Person.class.getConstructor(boolean.class);
		managedClass.constructor.setAccessible(true);

		InstanceFactory factory = getLocalInstanceFactory();
		factory.newInstance(managedClass, true);
	}

	private static InstanceFactory getLocalInstanceFactory() {
		return Classes.newInstance("js.container.LocalInstanceFactory");
	}

	// --------------------------------------------------------------------------------------------
	// SERVICE INSTANCE FACTORY

	@Test
	public void serviceInstanceFactory() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.interfaceClasses = new Class[] { Human.class };
		InstanceFactory factory = getServiceInstanceFactory();

		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader() {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (name.contains("$Human")) {
					name = "js/container/test/human";
				}
				return super.getResources(name);
			}
		});

		try {
			factory.newInstance(managedClass);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void serviceInstanceFactory_InstanceType() {
		getServiceInstanceFactory().getInstanceType();
	}

	/** Missing service provider should throw no provider exception. */
	@Test(expected = NoProviderException.class)
	public void serviceInstanceFactory_NoProvider() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.interfaceClasses = new Class[] { Person.class };
		InstanceFactory factory = getServiceInstanceFactory();
		factory.newInstance(managedClass);
	}

	/** Null interface classes should throw bug error. */
	@Test(expected = BugError.class)
	public void serviceInstanceFactory_NullInterfaceClasses() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		InstanceFactory factory = getServiceInstanceFactory();
		factory.newInstance(managedClass);
	}

	/** Empty interface classes should throw bug error. */
	@Test(expected = BugError.class)
	public void serviceInstanceFactory_EmptyInterfaceClasses() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.interfaceClasses = new Class[] {};
		InstanceFactory factory = getServiceInstanceFactory();
		factory.newInstance(managedClass);
	}

	/** Invoke with arguments should throw illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void serviceInstanceFactory_Arguments() {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.interfaceClasses = new Class[] { Human.class };
		InstanceFactory factory = getServiceInstanceFactory();
		factory.newInstance(managedClass, "John Doe");
	}

	private static InstanceFactory getServiceInstanceFactory() {
		return Classes.newInstance("js.container.ServiceInstanceFactory");
	}

	// --------------------------------------------------------------------------------------------
	// REMOTE INSTANCE FACTORY

	public void remoteInstanceFactory() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		managedClass.interfaceClasses = new Class[] { Human.class };

		InstanceFactory factory = getRemoteInstanceFactory();
		assertEquals(InstanceType.REMOTE, factory.getInstanceType());
		Human person = factory.newInstance(managedClass);
		assertNotNull(person);
	}

	@Test(expected = IllegalArgumentException.class)
	public void remoteInstanceFactory_Arguments() throws Exception {
		MockManagedClassSPI managedClass = new MockManagedClassSPI();
		InstanceFactory factory = getRemoteInstanceFactory();
		factory.newInstance(managedClass, "argument");
	}

	public void remoteInstanceFactory_GetRemoteInstance() throws Exception {
		RemoteFactory factory = (RemoteFactory) getRemoteInstanceFactory();
		assertNotNull(factory.getRemoteInstance("http://server/", Human.class));
		assertNotNull(factory.getRemoteInstance("https://server/", Human.class));
	}

	@Test(expected = UnsupportedProtocolException.class)
	public void remoteInstanceFactory_NotRegistered() throws Exception {
		RemoteFactory factory = (RemoteFactory) getRemoteInstanceFactory();
		assertNotNull(factory.getRemoteInstance("dots://server/", Human.class));
	}

	public void remoteInstanceFactory_RemoteFactories() throws Exception {
		InstanceFactory factory = getRemoteInstanceFactory();
		Map<String, RemoteFactory> remoteFactories = Classes.getFieldValue(factory, "remoteFactories");

		assertNotNull(remoteFactories);
		assertFalse(remoteFactories.isEmpty());
		assertNotNull(remoteFactories.get("http"));
		assertNotNull(remoteFactories.get("https"));
		assertEquals(remoteFactories.get("http"), remoteFactories.get("https"));
		assertEquals("js.net.client.HttpRmiFactory", remoteFactories.get("http").getClass().getName());
	}

	@Test
	public void remoteInstanceFactory_RemoteFactoriesProvider() throws Exception {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader() {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (name.contains("RemoteFactoryProvider")) {
					name = "js/container/test/remote-factory-provider";
				}
				return super.getResources(name);
			}
		});
		try {
			InstanceFactory factory = getRemoteInstanceFactory();
			Map<String, RemoteFactory> remoteFactories = Classes.getFieldValue(factory, "remoteFactories");
			assertNotNull(remoteFactories.get("mock"));
			assertEquals("js.container.test.InstanceFactoryUnitTest$MockRemoteFactory", remoteFactories.get("mock").getClass().getName());
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	private static InstanceFactory getRemoteInstanceFactory() {
		return Classes.newInstance("js.container.RemoteInstanceFactory");
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static abstract class XMan {
		public XMan() {
		}
	}

	public static interface Human {
	}

	public static class Person implements Human {
		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Person(boolean exception) throws IOException {
			throw new IOException();
		}

		private Person(int age) {

		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private Constructor<?> constructor;
		private Class<?>[] interfaceClasses;

		@Override
		public Constructor<?> getConstructor() {
			return constructor;
		}

		@Override
		public Class<?>[] getInterfaceClasses() {
			return interfaceClasses;
		}

		@Override
		public Class<?> getInterfaceClass() {
			return interfaceClasses[0];
		}

		@Override
		public InstanceType getInstanceType() {
			return InstanceType.POJO;
		}

		@Override
		public String getImplementationURL() {
			return "http://localhost/";
		}
	}

	private static class MockManagedClassSPI_NullConstructor extends ManagedClassSpiStub {
		@Override
		public Constructor<?> getConstructor() {
			return null;
		}
	}

	// must be public to be instantiable by service loader
	public static class MockRemoteFactoryProvider implements RemoteFactoryProvider {
		private RemoteFactory factory = new MockRemoteFactory();

		@Override
		public String[] getProtocols() {
			return new String[] { "mock" };
		}

		@Override
		public RemoteFactory getRemoteFactory() {
			return factory;
		}
	}

	private static class MockRemoteFactory implements RemoteFactory {
		@Override
		public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) throws UnsupportedProtocolException {
			return null;
		}
	}
}
