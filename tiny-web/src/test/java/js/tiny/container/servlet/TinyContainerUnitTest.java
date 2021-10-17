package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import org.junit.BeforeClass;
import org.junit.Test;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.tiny.container.cdi.ScopeFactory;
import js.tiny.container.core.Container;
import js.tiny.container.core.InstanceScope;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.NonceUser;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpServletResponseStub;
import js.tiny.container.unit.HttpSessionStub;
import js.tiny.container.unit.ServletContextStub;
import js.util.Classes;
import js.util.Files;

@SuppressWarnings({ "unused", "rawtypes" })
public class TinyContainerUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@AfterClass
	public static void afterClass() {
		new File("test-app").delete();
		new File("fixture/tomcat/work/Applications/test-app").delete();
	}

	@Test
	public void constructor() {
		TinyContainer container = new TinyContainer();

		Map<InstanceScope, ScopeFactory> scopeFactories = Classes.getFieldValue(container, Container.class, "scopeFactories");
		assertNotNull(scopeFactories);
		assertEquals(4, scopeFactories.size());
		assertClass("ApplicationScopeFactory", scopeFactories.get(InstanceScope.APPLICATION));
		assertClass("ThreadScopeFactory", scopeFactories.get(InstanceScope.THREAD));
		assertClass("SessionScopeFactory", scopeFactories.get(InstanceScope.SESSION));
		assertNull(scopeFactories.get(InstanceScope.LOCAL));

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

	@Test
	public void config_Login() throws ConfigException {
		String descriptor = "" + //
				"<test-app>" + //
				"	<login>" + //
				"		<property name='realm' value='Fax2e-mail' />" + //
				"		<property name='page' value='login-form.htm' />" + //
				"	</login>" + //
				"</test-app>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		TinyContainer container = new TinyContainer();
		container.config(builder.build());

		assertEquals("Fax2e-mail", container.getLoginRealm());
		assertEquals("/test-app/login-form.htm", container.getLoginPage());
	}

	@Test
	public void config_Login_AbsolutePage() throws ConfigException {
		String descriptor = "" + //
				"<test-app>" + //
				"	<login>" + //
				"		<property name='realm' value='Fax2e-mail' />" + //
				"		<property name='page' value='/app/login-form.htm' />" + //
				"	</login>" + //
				"</test-app>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		TinyContainer container = new TinyContainer();
		container.config(builder.build());

		assertEquals("Fax2e-mail", container.getLoginRealm());
		assertEquals("/app/login-form.htm", container.getLoginPage());
	}

	@Test
	public void config_Login_NullPage() throws ConfigException {
		String descriptor = "" + //
				"<test-app>" + //
				"	<login>" + //
				"		<property name='realm' value='Fax2e-mail' />" + //
				"	</login>" + //
				"</test-app>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		TinyContainer container = new TinyContainer();
		container.config(builder.build());

		assertEquals("Fax2e-mail", container.getLoginRealm());
		assertNull(container.getLoginPage());
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
			public void destroy() {
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
			public void destroy() {
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

	@Test
	public void getRequestLocale() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);
		assertEquals(Locale.ENGLISH, container.getRequestLocale());
	}

	@Test
	public void getRemoteAddr() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);
		assertEquals("1.2.3.4", container.getRemoteAddr());
	}

	// --------------------------------------------------------------------------------------------
	// SECURITY CONTEXT INTERFACE

	/**
	 * Login for servlet container provided authentication should send username and password to HTTP request and should not
	 * store anything on session attributes.
	 */
	@Test
	public void login_ServletContainer() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		assertTrue(container.login("username", "password"));

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;

		assertEquals(1, request.loginProbe);
		assertEquals("username", request.loginUsername);
		assertEquals("password", request.loginPassword);
		assertNull(request.session.attributes.get("principal"));
	}

	@Test
	public void login_ServletContainer_Fail() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		request.exception = true;

		assertFalse(container.login("username", "password"));

		assertEquals(0, request.loginProbe);
		assertNull(request.loginUsername);
		assertNull(request.loginPassword);
		assertNull(request.session.attributes.get("principal"));
	}

	@Test
	public void login_Application() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		class User implements Principal {
			@Override
			public String getName() {
				return "username";
			}
		}

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;

		container.login(new User());

		assertEquals(0, request.loginProbe);
		assertNull(request.loginUsername);
		assertNull(request.loginPassword);

		Object user = request.session.attributes.get(TinyContainer.ATTR_PRINCIPAL);
		assertNotNull(user);
		assertTrue(user instanceof User);
		assertEquals("username", ((User) user).getName());
	}

	@Test
	public void login_Application_Nonce() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		MockHttpSession session = request.session;

		container.login(new NonceUser(10));

		assertEquals(1, session.setMaxInactiveIntervalProbe);
		assertEquals(10, session.maxInactiveInterval);

		assertEquals(0, request.loginProbe);
		assertNull(request.loginUsername);
		assertNull(request.loginPassword);

		Object user = session.attributes.get(TinyContainer.ATTR_PRINCIPAL);
		assertNotNull(user);
		assertTrue(user instanceof NonceUser);
		assertEquals("nonce", ((NonceUser) user).getName());
	}

	@Test
	public void login_Application_InvalidateException() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		class User implements Principal {
			@Override
			public String getName() {
				return "username";
			}
		}

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		MockHttpSession session = request.session;
		session.exception = true;

		container.login(new User());

		assertEquals(0, request.loginProbe);
		assertNull(request.loginUsername);
		assertNull(request.loginPassword);
		assertNull(request.session.attributes.get("principal"));
	}

	@Test
	public void logout() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		MockHttpSession session = request.session;

		container.logout();

		assertEquals(1, request.logoutProbe);
		assertEquals(1, session.removeAttributeProbe);
		assertEquals(1, session.invalidateProbe);
		assertNull(session.attributes.get("principal"));
	}

	@Test
	public void logout_OutsideSession() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		request.session = null;

		container.logout();
		assertEquals(1, request.logoutProbe);
	}

	@Test
	public void logout_ServletException() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		request.exception = true;
		MockHttpSession session = request.session;

		container.logout();

		assertEquals(0, request.logoutProbe);
		assertEquals(1, session.removeAttributeProbe);
		assertEquals(1, session.invalidateProbe);
		assertNull(session.attributes.get("principal"));
	}

	@Test
	public void logout_InvalidateException() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;
		MockHttpSession session = request.session;
		session.exception = true;

		container.logout();

		assertEquals(1, request.logoutProbe);
		assertEquals(1, session.removeAttributeProbe);
		assertEquals(0, session.invalidateProbe);
		assertNull(session.attributes.get("principal"));
	}

	@Test(expected = BugError.class)
	public void login_OutsideHttpRequest() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		context.request = null;

		container.login(new NonceUser(10));
	}

	@Test(expected = BugError.class)
	public void logout_OutsideHttpRequest() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		context.request = null;

		container.logout();
	}

	@Test
	public void getUserPrincipal_Application() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);
		container.login("username", "passsword");

		Principal principal = container.getUserPrincipal();
		assertNotNull(principal);
		assertEquals("username", principal.getName());
	}

	@Test
	public void getUserPrincipal_Container() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		class User implements Principal {
			@Override
			public String getName() {
				return "username";
			}
		}
		container.login(new User());

		Principal principal = container.getUserPrincipal();
		assertNotNull(principal);
		assertEquals("username", principal.getName());
	}

	@Test
	public void getUserPrincipal_Container_NoSession() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;

		class User implements Principal {
			@Override
			public String getName() {
				return "username";
			}
		}
		container.login(new User());

		// invalidate session after login
		request.session = null;
		assertNull(container.getUserPrincipal());
	}

	@Test
	public void getUserPrincipal_Application_OutsideSession() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		MockRequestContext context = container.getInstance(RequestContext.class);
		MockHttpServletRequest request = context.request;

		class User implements Principal {
			@Override
			public String getName() {
				return "username";
			}
		}
		container.login(new User());

		// invalidate session after login to emulate multithread session tampering
		request.session.exception = true;
		assertNull(container.getUserPrincipal());
	}

	@Test
	public void isAuthneticated() throws ConfigException {
		String config = "<context interface='js.tiny.container.servlet.RequestContext' class='js.tiny.container.servlet.TinyContainerUnitTest$MockRequestContext' />";
		TinyContainer container = getContainer(config);

		assertFalse(container.isAuthenticated());
		container.login("username", "passsword");
		assertTrue(container.isAuthenticated());
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
