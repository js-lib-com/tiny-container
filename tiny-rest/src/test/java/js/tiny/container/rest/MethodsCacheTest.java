package js.tiny.container.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ejb.Remote;
import jakarta.ws.rs.Path;
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
		when(managedClass.scanAnnotation(Path.class)).thenReturn(classPath);

		when(methodPath.value()).thenReturn("resource");
		when(managedMethod.scanAnnotation(Path.class)).thenReturn(methodPath);

		doReturn(managedClass).when(managedMethod).getDeclaringClass();
	}

	@Test
	public void GivenAppContext_WhenCreateStorageKey_ThenIncludeAppContext() throws Exception {
		// given
		when(methodPath.value()).thenReturn("sub-resource");

		// when
		List<String> key = PathMethodsCache.key(managedMethod);

		// then
		assertThat(key, contains("GET", "resource", "sub-resource"));
	}

	@Test
	public void GivenMissingClassPath_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given
		when(managedClass.scanAnnotation(Path.class)).thenReturn(null);
		when(methodPath.value()).thenReturn("sub-resource");

		// when
		List<String> key = PathMethodsCache.key(managedMethod);

		// then
		assertThat(key, contains("GET", "sub-resource"));
	}

	@Test
	public void GivenRootContext_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given
		when(classPath.value()).thenReturn("/");
		when(methodPath.value()).thenReturn("sub-resource");

		// when
		List<String> key = PathMethodsCache.key(managedMethod);

		// then
		assertThat(key, contains("GET", "sub-resource"));
	}

	@Test
	public void GivenMissingMethodPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(methodPath.value()).thenReturn(null);
		when(managedMethod.getName()).thenReturn("toString");

		// when
		List<String> key = PathMethodsCache.key(managedMethod);

		// then
		assertThat(key, contains("GET", "resource", "to-string"));
	}

	@Test
	public void GivenEmptyMethodPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(methodPath.value()).thenReturn("");
		when(managedMethod.getName()).thenReturn("toString");

		// when
		List<String> key = PathMethodsCache.key(managedMethod);

		// then
		assertThat(key, contains("GET", "resource", "to-string"));
	}

	@Test
	public void GivenWhitespaceMethodPath_WhenCreateStorageKey_ThenUseMethodName() {
		// given
		when(methodPath.value()).thenReturn("	 ");
		when(managedMethod.getName()).thenReturn("toString");

		// when
		List<String> key = PathMethodsCache.key(managedMethod);

		// then
		assertThat(key, contains("GET", "resource", "to-string"));
	}

	@Test
	public void GivenValidRequestPath_WhenCreateRetrieveKey_ThenValidKey() throws Exception {
		assertThat(PathMethodsCache.key("GET", "/resource/sub-resource?query"), contains("GET", "resource", "sub-resource"));
		assertThat(PathMethodsCache.key("GET", "/resource/sub-resource?"), contains("GET", "resource", "sub-resource"));
		assertThat(PathMethodsCache.key("GET", "/resource/sub-resource"), contains("GET", "resource", "sub-resource"));

		assertThat(PathMethodsCache.key("GET", "/resource/sub-resource.ext?query"), contains("GET", "resource", "sub-resource"));

		assertThat(PathMethodsCache.key("GET", "/sub-resource?query"), contains("GET", "sub-resource"));
		assertThat(PathMethodsCache.key("GET", "/sub-resource?"), contains("GET", "sub-resource"));
		assertThat(PathMethodsCache.key("GET", "/sub-resource"), contains("GET", "sub-resource"));
	}
}
