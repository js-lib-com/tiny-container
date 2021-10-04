package js.tiny.container.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import org.junit.Ignore;
import org.junit.Test;

import js.tiny.container.http.Resource;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.tiny.container.unit.ServletConfigStub;
import js.tiny.container.unit.ServletContextStub;
import js.util.Classes;

public class ResourceServletUnitTest {
	@Test
	public void constructor() {
		ResourceServlet servlet = new ResourceServlet();
		assertNotNull(Classes.getFieldValue(servlet, "argumentsReaderFactory"));
		assertNotNull(Classes.getFieldValue(servlet, "resourceMethods"));
	}

	@Test
	@Ignore
	public void init() throws ServletException {
		MockContainer container = new MockContainer();
		container.methods.add(new MockManagedMethod("setUser", void.class));
		container.methods.add(new MockManagedMethod("index", Resource.class));

		MockServletConfig config = new MockServletConfig();
		config.context.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		ResourceServlet servlet = new ResourceServlet();
		servlet.init(config);

		Map<String, IManagedMethod> methods = Classes.getFieldValue(servlet, "resourceMethods");
		assertNotNull(methods);
		assertEquals(1, methods.size());
		assertNotNull(methods.get("/controller/index"));
		assertEquals("index", methods.get("/controller/index").getServiceMeta(RequestPathMeta.class).value());
	}

	@Test(expected = UnavailableException.class)
	public void init_NoContainer() throws ServletException {
		MockServletConfig config = new MockServletConfig();
		ResourceServlet servlet = new ResourceServlet();
		servlet.init(config);
	}

	@Test
	public void storageKey() throws Exception {
		MockManagedMethod resourceMethod = new MockManagedMethod("resource", Resource.class);
		assertEquals("/controller/resource", key(resourceMethod));

		resourceMethod.declaringClass.requestPath = null;
		assertEquals("/resource", key(resourceMethod));
	}

	@Test
	public void retrievalKey() throws Exception {
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

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockContainer extends ContainerStub {
		private List<IManagedMethod> methods = new ArrayList<>();

		@Override
		public Iterable<IManagedMethod> getNetMethods() {
			return methods;
		}
	}

	private static class MockManagedClass extends ManagedClassSpiStub {
		private String requestPath = "controller";

		@SuppressWarnings("unchecked")
		@Override
		public <T extends IServiceMeta> T getServiceMeta(Class<T> type) {
			return (T) new ControllerMeta(requestPath);
		}
	}

	private static class MockManagedMethod extends ManagedMethodSpiStub {
		private MockManagedClass declaringClass = new MockManagedClass();
		private String requestPath;
		private Class<?> returnType;

		public MockManagedMethod(String requestPath, Class<?> returnType) {
			this.requestPath = requestPath;
			this.returnType = returnType;
		}

		@Override
		public IManagedClass getDeclaringClass() {
			return declaringClass;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends IServiceMeta> T getServiceMeta(Class<T> type) {
			return (T) new RequestPathMeta(requestPath);
		}

		@Override
		public Type getReturnType() {
			return returnType;
		}
	}

	private static class MockServletConfig extends ServletConfigStub {
		private MockServletContext context = new MockServletContext();

		@Override
		public String getServletName() {
			return "resource-servlet";
		}

		@Override
		public ServletContext getServletContext() {
			return context;
		}

		@Override
		public String getInitParameter(String name) {
			return null;
		}
	}

	private static class MockServletContext extends ServletContextStub {
		private Map<String, Object> attributes = new HashMap<>();

		@Override
		public String getServletContextName() {
			return "test-app";
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		@Override
		public String getInitParameter(String name) {
			return null;
		}
	}
}
