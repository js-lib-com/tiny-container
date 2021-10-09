package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import js.lang.BugError;
import js.lang.GType;
import js.lang.InvocationException;
import js.tiny.container.ManagedMethod;
import js.tiny.container.interceptor.PostInvokeInterceptor;
import js.tiny.container.interceptor.PreInvokeInterceptor;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ContainerStub;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.util.Classes;
import js.util.Types;

@SuppressWarnings("unused")
public class ManagedMethodUnitTest {
	private MockContainer container;
	private MockManagedClass managedClass;

	@Before
	public void beforeTest() {
		container = new MockContainer();
		managedClass = new MockManagedClass();
		managedClass.container = container;
	}

	@Test
	public void constructor() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);

		assertEquals(managedClass, managedMethod.getDeclaringClass());
		assertEquals(method, managedMethod.getMethod());
		assertEquals(1, managedMethod.getParameterTypes().length);
		assertEquals(String.class, managedMethod.getParameterTypes()[0]);
		assertEquals(String.class, managedMethod.getReturnType());
		assertFalse(managedMethod.isVoid());
		assertFalse(managedMethod.isRemotelyAccessible());
		assertEquals("js.tiny.container.unit.ManagedMethodUnitTest$Person#setName(String)", managedMethod.toString());
	}

	@Test
	public void getParameterTypes() {
		class Params {
			public void method1() {
			}

			public void method2(String name) {
			}

			public void method3(String[] names) {
			}

			public void method4(String name, int age) {
			}

			public void method5(List<String> names) {
			}

			public void method6(String name, int[] values, List<String> names) {
			}
		}

		Map<String, Type[]> params = new HashMap<>();
		params.put("method1", new Type[] {});
		params.put("method2", new Type[] { String.class });
		params.put("method3", new Type[] { String[].class });
		params.put("method4", new Type[] { String.class, int.class });
		params.put("method5", new Type[] { new GType(List.class, String.class) });
		params.put("method6", new Type[] { String.class, int[].class, new GType(List.class, String.class) });

		for (Method method : Params.class.getMethods()) {
			final Type[] expected = params.get(method.getName());
			if (expected == null) {
				continue;
			}
			final Type[] actual = new ManagedMethod(managedClass, method).getParameterTypes();

			assertEquals(expected.length, actual.length);
			for (int i = 0; i < expected.length; ++i) {
				assertTrue(Types.isEqual(expected[i], actual[i]));
			}
		}
	}

	@Test
	@Ignore
	public void getMeter() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		assertNull(Classes.getFieldValue(managedMethod, "meter"));
		assertNotNull(Classes.invoke(managedMethod, "getMeter"));
		assertNotNull(Classes.getFieldValue(managedMethod, "meter"));
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED METHOD INVOKE

	@Test
	public void invokeObject() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		Person person = new Person();
		assertEquals("John Doe", managedMethod.invoke(person, "John Doe"));
		assertEquals("John Doe", person.name);
	}

	/** Throws authorization exception if container is not authenticated and managed method is private remote. */
	@Test(expected = AuthorizationException.class)
	@Ignore
	public void invokeObject_NoAuthorization() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		Classes.invoke(managedMethod, "setSecurityEnabled", true);

		managedMethod.invoke(new Person(), "John Doe");
	}

	/** Throws invocation exception if method execution fails. */
	@Test(expected = InvocationException.class)
	public void invokeObject_InvocationException() throws Exception {
		class PersonEx {
			void method() throws Exception {
				throw new Exception("exception");
			}
		}
		Method method = PersonEx.class.getDeclaredMethod("method");
		new ManagedMethod(managedClass, method).invoke(new PersonEx());
	}

	/** It is considered a bug if Java method accessible flag is not set. It should be set by constructor. */
	@Test(expected = BugError.class)
	public void invokeObject_NoAccessibleMethod() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);
		method.setAccessible(false);
		managedMethod.invoke(new Person(), "John Doe");
	}

	@Test
	public void invokeProxy() throws Exception {
		MockInvocationHandler handler = new MockInvocationHandler();
		handler.instance = new Person();
		Human human = (Human) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Human.class }, handler);

		Method method = Human.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);
		assertEquals("John Doe", managedMethod.invoke(human, "John Doe"));
		assertEquals("John Doe", ((Person) handler.instance).name);
	}

	@Test(expected = InvocationException.class)
	public void invokeProxy_InvocationException() throws Exception {
		class PersonEx implements Human {
			@Override
			public String setName(String name) throws Exception {
				throw new Exception("exception");
			}
		}

		MockInvocationHandler handler = new MockInvocationHandler();
		handler.instance = new PersonEx();
		Human human = (Human) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Human.class }, handler);

		Method method = Human.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);
		assertEquals("John Doe", managedMethod.invoke(human, "John Doe"));
		assertEquals("John Doe", ((Person) handler.instance).name);
	}

	/** It is considered a bug if Java method accessible flag is not set. It should be set by constructor. */
	@Test(expected = BugError.class)
	public void invokeProxy_NoAccessibleMethod() throws Exception {
		MockInvocationHandler handler = new MockInvocationHandler();
		handler.instance = new Person();
		Human human = (Human) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Human.class }, handler);

		Method method = Human.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);
		method.setAccessible(false);
		managedMethod.invoke(human, "John Doe");
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private IManagedMethod getManagedMethod() throws NoSuchMethodException, SecurityException {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		return new ManagedMethod(managedClass, method);
	}

	/**
	 * Put thread on sleep for a while. This method is necessary only when run tests from Maven, Surefire plugin. Apparently
	 * there is a bug when current thread keeps interrupted flag and, when reused, Thread.sleep() throws InterruptedException.
	 * This behavior is not consistent; it depends on operating system - for example on Windows is working well, and apparently
	 * on Maven / Surefire version. Also on a virtual machine on Windows host tests are working properly.
	 */
	private static void sleep(long duration) {
		long timestamp = System.currentTimeMillis() + duration;
		for (;;) {
			long delay = timestamp - System.currentTimeMillis();
			if (delay <= 0) {
				break;
			}
			try {
				Thread.sleep(delay);
			} catch (Throwable unused) {
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static interface Human {
		String setName(String name) throws Exception;
	}

	private static class Person implements Human {
		private String name;

		public String setName(String name) {
			this.name = name;
			return name;
		}
	}

	private static class MockContainer extends ContainerStub {
		public Object managedInstance;

		@Override
		public boolean isManagedClass(Class<?> interfaceClass) {
			return managedInstance != null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
			return (T) managedInstance;
		}
	}

	private static class MockManagedClass extends ManagedClassSpiStub {
		private MockContainer container;

		@Override
		public IContainer getContainer() {
			return container;
		}

		@Override
		public Collection<IContainerService> getServices() {
			return Collections.emptySet();
		}

		@Override
		public Class<?> getImplementationClass() {
			return Person.class;
		}
	}

	private static class MockInterceptor implements PreInvokeInterceptor, PostInvokeInterceptor {
		private static int preInvokeProbe;
		private static int postInvokeProbe;

		private IManagedMethod preMethod;
		private Object[] preArgs;

		private IManagedMethod postMethod;
		private Object[] postArgs;
		private Object postReturnValue;

		@Override
		public void preInvoke(IManagedMethod managedMethod, Object[] args) {
			++preInvokeProbe;
			this.preMethod = managedMethod;
			this.preArgs = args;
		}

		@Override
		public void postInvoke(IManagedMethod managedMethod, Object[] args, Object returnValue) {
			++postInvokeProbe;
			this.postMethod = managedMethod;
			this.postArgs = args;
			this.postReturnValue = returnValue;
		}
	}

	private static class MockPreInvoker implements PreInvokeInterceptor {
		private static boolean exception;
		private static int invokeProbe;

		@Override
		public void preInvoke(IManagedMethod managedMethod, Object[] args) throws Exception {
			if (exception) {
				throw new Exception("exception");
			}
			++invokeProbe;
		}
	}

	private static class MockPostInvoker implements PostInvokeInterceptor {
		private static boolean exception;
		private static int invokeProbe;

		@Override
		public void postInvoke(IManagedMethod managedMethod, Object[] args, Object returnValue) throws Exception {
			if (exception) {
				throw new Exception("exception");
			}
			++invokeProbe;
		}
	}

	private static class MockInvocationHandler implements InvocationHandler {
		private Object instance;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return method.invoke(instance, args);
		}
	}
}
