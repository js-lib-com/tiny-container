package js.tiny.container.net.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

import org.junit.Test;

import js.tiny.container.core.Container;
import js.tiny.container.http.Resource;
import js.tiny.container.net.HttpRmiServlet;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.util.Classes;

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
		IManagedClass<?> managedClass = getManagedClass(container, "java.lang.Object", "/java/lang/Object/toString");
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

	@Test
	public void getManagedMethod() throws Exception {
		MockManagedClass<?> managedClass = new MockManagedClass<>();
		IManagedMethod managedMethod = getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
		assertTrue(managedMethod instanceof MockManagedMethod);
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_NoManagedMethod() throws Exception {
		MockManagedClass<?> managedClass = new MockManagedClass<>();
		managedClass.managedMethod = null;
		getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_IsResource() throws Exception {
		MockManagedClass<?> managedClass = new MockManagedClass<>();
		managedClass.managedMethod.returnType = Resource.class;
		getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
	}
	
	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String className(String classPath) throws Exception {
		return Classes.invoke(HttpRmiServlet.class, "className", classPath);
	}

	private static IManagedClass<?> getManagedClass(IContainer container, String interfaceName, String requestURI) throws Exception {
		return Classes.invoke(HttpRmiServlet.class, "getManagedClass", container, interfaceName, requestURI);
	}

	private static IManagedMethod getManagedMethod(IManagedClass<?> managedClass, String methodName, String requestURI) throws Exception {
		return Classes.invoke(HttpRmiServlet.class, "getManagedMethod", managedClass, methodName, requestURI);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends Container {
		private MockManagedClass<?> managedClass = new MockManagedClass<>();

		@SuppressWarnings("unchecked")
		@Override
		public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
			return (IManagedClass<T>) managedClass;
		}
	}

	private static class MockManagedClass<T> extends ManagedClassSpiStub<T> {
		private MockManagedMethod managedMethod = new MockManagedMethod();

		@SuppressWarnings("unchecked")
		@Override
		public Class<T> getInterfaceClass() {
			return (Class<T>) Object.class;
		}

		@Override
		public IManagedMethod getManagedMethod(String methodName) {
			return managedMethod;
		}
	}

	private static class MockManagedMethod extends ManagedMethodSpiStub {
		private Type returnType = void.class;

		@Override
		public Type getReturnType() {
			return returnType;
		}
	}
}
