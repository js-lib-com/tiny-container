package js.tiny.container.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.http.Resource;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class HttpRmiServletUnitTest {
	@Mock
	private IContainer container; 
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;
	
	@Before
	public void beforeTest() {
		doReturn(managedClass).when(container).getManagedClass(any());
		when(managedClass.getManagedMethod(any())).thenReturn(managedMethod);
	}
	
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
		IManagedClass<?> managedClass = getManagedClass(container, "java.lang.Object", "/java/lang/Object/toString");
		assertTrue(managedClass instanceof IManagedClass);
	}

	@Test(expected = ClassNotFoundException.class)
	public void getManagedClass_NoInterfaceClass() throws Exception {
		getManagedClass(container, "fake.interface.Class", "/java/lang/Object/toString");
	}

	@Test(expected = ClassNotFoundException.class)
	public void getManagedClass_NoManagedClass() throws Exception {
		when(container.getManagedClass(any())).thenReturn(null);
		getManagedClass(container, "java.lang.Object", "/java/lang/Object/toString");
	}

	@Test
	public void getManagedMethod() throws Exception {
		IManagedMethod managedMethod = getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
		assertTrue(managedMethod instanceof IManagedMethod);
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_NoManagedMethod() throws Exception {
		when(managedClass.getManagedMethod(any())).thenReturn(null);
		getManagedMethod(managedClass, "toString", "/java/lang/Object/toString");
	}

	@Test(expected = NoSuchMethodException.class)
	public void getManagedMethod_IsResource() throws Exception {
		when(managedMethod.getReturnType()).thenReturn(Resource.class);
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
}
