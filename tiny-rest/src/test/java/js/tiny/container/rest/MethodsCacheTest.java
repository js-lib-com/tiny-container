package js.tiny.container.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import javax.ejb.Remote;
import javax.ws.rs.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class MethodsCacheTest {
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Mock
	private Remote remote;
	@Mock
	private Path classPath;
	@Mock
	private Path methodPath;

	@Before
	public void beforeTest() {
		when(classPath.value()).thenReturn("resource");
		when(managedClass.getAnnotation(Path.class)).thenReturn(classPath);

		when(methodPath.value()).thenReturn("resource");
		when(managedMethod.scanAnnotation(Path.class)).thenReturn(methodPath);

		doReturn(managedClass).when(managedMethod).getDeclaringClass();
	}

	@Test
	public void GivenAppContext_WhenCreateStorageKey_ThenIncludeAppContext() throws Exception {
		// given
		when(methodPath.value()).thenReturn("sub-resource");


		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/resource/sub-resource", key);
	}

	@Test
	public void GivenRootContext_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given
		when(managedClass.getAnnotation(Path.class)).thenReturn(null);
		when(methodPath.value()).thenReturn("sub-resource");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/sub-resource", key);
	}

	@Test
	public void GivenMissingMethodPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(methodPath.value()).thenReturn(null);
		when(managedMethod.getName()).thenReturn("toString");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/resource/to-string", key);
	}

	@Test
	public void GivenEmptyMethodPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(methodPath.value()).thenReturn("");
		when(managedMethod.getName()).thenReturn("toString");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/resource/to-string", key);
	}

	@Test
	public void GivenWhitespaceMethodPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(methodPath.value()).thenReturn("	 ");
		when(managedMethod.getName()).thenReturn("toString");

		// when
		String key = MethodsCache.key(managedMethod);

		// then
		assertEquals("/resource/to-string", key);
	}

	@Test
	public void GivenValidRequestPath_WhenCreateRetrieveKey_ThenValidKey() throws Exception {
		assertEquals("/resource/sub-resource", MethodsCache.key("/resource/sub-resource?query"));
		assertEquals("/resource/sub-resource", MethodsCache.key("/resource/sub-resource?"));
		assertEquals("/resource/sub-resource", MethodsCache.key("/resource/sub-resource"));

		assertEquals("/resource/sub-resource", MethodsCache.key("/resource/sub-resource.ext?query"));

		assertEquals("/sub-resource", MethodsCache.key("/sub-resource?query"));
		assertEquals("/sub-resource", MethodsCache.key("/sub-resource?"));
		assertEquals("/sub-resource", MethodsCache.key("/sub-resource"));
	}
}
