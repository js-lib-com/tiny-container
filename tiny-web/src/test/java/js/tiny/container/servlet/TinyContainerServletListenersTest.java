package js.tiny.container.servlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.lang.ConfigException;

@RunWith(MockitoJUnitRunner.class)
public class TinyContainerServletListenersTest {
	@Mock
	private ServletContext servletContext;
	@Mock
	private ServletContextEvent contextEvent;
	
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private ServletRequestEvent requestEvent;

	@Mock
	private HttpSessionEvent sessionEvent;
	@Mock
	private HttpSession httpSession;
		
	private TinyContainer container;

	@Before
	public void beforeTest() {
		when(contextEvent.getServletContext()).thenReturn(servletContext);
		when(servletContext.getContextPath()).thenReturn("/context");
		when(servletContext.getRealPath(anyString())).thenReturn("src/test/resources");
		when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
		
		when(requestEvent.getServletRequest()).thenReturn(httpRequest);
		
		when(sessionEvent.getSession()).thenReturn(httpSession);
		
		container = new TinyContainer();
	}
	
	@Test
	public void Given_WhenContextInitialized_Then() {
		// given
		
		// when
		container.contextInitialized(contextEvent);
		
		// then
	}

	@Test
	public void GivenServletContextEvent_WhenContextInitialized_ThenConfigureAndStart() throws ConfigException {
		// given
		TinyContainer containerSpy = spy(container);

		// when
		containerSpy.contextInitialized(contextEvent);

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
		containerSpy.contextInitialized(contextEvent);

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenStartException_WhenContextInitialized_ThenException() {
		// given
		TinyContainer containerSpy = spy(container);
		doThrow(RuntimeException.class).when(containerSpy).start();

		// when
		containerSpy.contextInitialized(contextEvent);

		// then
	}

	@Test
	public void GivenServletContextEvent_WhenContextDestroyed_ThenClose() {
		// given
		TinyContainer containerSpy = spy(container);

		// when
		containerSpy.contextDestroyed(contextEvent);

		// then
		verify(containerSpy, times(1)).close();
	}

	@Test
	public void GivenCloseException_WhenContextDestroyed_ThenExceptionSwallowed() {
		// given
		TinyContainer containerSpy = spy(container);
		doThrow(RuntimeException.class).when(containerSpy).close();

		// when
		containerSpy.contextDestroyed(contextEvent);

		// then
		verify(containerSpy, times(1)).close();
	}

	@Test
	public void Given_WhenSessionCreated_ThenSessionId() {
		// given
		
		// when
		container.sessionCreated(sessionEvent);
		
		// then
		verify(sessionEvent, times(1)).getSession();
		verify(httpSession, times(1)).getId();
	}

	@Test
	public void Given_WhenSessionDestroyed_ThenSessionId() {
		// given
		
		// when
		container.sessionDestroyed(sessionEvent);
		
		// then
		verify(sessionEvent, times(1)).getSession();
		verify(httpSession, times(1)).getId();
	}

	@Test
	public void Given_WhenRequestInitialized_Then() {
		// given
		
		// when
		container.requestInitialized(requestEvent);
		
		// then
	}

	@Test
	public void Given_WhenRequestDestroyed_Then() {
		// given
		
		// when
		container.requestDestroyed(requestEvent);
		
		// then
	}
}
