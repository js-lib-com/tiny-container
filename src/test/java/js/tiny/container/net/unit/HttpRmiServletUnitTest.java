package js.tiny.container.net.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

import js.tiny.container.ContainerSPI;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.http.Resource;
import js.tiny.container.net.HttpRmiServlet;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.util.Classes;

import org.junit.Test;

public class HttpRmiServletUnitTest {
	@Test
	public void constructor() {
		HttpRmiServlet servlet = new HttpRmiServlet();
		assertNotNull(Classes.getFieldValue(servlet, "argumentsReaderFactory"));
		assertNotNull(Classes.getFieldValue(servlet, "valueWriterFactory"));
	}

	@Test
	public void requestPathPattern() {
		Pattern requestPath = Classes.getFieldValue(HttpRmiServlet.class, "REQUEST_PATH_PATTERN");

		assertTrue(requestPath.matcher("/package/name/Class/method").find());
		assertTrue(requestPath.matcher("/package/name/Class/InnerClass/method").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.1").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.rmi").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.rmi?q=query").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.rmi?q").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.rmi?").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.RMI").find());
		assertTrue(requestPath.matcher("/package/name/Class/method.fake").find());

		assertFalse(requestPath.matcher("package/name/Class/method").find());
		assertFalse(requestPath.matcher("/package/name/Class/method/").find());
		assertFalse(requestPath.matcher("/package/name/Class/method/innerMethod").find());
		assertFalse(requestPath.matcher("/Class/method").find());
		assertFalse(requestPath.matcher("/package/name/Class/").find());
		assertFalse(requestPath.matcher("/Package/name/Class/method").find());
		assertFalse(requestPath.matcher("/package/name/Class/Method").find());
	}

	@Test
	public void className() throws Exception {
		assertEquals("js.test.net.RmiController", className("/js/test/net/RmiController"));
		assertEquals("js.test.net.RmiController$Query", className("/js/test/net/RmiController/Query"));
		assertEquals("js.test.net.RmiController$Query$Item", className("/js/test/net/RmiController/Query/Item"));
	}

	@Test
	public void getManagedClass() throws Exception {
		MockContainer container = new MockContainer();
		ManagedClassSPI managedClass = getManagedClass(container, "java.lang.Object", "/java/lang/Object/toString");
		assertTrue(managedClass instanceof MockManagedClass);
	}

	@Test(expected = ClassNotFoundException.class)
	public void getManagedClass_NoInterfaceClass() throws Exception {
		MockContainer container = new MockContainer();
		getManagedClass(container, "fake.interface.Class", "/java/lang/Object/toString");
	}

	@Test(expected = ClassNotFoundException.class)
	public void getManagedClass_NoManagedClass() throws Exception {
		MockContainer container = new MockContainer();
		container.managedClass = null;
		getManagedClass(container, "java.lang.Object", "/java/lang/Object/toString");
	}

	@Test(expected = ClassNotFoundException.class)
	public void getManagedClass_NoRemotelyAccessible() throws Exception {
		MockContainer container = new MockContainer();
		container.managedClass.remotelyAccessible = false;
		getManagedClass(container, "java.lang.Object", "/java/lang/Object/toString");
	}

	@Test
	public void getManagedMethod() throws Exception {
		MockManagedClass managedClass = new MockManagedClass();
		ManagedMethodSPI managedMethod = getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
		assertTrue(managedMethod instanceof MockManagedMethod);
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_NoManagedMethod() throws Exception {
		MockManagedClass managedClass = new MockManagedClass();
		managedClass.managedMethod = null;
		getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_NoRemotelyAccessible() throws Exception {
		MockManagedClass managedClass = new MockManagedClass();
		managedClass.managedMethod.remotelyAccessible = false;
		getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_IsResource() throws Exception {
		MockManagedClass managedClass = new MockManagedClass();
		managedClass.managedMethod.returnType = Resource.class;
		getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
	}
	
	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String className(String classPath) throws Exception {
		return Classes.invoke(HttpRmiServlet.class, "className", classPath);
	}

	private static ManagedClassSPI getManagedClass(ContainerSPI container, String interfaceName, String requestURI) throws Exception {
		return Classes.invoke(HttpRmiServlet.class, "getManagedClass", container, interfaceName, requestURI);
	}

	private static ManagedMethodSPI getManagedMethod(ManagedClassSPI managedClass, String methodName, String requestURI) throws Exception {
		return Classes.invoke(HttpRmiServlet.class, "getManagedMethod", managedClass, methodName, requestURI);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends ContainerStub {
		private MockManagedClass managedClass = new MockManagedClass();

		@Override
		public ManagedClassSPI getManagedClass(Class<?> interfaceClass) {
			return managedClass;
		}
	}

	private static class MockManagedClass extends ManagedClassSpiStub {
		private boolean remotelyAccessible = true;
		private MockManagedMethod managedMethod = new MockManagedMethod();

		@Override
		public Class<?> getInterfaceClass() {
			return Object.class;
		}

		@Override
		public boolean isRemotelyAccessible() {
			return remotelyAccessible;
		}

		@Override
		public ManagedMethodSPI getNetMethod(String methodName) {
			return managedMethod;
		}
	}

	private static class MockManagedMethod extends ManagedMethodSpiStub {
		private boolean remotelyAccessible = true;
		private Type returnType = void.class;

		@Override
		public boolean isRemotelyAccessible() {
			return remotelyAccessible;
		}

		@Override
		public Type getReturnType() {
			return returnType;
		}
	}
}
