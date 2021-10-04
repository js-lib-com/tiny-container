package js.tiny.container.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.http.Resource;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServletUnitTest {
	@Mock
	private ServletConfig servletConfig;
	@Mock
	private ServletContext servletContext;

	@Mock
	private ITinyContainer container;
	@Mock
	private IManagedClass managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Before
	public void beforeTest() {
		when(servletConfig.getServletName()).thenReturn("resource-servlet");
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getServletContextName()).thenReturn("test-app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(container.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedClass.getServiceMeta(ControllerMeta.class)).thenReturn(new ControllerMeta("controller"));
		when(managedMethod.getDeclaringClass()).thenReturn(managedClass);
		when(managedMethod.getName()).thenReturn("resource");
	}

	@Test
	public void GivenDefault_WhenConstructor_ThenNotNullState() {
		ResourceServlet servlet = new ResourceServlet();
		assertNotNull(Classes.getFieldValue(servlet, "argumentsReaderFactory"));
		assertNotNull(Classes.getFieldValue(servlet, "resourceMethods"));
	}

	@Test
	public void GivenResourceMethod_WhenServletInit_ThenRegister() throws ServletException {
		// given
		when(managedMethod.getServiceMeta(RequestPathMeta.class)).thenReturn(new RequestPathMeta("index"));
		when(managedMethod.getReturnType()).thenReturn(Resource.class);

		// when
		ResourceServlet servlet = new ResourceServlet();
		servlet.init(servletConfig);

		// then
		Map<String, IManagedMethod> methods = Classes.getFieldValue(servlet, "resourceMethods");
		assertNotNull(methods);
		assertEquals(1, methods.size());
		assertNotNull(methods.get("/controller/index"));
		assertEquals("index", methods.get("/controller/index").getServiceMeta(RequestPathMeta.class).value());
	}

	@Test
	public void GivenNotResourceMethod_WhenServletInit_ThenNotRegister() throws ServletException {
		// given
		when(managedMethod.getReturnType()).thenReturn(void.class);

		// when
		ResourceServlet servlet = new ResourceServlet();
		servlet.init(servletConfig);

		// then
	}

	@Test(expected = UnavailableException.class)
	public void GivenNoContainerAttribute_WhenServletInit_ThenException() throws ServletException {
		// given
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(null);

		// when
		ResourceServlet servlet = new ResourceServlet();
		servlet.init(servletConfig);

		// then
	}

	@Test
	public void GivenAppContext_WhenCreateStorageKey_ThenIncludeAppContext() throws Exception {
		// given
		when(managedMethod.getServiceMeta(RequestPathMeta.class)).thenReturn(new RequestPathMeta("resource"));

		// when
		String key = key(managedMethod);

		// then
		assertEquals("/controller/resource", key);
	}

	@Test
	public void GivenRootContext_WhenCreateStorageKey_ThenNoAppContext() throws Exception {
		// given
		when(managedClass.getServiceMeta(ControllerMeta.class)).thenReturn(new ControllerMeta(""));

		// when
		String key = key(managedMethod);

		// then
		assertEquals("/resource", key);
	}

	@Test
	public void GivenValidRequestPath_WhenCreateRetrieveKey_ThenValidKey() throws Exception {
		assertEquals("/controller/resource", key("/controller/resource.xsp?query"));
		assertEquals("/controller/resource", key("/controller/resource.xsp?"));
		assertEquals("/controller/resource", key("/controller/resource.xsp"));
		assertEquals("/controller/resource", key("/controller/resource."));
		assertEquals("/controller/resource", key("/controller/resource"));
		assertEquals("/controller/resource", key("/controller/resource?query"));
		assertEquals("/controller/resource", key("/controller/resource?"));

		assertEquals("/resource", key("/resource.xsp?query"));
		assertEquals("/resource", key("/resource.xsp?"));
		assertEquals("/resource", key("/resource.xsp"));
		assertEquals("/resource", key("/resource."));
		assertEquals("/resource", key("/resource"));
		assertEquals("/resource", key("/resource?query"));
		assertEquals("/resource", key("/resource?"));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String key(IManagedMethod resourceMethod) throws Exception {
		return Classes.invoke(ResourceServlet.class, "key", resourceMethod);
	}

	private static String key(String requestPath) throws Exception {
		return Classes.invoke(ResourceServlet.class, "key", requestPath);
	}
}
