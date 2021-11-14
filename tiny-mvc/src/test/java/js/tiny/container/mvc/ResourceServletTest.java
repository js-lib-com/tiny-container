package js.tiny.container.mvc;

import static org.mockito.Mockito.when;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServletTest {
	@Mock
	private ServletConfig servletConfig;
	@Mock
	private ServletContext servletContext;

	@Mock
	private ITinyContainer container;
	@Mock
	private IContainerService service;
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Before
	public void beforeTest() {
		when(servletConfig.getServletName()).thenReturn("resource-servlet");
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getServletContextName()).thenReturn("test-app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);
	}

	@Test
	public void GivenResourceMethod_WhenServletInit_ThenRegister() throws ServletException {
		// given

		// when
		ResourceServlet servlet = new ResourceServlet();
		servlet.init(servletConfig);

		// then
	}

	@Test
	public void GivenNotResourceMethod_WhenServletInit_ThenNotRegister() throws ServletException {
		// given

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
}
