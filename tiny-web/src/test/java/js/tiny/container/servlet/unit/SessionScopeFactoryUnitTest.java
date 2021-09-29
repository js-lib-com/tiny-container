package js.tiny.container.servlet.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.BeforeClass;
import org.junit.Test;

import js.lang.BugError;
import js.tiny.container.Container;
import js.tiny.container.InstanceKey;
import js.tiny.container.InstanceScope;
import js.tiny.container.ScopeFactory;
import js.tiny.container.core.AppFactory;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.IContainer;
import js.tiny.container.stub.AppFactoryStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpSessionStub;
import js.util.Classes;

@SuppressWarnings({ "unused", "unchecked" })
public class SessionScopeFactoryUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@Test
	public void containerRegistration() throws Exception {
		TinyContainer container = new TinyContainer();
		Map<InstanceScope, ScopeFactory> scopeFactories = Classes.getFieldValue(container, Container.class, "scopeFactories");

		assertNotNull(scopeFactories);
		assertEquals(4, scopeFactories.size());
		assertTrue(scopeFactories.get(InstanceScope.SESSION) instanceof ScopeFactory);
	}

	@Test
	public void getInstanceScope() {
		ScopeFactory factory = getSessionScopeFactory();
		assertEquals(InstanceScope.SESSION, factory.getInstanceScope());
	}

	@Test
	public void getInstance() {
		MockAppFactory appFactory = new MockAppFactory();
		InstanceKey instanceKey = new InstanceKey("1");
		ScopeFactory factory = getSessionScopeFactory(appFactory);

		assertNull(factory.getInstance(instanceKey));
		factory.persistInstance(instanceKey, new Person());

		Person p1 = (Person) factory.getInstance(instanceKey);
		Person p2 = (Person) factory.getInstance(instanceKey);

		assertNotNull(p1);
		assertNotNull(p2);
		assertEquals(p1, p2);
	}

	@Test
	public void persistInstance() {
		MockAppFactory appFactory = new MockAppFactory();
		ScopeFactory factory = getSessionScopeFactory(appFactory);

		InstanceKey instanceKey = new InstanceKey("1");
		assertNull(factory.getInstance(instanceKey));
		Person person = new Person();
		factory.persistInstance(instanceKey, person);

		assertEquals(person, appFactory.context.request.session.getAttribute("1"));
	}

	@Test
	public void clear() {
		MockAppFactory appFactory = new MockAppFactory();
		ScopeFactory factory = getSessionScopeFactory(appFactory);
		factory.clear();
	}

	@Test
	public void getSession() throws Exception {
		MockAppFactory appFactory = new MockAppFactory();
		ScopeFactory factory = getSessionScopeFactory(appFactory);
		HttpSession session = Classes.invoke(factory, "getSession", new InstanceKey("1"));
		assertNotNull(session);
	}

	@Test(expected = BugError.class)
	public void getSession_NullRequest() throws Exception {
		MockAppFactory appFactory = new MockAppFactory();
		appFactory.context.request = null;

		ScopeFactory factory = getSessionScopeFactory(appFactory);
		Classes.invoke(factory, "getSession", new InstanceKey("1"));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static ScopeFactory getSessionScopeFactory() {
		return getSessionScopeFactory(new MockAppFactory());
	}

	private static ScopeFactory getSessionScopeFactory(AppFactory appFactory) {
		return Classes.newInstance("js.tiny.container.servlet.SessionScopeFactory", appFactory);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	public static class Person {
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		@Override
		public Integer getKey() {
			return 1;
		}
	}

	private static class MockAppFactory extends AppFactoryStub {
		private MockRequestContext context = new MockRequestContext(new TinyContainer());

		@Override
		public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
			return (T) context;
		}
	}

	private static class MockRequestContext extends RequestContext {
		private MockHttpServletRequest request = new MockHttpServletRequest();

		public MockRequestContext(IContainer container) {
			super(container);
		}

		@Override
		public HttpServletRequest getRequest() {
			return request;
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private MockHttpSession session = new MockHttpSession();

		@Override
		public HttpSession getSession(boolean create) {
			return session;
		}
	}

	private static class MockHttpSession extends HttpSessionStub {
		private Map<String, Object> attributes = new HashMap<>();

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		@Override
		public void setAttribute(String name, Object value) {
			attributes.put(name, value);
		}
	}
}
