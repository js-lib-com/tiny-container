package com.jslib.container.rmi;

import static com.jslib.container.rmi.HttpRmiServlet.className;
import static com.jslib.container.rmi.HttpRmiServlet.managedClass;
import static com.jslib.container.rmi.HttpRmiServlet.managedMethod;
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

import com.jslib.container.http.Resource;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.util.Classes;

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
	public void GivenValidClassPath_WhenPatternMatch_ThenFind() {
		Pattern requestPath = HttpRmiServlet.REQUEST_PATH_PATTERN;

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
	public void GivenValidClassPath_WhenGetClassName_ThenRetrieve() throws Exception {
		assertEquals("js.test.net.RmiController", className("/js/test/net/RmiController"));
		assertEquals("js.test.net.RmiController$Query", className("/js/test/net/RmiController/Query"));
		assertEquals("js.test.net.RmiController$Query$Item", className("/js/test/net/RmiController/Query/Item"));
	}

	@Test
	public void GivenExistingClass_WhenGetManagedClass_ThenRetrieve() throws Exception {
		// given

		// when
		IManagedClass<?> managedClass = managedClass(container, "java.lang.Object", "/java/lang/Object/toString");

		// then
		assertTrue(managedClass instanceof IManagedClass);
	}

	@Test(expected = ClassNotFoundException.class)
	public void GivenMispelledClassName_WhenGetManagedClass_ThenException() throws Exception {
		// given

		// when
		managedClass(container, "java.lang.FakeObject", "/java/lang/Object/toString");

		// then
	}

	@Test(expected = ClassNotFoundException.class)
	public void GivenMissingClass_WhenGetManagedClass_ThenException() throws Exception {
		// given
		when(container.getManagedClass(any())).thenReturn(null);

		// when
		managedClass(container, "java.lang.Object", "/java/lang/Object/toString");

		// then
	}

	@Test
	public void GivenExistingMethod_WhenGetManagedMethod_ThenRetrieve() throws Exception {
		// given

		// when
		IManagedMethod managedMethod = managedMethod(managedClass, "toString", "/java/lang/Object/toString");

		// then
		assertTrue(managedMethod instanceof IManagedMethod);
	}

	@Test(expected = NoSuchMethodException.class)
	public void GivenMissingMethod_WhenGetManagedMethod_ThenException() throws Exception {
		// given
		when(managedClass.getManagedMethod(any())).thenReturn(null);

		// when
		managedMethod(managedClass, "toString", "/java/lang/Object/toString");

		// then
	}

	@Test(expected = NoSuchMethodException.class)
	public void GivenResourceMethod_WhenGetManagedMethod_ThenException() throws Exception {
		// given
		when(managedMethod.getReturnType()).thenReturn(Resource.class);

		// when
		managedMethod(managedClass, "toString", "/java/lang/Object/toString");

		// then
	}
}
