package com.jslib.container.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.cdi.CDI;
import com.jslib.container.spi.IInstanceLifecycleListener;
import com.jslib.container.spi.ISecurityContext;
import com.jslib.lang.ConfigException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

@RunWith(MockitoJUnitRunner.class)
public class TinyContainerUnitTest {
	@Mock
	private ServletContext servletContext;
	@Mock
	private ServletContextEvent contextEvent;

	@Mock
	private HttpSession httpSession;
	@Mock
	private HttpSessionEvent httpSessionEvent;

	@Mock
	private HttpServletResponse httpResponse;

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
		when(contextEvent.getServletContext()).thenReturn(servletContext);
		when(servletContext.getContextPath()).thenReturn("/test");
		when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
		when(servletContext.getRealPath("")).thenReturn("src/test/resources");

		container = new TinyContainer();
		container.init(cdi);
	}

	@After
	public void afterTest() {
		container.close();
	}

	@Test
	public void Given_WhenGetInitParameter_Then() throws ConfigException {
		// given
		when(servletContext.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("name")));
		when(servletContext.getInitParameter("name")).thenReturn("Tom Joad");
		container.contextInitialized(contextEvent);

		// when
		String name = container.getInitParameter("name", String.class);

		// then
		assertThat(name, notNullValue());
	}
}
