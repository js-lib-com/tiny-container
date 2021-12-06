package js.tiny.container.servlet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TinyContainerServletListenersTest {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@Mock
	private ServletContextEvent contextEvent;
	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpSessionEvent sessionEvent;
	@Mock
	private HttpSession httpSession;
		
	private TinyContainer container;

	@Before
	public void beforeTest() {
		when(contextEvent.getServletContext()).thenReturn(servletContext);
		when(servletContext.getContextPath()).thenReturn("/context");
		when(servletContext.getRealPath(anyString())).thenReturn(".");
		when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
		when(sessionEvent.getSession()).thenReturn(httpSession);
		when(httpSession.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
		
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
	public void Given_WhenContextDestroyed_Then() {
		// given
		
		// when
		container.contextDestroyed(contextEvent);
		
		// then
	}

	@Test
	public void Given_WhenSessionCreated_Then() {
		// given
		
		// when
		container.sessionCreated(sessionEvent);
		
		// then
	}

	@Test
	public void Given_WhenSessionDestroyed_Then() {
		// given
		
		// when
		container.sessionDestroyed(sessionEvent);
		
		// then
	}
}
