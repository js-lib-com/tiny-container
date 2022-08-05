package com.jslib.container.mvc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.mvc.annotation.Controller;
import com.jslib.container.mvc.annotation.RequestPath;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class MethodsCacheTest {
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private RequestPath requestPath;

	@Before
	public void beforeTest() {
		doReturn(managedClass).when(managedMethod).getDeclaringClass();
		when(managedMethod.getName()).thenReturn("resource");
		when(managedMethod.scanAnnotation(RequestPath.class)).thenReturn(requestPath);
	}

	@Test
	public void GivenAppContext_WhenCreateStorageKey_ThenIncludeAppContext() throws Exception {
		// given
		when(requestPath.value()).thenReturn("resource");

		Controller controller = mock(Controller.class);
		when(controller.value()).thenReturn("controller");
		when(managedClass.scanAnnotation(Controller.class)).thenReturn(controller);

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/controller/resource", key);
	}

	@Test
	public void GivenRootContext_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/resource", key);
	}

	@Test
	public void GivenMissingRequestPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(requestPath.value()).thenReturn(null);
		when(managedMethod.getName()).thenReturn("toString");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/to-string", key);
	}

	@Test
	public void GivenEmptyRequestPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(requestPath.value()).thenReturn("");
		when(managedMethod.getName()).thenReturn("toString");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/to-string", key);
	}

	@Test
	public void GivenWhitespaceRequestPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(requestPath.value()).thenReturn("	 ");
		when(managedMethod.getName()).thenReturn("toString");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/to-string", key);
	}

	@Test
	public void GivenValidRequestPath_WhenCreateRetrieveKey_ThenValidKey() throws Exception {
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource.xsp?query"));
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource.xsp?"));
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource.xsp"));
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource."));
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource"));
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource?query"));
		assertEquals("/controller/resource", MethodsCache.key("/controller/resource?"));

		assertEquals("/resource", MethodsCache.key("/resource.xsp?query"));
		assertEquals("/resource", MethodsCache.key("/resource.xsp?"));
		assertEquals("/resource", MethodsCache.key("/resource.xsp"));
		assertEquals("/resource", MethodsCache.key("/resource."));
		assertEquals("/resource", MethodsCache.key("/resource"));
		assertEquals("/resource", MethodsCache.key("/resource?query"));
		assertEquals("/resource", MethodsCache.key("/resource?"));
	}
}
