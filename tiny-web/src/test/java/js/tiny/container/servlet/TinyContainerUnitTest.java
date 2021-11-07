package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IInstancePostConstructionListener;
import js.tiny.container.core.Container;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpServletResponseStub;
import js.tiny.container.unit.HttpSessionStub;
import js.tiny.container.unit.ServletContextStub;
import js.util.Classes;
import js.util.Files;

@SuppressWarnings({ "unused", "rawtypes" })
@RunWith(MockitoJUnitRunner.class)
public class TinyContainerUnitTest {

	@Mock
	private RequestContext requestContext;

	@Mock
	private IManagedClass<RequestContext> requestContextManagedClass;
	
	@Mock
	private CDI cdi;
	@Mock
	private IInstancePostConstructionListener instanceListener;
	
	@Mock
	private TinyConfigBuilder configBuilder;
	@Mock
	private SecurityContextProvider securityProvider;

	private TinyContainer container;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@AfterClass
	public static void afterClass() {
		new File("test-app").delete();
		new File("fixture/tomcat/work/Applications/test-app").delete();
	}

	@Before
	public void beforeTest() {
		//when(cdi.getInstance(RequestContext.class, instanceListener)).thenReturn(requestContext);
		
		container = new TinyContainer(cdi, configBuilder, securityProvider);
		
		when(requestContextManagedClass.getInterfaceClass()).thenReturn(RequestContext.class);
		container.config(requestContextManagedClass);
	}

	@Test
	public void constructor() {
		TinyContainer container = new TinyContainer();

		Map<Class<?>, IManagedClass> classesPool = Classes.getFieldValue(container, Container.class, "classesPool");
		assertNotNull(classesPool);
		assertTrue(classesPool.isEmpty());
	}

	private static void assertClass(String expected, Object object) {
		assertEquals(expected, object.getClass().getSimpleName());
	}

	@Test
	public void config_NoPrivateDir() throws ConfigException {
		String descriptor = "" + //
				"<test-app>" + //
				"</test-app>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		new File("test-app").delete();
		new File("fixture/tomcat/work/Applications/test-app").delete();

		TinyContainer container = new TinyContainer();
		container.config(builder.build());

		File privateDir = Classes.getFieldValue(container, "privateDir");
		assertNotNull(privateDir);
		assertTrue(privateDir.exists());
	}

	// --------------------------------------------------------------------------------------------
	// SERVLET CONTAINER LISTENERS

	@Test
	public void contextInitialized() {
		class MockContainer extends TinyContainer {
			int configProbe;
			int startProbe;

			@Override
			public void config(Config config) throws ConfigException {
				++configProbe;
			}

			@Override
			public void start() {
				++startProbe;
			}
		}

		MockServletContext servletContext = new MockServletContext();
		ServletContextEvent contextEvent = new ServletContextEvent(servletContext);

		MockContainer container = new MockContainer();
		container.contextInitialized(contextEvent);

		assertNotNull(servletContext.attributes.get(TinyContainer.ATTR_INSTANCE));
		assertTrue(servletContext.attributes.get(TinyContainer.ATTR_INSTANCE) instanceof TinyContainer);
		assertEquals(1, container.configProbe);
		assertEquals(1, container.startProbe);
	}

	@Test
	public void contextInitialized_Concurent() {
		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; ++i) {
			threads[i] = new Thread((new Runnable() {
				@Override
				public void run() {
					TinyContainer container = new TinyContainer();
					MockServletContext servletContext = new MockServletContext();
					ServletContextEvent contextEvent = new ServletContextEvent(servletContext);
					container.contextInitialized(contextEvent);
					assertNotNull(servletContext.attributes.get(TinyContainer.ATTR_INSTANCE));
					assertTrue(servletContext.attributes.get(TinyContainer.ATTR_INSTANCE) instanceof TinyContainer);
				}
			}));
		}

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			try {
				thread.join(10000);
			} catch (InterruptedException e) {
			}
		}
	}

	@Test
	public void contextInitialized_ConfigException() {
		class MockContainer extends TinyContainer {
			@Override
			public void config(Config config) throws ConfigException {
				throw new ConfigException("config exception");
			}
		}

		MockServletContext servletContext = new MockServletContext();
		ServletContextEvent contextEvent = new ServletContextEvent(servletContext);

		MockContainer container = new MockContainer();
		container.contextInitialized(contextEvent);
		assertNull(servletContext.attributes.get(TinyContainer.ATTR_INSTANCE));
	}

	@Test(expected = RuntimeException.class)
	public void contextInitialized_Throwable() {
		class MockContainer extends TinyContainer {
			@Override
			public void start() {
				throw new RuntimeException("runtime exception");
			}
		}

		MockServletContext servletContext = new MockServletContext();
		ServletContextEvent contextEvent = new ServletContextEvent(servletContext);

		MockContainer container = new MockContainer();
		container.contextInitialized(contextEvent);
		assertNull(servletContext.attributes.get(TinyContainer.ATTR_INSTANCE));
	}

	@Test
	public void contextDestroyed() {
		class MockContainer extends TinyContainer {
			int destroyProbe;

			@Override
			public void close() {
				++destroyProbe;
			}
		}

		MockServletContext servletContext = new MockServletContext();
		ServletContextEvent contextEvent = new ServletContextEvent(servletContext);

		MockContainer container = new MockContainer();
		container.contextDestroyed(contextEvent);
		assertEquals(1, container.destroyProbe);
	}

	@Test
	public void contextDestroyed_Throwable() {
		class MockContainer extends TinyContainer {
			@Override
			public void close() {
				throw new RuntimeException("runtime exception");
			}
		}

		MockServletContext servletContext = new MockServletContext();
		ServletContextEvent contextEvent = new ServletContextEvent(servletContext);

		MockContainer container = new MockContainer();
		container.contextDestroyed(contextEvent);
		// no assertion to test on exception beside context destroyed not throwing it
	}

	@Test
	public void sessionCreated() {
		MockHttpSession session = new MockHttpSession();
		HttpSessionEvent sessionEvent = new HttpSessionEvent(session);

		TinyContainer container = new TinyContainer();
		container.sessionCreated(sessionEvent);
		assertEquals(1, session.getIdProbe);
	}

	@Test
	public void sessionDestroyed() {
		MockHttpSession session = new MockHttpSession();
		HttpSessionEvent sessionEvent = new HttpSessionEvent(session);

		TinyContainer container = new TinyContainer();
		container.sessionDestroyed(sessionEvent);
		assertEquals(1, session.getIdProbe);
	}

	// --------------------------------------------------------------------------------------------
	// APPLICATION CONTEXT INTERFACE

	@Test
	public void getContextName() throws ConfigException {
		TinyContainer container = getContainer();
		assertEquals("test-app", container.getAppName());
	}

	@Test
	public void getAppFile() throws ConfigException {
		TinyContainer container = getContainer();
		assertTrue(Files.path2unix(container.getAppFile("file").getPath()).endsWith("test-app/file"));
	}

	@Test
	public void getProperty() throws ConfigException {
		TinyContainer container = new TinyContainer();
		container.contextInitialized(new ServletContextEvent(new MockServletContext()));
		assertEquals("/server/base/dir", container.getProperty("server.base.dir", String.class));
	}

	@Test
	public void getProperty_NotDefined() throws ConfigException {
		TinyContainer container = new TinyContainer();
		container.contextInitialized(new ServletContextEvent(new MockServletContext()));
		assertNull(container.getProperty("not.defined", String.class));
	}

	@Test
	public void getTypeProperty() throws ConfigException {
		TinyContainer container = new TinyContainer();
		container.contextInitialized(new ServletContextEvent(new MockServletContext()));
		assertEquals(new File("/server/base/dir"), container.getProperty("server.base.dir", File.class));
	}

	@Test
	public void getTypeProperty_NotDefined() throws ConfigException {
		TinyContainer container = new TinyContainer();
		container.contextInitialized(new ServletContextEvent(new MockServletContext()));
		assertNull(container.getProperty("not.defined", File.class));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static TinyContainer getContainer() throws ConfigException {
		return getContainer("");
	}

	private static TinyContainer getContainer(String descriptor) throws ConfigException {
		String config = "<?xml version='1.0' ?>" + //
				"<test-app>" + //
				"   <managed-classes>" + //
				descriptor + //
				"   </managed-classes>" + //
				"</test-app>";
		ConfigBuilder builder = new ConfigBuilder(config);
		TinyContainer container = new TinyContainer();
		container.config(builder.build());
		return container;
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockServletContext extends ServletContextStub {
		private Map<String, Object> attributes = new HashMap<>();

		@Override
		public String getContextPath() {
			return "/test-app";
		}

		@Override
		public String getRealPath(String resource) {
			return new File(new File("fixture/tomcat/webapps/test-app/"), resource).getPath();
		}

		@Override
		public Enumeration getInitParameterNames() {
			return Collections.enumeration(Arrays.asList("server.base.dir"));
		}

		@Override
		public String getInitParameter(String name) {
			return "/server/base/dir";
		}

		@Override
		public void setAttribute(String name, Object value) {
			attributes.put(name, value);
		}
	}

	private static class MockRequestContext extends RequestContext {
		private MockHttpServletRequest request = new MockHttpServletRequest();
		private MockHttpServletResponse response = new MockHttpServletResponse();

		public MockRequestContext(ITinyContainer container) {
			super(container);
		}

		@Override
		public Locale getLocale() {
			return Locale.ENGLISH;
		}

		@Override
		public String getRemoteHost() {
			return "1.2.3.4";
		}

		@Override
		public HttpServletRequest getRequest() {
			return request;
		}

		@Override
		public HttpServletResponse getResponse() {
			return response;
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private MockHttpSession session = new MockHttpSession();

		private boolean exception;

		private int loginProbe;
		private String loginUsername;
		private String loginPassword;

		private int logoutProbe;

		private int getSessionProbe;

		@Override
		public void login(String username, String password) throws ServletException {
			if (exception) {
				throw new ServletException();
			}
			++loginProbe;
			loginUsername = username;
			loginPassword = password;
		}

		@Override
		public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
			return true;
		}

		@Override
		public void logout() throws ServletException {
			if (exception) {
				throw new ServletException();
			}
			++logoutProbe;
		}

		@Override
		public HttpSession getSession(boolean create) {
			++getSessionProbe;
			return session;
		}

		@Override
		public HttpSession getSession() {
			++getSessionProbe;
			return session;
		}

		@Override
		public Principal getUserPrincipal() {
			if (loginUsername != null && loginPassword != null) {
				return new Principal() {
					@Override
					public String getName() {
						return loginUsername;
					}
				};
			}
			return null;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
	}

	private static class MockHttpSession extends HttpSessionStub {
		private boolean exception;
		private Map<String, Object> attributes = new HashMap<>();

		private int setAttributeProbe;
		private int removeAttributeProbe;

		private int setMaxInactiveIntervalProbe;
		private int maxInactiveInterval;

		private int invalidateProbe;

		private int getIdProbe;

		@Override
		public void setAttribute(String name, Object value) {
			if (exception) {
				throw new IllegalStateException("exception");
			}
			++setAttributeProbe;
			attributes.put(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			if (exception) {
				throw new IllegalStateException("exception");
			}
			return attributes.get(name);
		}

		@Override
		public void removeAttribute(String name) {
			++removeAttributeProbe;
			attributes.remove(name);
		}

		@Override
		public void setMaxInactiveInterval(int maxInactiveInterval) {
			++setMaxInactiveIntervalProbe;
			this.maxInactiveInterval = maxInactiveInterval;
		}

		@Override
		public void invalidate() {
			if (exception) {
				throw new IllegalStateException("exception");
			}
			++invalidateProbe;
		}

		@Override
		public String getId() {
			++getIdProbe;
			return "session";
		}
	}

	// --------------------------------------------------------------------------------------------

	private static interface Pojo {
		String getString();
	}

	private static class PojoImpl implements Pojo {
		@Override
		public String getString() {
			return "string";
		}
	}
}
