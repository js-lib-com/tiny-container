package js.tiny.container.mvc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.mvc.annotation.RequestPath;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class MethodsCacheTest {
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Before
	public void beforeTest() {
		doReturn(managedClass).when(managedMethod).getDeclaringClass();
		when(managedMethod.getName()).thenReturn("resource");
	}
	
	@Test
	public void GivenAppContext_WhenCreateStorageKey_ThenIncludeAppContext() throws Exception {
		// given
		RequestPath requestPath = mock(RequestPath.class);
		when(requestPath.value()).thenReturn("resource");
		when(managedMethod.getAnnotation(RequestPath.class)).thenReturn(requestPath);

		Controller controller = mock(Controller.class);
		when(controller.value()).thenReturn("controller");
		when(managedClass.getAnnotation(Controller.class)).thenReturn(controller);

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/controller/resource", key);
	}

	@Test
	public void GivenRootContext_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given
		Controller controller = mock(Controller.class);
		when(controller.value()).thenReturn("");
		when(managedClass.getAnnotation(Controller.class)).thenReturn(controller);

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/resource", key);
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

	@Test
	public void Given_When_Then() {
		// given
		
		// when
		
		// then
	}
}
