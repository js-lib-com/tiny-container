package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import js.lang.BugError;
import js.lang.GType;
import js.lang.InvocationException;
import js.tiny.container.Container;
import js.tiny.container.InvocationMeter;
import js.tiny.container.ManagedMethod;
import js.tiny.container.PostInvokeInterceptor;
import js.tiny.container.PreInvokeInterceptor;
import js.tiny.container.core.AppFactory;
import js.tiny.container.core.Factory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
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
		assertFalse(managedMethod.isUnchecked());
		assertFalse(managedMethod.isTransactional());
		assertFalse(managedMethod.isImmutable());
		assertFalse(managedMethod.isAsynchronous());
		assertEquals("js.tiny.container.unit.ManagedMethodUnitTest$Person#setName(String)", managedMethod.toString());
	}

	@Test
	public void getRequestPath() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		Classes.setFieldValue(managedMethod, "remotelyAccessible", true);

		assertNull(managedMethod.getRequestPath());
		Classes.invoke(managedMethod, "setRequestPath", (String) null);
		assertEquals("set-name", managedMethod.getRequestPath());
		Classes.invoke(managedMethod, "setRequestPath", "method/path");
		assertEquals("method/path", managedMethod.getRequestPath());
	}

	@Test(expected = BugError.class)
	public void getRequestPath_OnLocal() throws Exception {
		assertNull(getManagedMethod().getRequestPath());
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
	public void setAsynchronous() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();

		assertFalse(managedMethod.isAsynchronous());
		assertEquals("DefaultInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		Classes.invoke(managedMethod, "setAsynchronous", false);
		assertFalse(managedMethod.isAsynchronous());
		assertEquals("DefaultInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		Classes.invoke(managedMethod, "setAsynchronous", true);
		assertTrue(managedMethod.isAsynchronous());
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());
	}

	@Test
	public void getMeter() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		assertNull(Classes.getFieldValue(managedMethod, "meter"));
		assertNotNull(Classes.invoke(managedMethod, "getMeter"));
		assertNotNull(Classes.getFieldValue(managedMethod, "meter"));
	}

	// --------------------------------------------------------------------------------------------
	// INVOKERS

	@Test
	public void defaultInvoker() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		assertEquals("DefaultInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		Person person = new Person();
		assertEquals("John Doe", managedMethod.invoke(person, "John Doe"));
		assertEquals("John Doe", person.name);
	}

	@Test
	public void interceptedInvoker_PojoConstructor() throws Exception {
		IManagedMethod methodInstance = getManagedMethod();
		Class<?> invokerClass = Class.forName("js.tiny.container.ManagedMethod$InterceptedInvoker");
		Constructor<?> constructor = invokerClass.getConstructor(methodInstance.getClass(), Class.class);
		constructor.setAccessible(true);

		Object invoker = constructor.newInstance(methodInstance, MockInterceptor.class);
		assertNotNull(Classes.getFieldValue(invoker, "interceptor"));
	}

	@Test
	public void interceptedInvoker_ManagedConstructor() throws Exception {
		IManagedMethod methodInstance = getManagedMethod();
		Class<?> invokerClass = Class.forName("js.tiny.container.ManagedMethod$InterceptedInvoker");
		Constructor<?> constructor = invokerClass.getConstructor(methodInstance.getClass(), Class.class);
		constructor.setAccessible(true);

		container.managedInstance = new MockInterceptor();
		Object invoker = constructor.newInstance(methodInstance, container.managedInstance.getClass());
		assertNotNull(Classes.getFieldValue(invoker, "interceptor"));
	}

	@Test
	public void interceptedInvoker_Invoke() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, MockInterceptor.class, method);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		MockInterceptor.preInvokeProbe = 0;
		MockInterceptor.postInvokeProbe = 0;

		Person person = new Person();
		assertEquals("John Doe", managedMethod.invoke(person, "John Doe"));
		assertEquals("John Doe", person.name);

		assertEquals(1, MockInterceptor.preInvokeProbe);
		assertEquals(1, MockInterceptor.postInvokeProbe);
	}

	@Test
	public void interceptedInvoker_PreInvoke() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, MockPreInvoker.class, method);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		MockPreInvoker.exception = false;
		MockPreInvoker.invokeProbe = 0;

		Person person = new Person();
		assertEquals("John Doe", managedMethod.invoke(person, "John Doe"));
		assertEquals("John Doe", person.name);

		assertEquals(1, MockPreInvoker.invokeProbe);
	}

	@Test
	public void interceptedInvoker_PostInvoke() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, MockPostInvoker.class, method);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		MockPostInvoker.exception = false;
		MockPostInvoker.invokeProbe = 0;

		Person person = new Person();
		assertEquals("John Doe", managedMethod.invoke(person, "John Doe"));
		assertEquals("John Doe", person.name);

		assertEquals(1, MockPostInvoker.invokeProbe);
	}

	@Test(expected = InvocationException.class)
	public void interceptedInvoker_PreInvokeException() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, MockPreInvoker.class, method);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		MockPreInvoker.exception = true;
		MockPreInvoker.invokeProbe = 0;
		managedMethod.invoke(new Person(), "John Doe");
	}

	@Test(expected = InvocationException.class)
	public void interceptedInvoker_PostInvokeException() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, MockPostInvoker.class, method);
		assertEquals("InterceptedInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		MockPostInvoker.exception = true;
		MockPostInvoker.invokeProbe = 0;
		managedMethod.invoke(new Person(), "John Doe");
	}

	@Test
	public void asyncInvoker() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		Classes.invoke(managedMethod, "setAsynchronous", true);
		assertTrue(managedMethod.isAsynchronous());
		assertEquals("AsyncInvoker", Classes.getFieldValue(managedMethod, "invoker").getClass().getSimpleName());

		Person person = new Person();
		assertNull(managedMethod.invoke(person, "John Doe"));
		// wait for asynchronous invoker to finish; there is no way to join asynchronous invoker
		// next hard coded value is critical and depends on system performance and loading
		// hopefully 500 is the right value; at least is a round number
		sleep(500);
		assertEquals("John Doe", person.name);
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
	public void invokeObject_NoAuthorization() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		container.authenticated = false;
		Classes.invoke(managedMethod, "setRemotelyAccessible", true);

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

	@Test
	public void invokeObject_InstrumentationEnabled() throws Exception {
		IManagedMethod managedMethod = getManagedMethod();
		InvocationMeter meter = Classes.invoke(managedMethod, "getMeter");

		Person person = new Person();
		assertEquals("John Doe", managedMethod.invoke(person, "John Doe"));
		assertEquals("John Doe", person.name);

		assertEquals(1, meter.getInvocationsCount());
		assertEquals(0, meter.getExceptionsCount());
		assertTrue(meter.getMaxProcessingTime() > 0);
		assertEquals(meter.getMaxProcessingTime(), meter.getTotalProcessingTime());
	}

	@Test
	public void invokeObject_InstrumentationEnabled_InvocationException() throws Exception {
		class PersonEx {
			void method() throws Exception {
				throw new Exception("exception");
			}
		}
		Method method = PersonEx.class.getDeclaredMethod("method");
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);
		InvocationMeter meter = Classes.invoke(managedMethod, "getMeter");

		try {
			managedMethod.invoke(new PersonEx());
			fail("Expected InvocationException.");
		} catch (InvocationException unused) {
		}

		assertEquals(1, meter.getInvocationsCount());
		assertEquals(1, meter.getExceptionsCount());
		assertEquals(0, meter.getMaxProcessingTime());
		assertEquals(0, meter.getTotalProcessingTime());
	}

	@Test
	public void invokeObject_InstrumentationEnabled_NoAccessibleMethod() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		IManagedMethod managedMethod = new ManagedMethod(managedClass, method);
		method.setAccessible(false);
		InvocationMeter meter = Classes.invoke(managedMethod, "getMeter");

		try {
			managedMethod.invoke(new Person(), "John Doe");
			fail("Expected InvocationException.");
		} catch (BugError unused) {
		}

		assertEquals(1, meter.getInvocationsCount());
		// exception counter is not incremented on bugs
		assertEquals(0, meter.getExceptionsCount());
		assertEquals(0, meter.getMaxProcessingTime());
		assertEquals(0, meter.getTotalProcessingTime());
	}

	// --------------------------------------------------------------------------------------------
	// INVOCATION METER

	@Test
	public void meter_Constructor() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		InvocationMeter meter = Classes.newInstance("js.tiny.container.ManagedMethod$Meter", method);

		assertEquals(Person.class, meter.getMethodDeclaringClass());
		assertEquals("setName(String)", meter.getMethodSignature());
		assertEquals(0, meter.getInvocationsCount());
		assertEquals(0, meter.getExceptionsCount());
		assertEquals(0, meter.getTotalProcessingTime());
		assertEquals(0, meter.getMaxProcessingTime());

		assertEquals("js.tiny.container.unit.ManagedMethodUnitTest.Person#setName(String): 0: 0: 0: 0: 0", meter.toExternalForm());
	}

	@Test
	public void meter_Setters() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		InvocationMeter meter = Classes.newInstance("js.tiny.container.ManagedMethod$Meter", method);

		Classes.invoke(meter, "incrementInvocationsCount");
		Classes.invoke(meter, "incrementExceptionsCount");
		Classes.invoke(meter, "startProcessing");
		sleep(2);
		Classes.invoke(meter, "stopProcessing");

		assertEquals(1, meter.getInvocationsCount());
		assertEquals(1, meter.getExceptionsCount());
		assertTrue(meter.getTotalProcessingTime() > 0);
		assertTrue(meter.getMaxProcessingTime() > 0);

		assertTrue(meter.toExternalForm().contains("Person#setName(String): 1: 1: "));
	}

	@Test
	public void meter_Reset() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		InvocationMeter meter = Classes.newInstance("js.tiny.container.ManagedMethod$Meter", method);

		Classes.invoke(meter, "incrementInvocationsCount");
		Classes.invoke(meter, "incrementExceptionsCount");
		Classes.invoke(meter, "startProcessing");
		sleep(2);
		Classes.invoke(meter, "stopProcessing");

		meter.reset();

		assertEquals(0, meter.getInvocationsCount());
		assertEquals(0, meter.getExceptionsCount());
		assertEquals(0, meter.getTotalProcessingTime());
		assertEquals(0, meter.getMaxProcessingTime());
	}

	@Test
	public void meter_MaxProcessingTime() throws Exception {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		InvocationMeter meter = Classes.newInstance("js.tiny.container.ManagedMethod$Meter", method);

		Classes.invoke(meter, "startProcessing");
		sleep(200);
		Classes.invoke(meter, "stopProcessing");
		assertEquals(meter.getMaxProcessingTime(), meter.getTotalProcessingTime());
		long maxProcessingTime = meter.getMaxProcessingTime();

		Classes.invoke(meter, "startProcessing");
		sleep(1);
		Classes.invoke(meter, "stopProcessing");
		assertTrue(meter.getTotalProcessingTime() > meter.getMaxProcessingTime());
		assertEquals(maxProcessingTime, meter.getMaxProcessingTime());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private IManagedMethod getManagedMethod() throws NoSuchMethodException, SecurityException {
		Method method = Person.class.getDeclaredMethod("setName", String.class);
		return new ManagedMethod(managedClass, method);
	}

	private static String key(InvocationMeter meter) {
		return meter.getMethodDeclaringClass().getCanonicalName() + "#" + meter.getMethodSignature();
	}

	private static List<InvocationMeter> getMeters() throws Throwable {
		List<InvocationMeter> invocationMeters = new ArrayList<InvocationMeter>();
		AppFactory appFactory = Classes.invoke(Factory.class, "getAppFactory");
		Map<Class<?>, IManagedClass> managedClasses = Classes.getFieldValue(appFactory, Container.class, "classesPool");
		Set<IManagedClass> managedClassesSet = new HashSet<IManagedClass>(managedClasses.values());
		for (IManagedClass managedClass : managedClassesSet) {
			Map<Method, IManagedMethod> managedMethods = Classes.getFieldValue(managedClass, "methodsPool");
			for (IManagedMethod managedMethod : managedMethods.values()) {
				Field field = Classes.getFieldEx(managedMethod.getClass(), "meter");
				invocationMeters.add((InvocationMeter) field.get(managedMethod));
			}
		}
		return invocationMeters;
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
		private boolean authenticated;

		@Override
		public boolean isAuthenticated() {
			return authenticated;
		}

		@Override
		public boolean isAuthorized(String... roles) {
			return authenticated;
		}

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
