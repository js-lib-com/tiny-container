package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.cdi.CDI;
import js.tiny.container.spi.IInstanceLifecycleListener;
import js.tiny.container.spi.ISecurityContext;

@RunWith(MockitoJUnitRunner.class)
public class TinyContainerUnitTest {
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private ServletContextEvent servletContextEvent;
	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpSession httpSession;
	@Mock
	private HttpSessionEvent httpSessionEvent;

	@Mock
	private RequestContext requestContext;

	@Mock
	private CDI cdi;
	@Mock
	private IInstanceLifecycleListener instanceListener;

	@Mock
	private ISecurityContext securityProvider;

	private TinyContainer container;

	@AfterClass
	public static void afterClass() {
		new File("test-app").delete();
		new File("fixture/tomcat/work/Applications/test-app").delete();
	}

	@Before
	public void beforeTest() throws ConfigException {
		when(servletContextEvent.getServletContext()).thenReturn(servletContext);
		when(servletContext.getContextPath()).thenReturn("/test");
		when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
		when(servletContext.getRealPath("")).thenReturn("src/test/resources");

		when(httpSessionEvent.getSession()).thenReturn(httpSession);

		container = new TinyContainer(cdi);
	}

	@After
	public void afterTest() {
		container.close();
	}

	// --------------------------------------------------------------------------------------------
	// SERVLET CONTAINER LISTENERS

	@Test
	public void GivenServletContextEvent_WhenContextInitialized_ThenConfigureAndStart() throws ConfigException {
		// given
		TinyContainer containerSpy = spy(container);

		// when
		containerSpy.contextInitialized(servletContextEvent);

		// then
		verify(servletContext, times(1)).setAttribute(TinyContainer.ATTR_INSTANCE, containerSpy);
		verify(containerSpy, times(1)).configure(any(Config.class));
		verify(containerSpy, times(1)).start();
	}

	@Test(expected = RuntimeException.class)
	public void GivenConfigureException_WhenContextInitialized_ThenException() throws ConfigException {
		// given
		TinyContainer containerSpy = spy(container);
		doThrow(RuntimeException.class).when(containerSpy).configure(any(Config.class));

		// when
		containerSpy.contextInitialized(servletContextEvent);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenStartException_WhenContextInitialized_ThenException() {
		// given
		TinyContainer containerSpy = spy(container);
		doThrow(RuntimeException.class).when(containerSpy).start();

		// when
		containerSpy.contextInitialized(servletContextEvent);

		// then
	}

	@Test
	public void GivenServletContextEvent_WhenContextDestroyed_ThenClose() {
		// given
		TinyContainer containerSpy = spy(container);

		// when
		containerSpy.contextDestroyed(servletContextEvent);

		// then
		verify(containerSpy, times(1)).close();
	}

	@Test
	public void GivenCloseException_WhenContextDestroyed_ThenDumpExceptionToLogger() {
		// given
		TinyContainer containerSpy = spy(container);
		doThrow(RuntimeException.class).when(containerSpy).close();

		// when
		containerSpy.contextDestroyed(servletContextEvent);

		// then
		verify(containerSpy, times(1)).close();
	}

	@Test
	public void GivenHttpSessionEvent_WhneSessionCreated_ThenLogSessionId() {
		// given

		// when
		container.sessionCreated(httpSessionEvent);

		// then
		verify(httpSessionEvent, times(1)).getSession();
		verify(httpSession, times(1)).getId();
	}

	@Test
	public void GivenHttpSessionEvent_WhneSessionDestroyed_ThenLogSessionId() {
		// given

		// when
		container.sessionDestroyed(httpSessionEvent);

		// then
		verify(httpSessionEvent, times(1)).getSession();
		verify(httpSession, times(1)).getId();
	}

	@Test
	public void getAppName() throws ConfigException {
		assertEquals("test-app", container.getAppName());
	}
}
