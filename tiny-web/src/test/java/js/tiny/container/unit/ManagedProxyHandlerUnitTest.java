package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import js.lang.Config;
import js.lang.InstanceInvocationHandler;
import js.lang.InvocationException;
import js.lang.VarArgs;
import js.tiny.container.ManagedProxyHandler;
import js.tiny.container.TransactionalResource;
import js.tiny.container.core.AppFactory;
import js.tiny.container.core.Factory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.tiny.container.stub.TransactionManagerStub;
import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.util.Classes;

/**
 * Managed proxy handler unit test. This class focuses on general behavior and leave transactions life cycle for
 * {@link ManagedProxyHandlerTransactionUnitTest}.
 * 
 * @author Iulian Rotaru
 */
@SuppressWarnings("unused")
public class ManagedProxyHandlerUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private AppFactory factory;
	private MockManagedClassSPI managedClass;

	@Before
	public void beforeTest() throws Exception {
		factory = TestContext.start(DESCRIPTOR);
		managedClass = new MockManagedClassSPI();
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED PROXY HANDLER CONSTRUCTOR

	@Test
	public void nonTrasactionalConstructor() {
		Object handler = new ManagedProxyHandler(managedClass, new Object());

		assertNull(Classes.getFieldValue(handler, "transactionalResource"));
		assertNotNull(Classes.getFieldValue(handler, "managedClass"));
		assertNotNull(Classes.getFieldValue(handler, "managedInstance"));
	}

	@Test
	public void trasactionalConstructor() {
		managedClass.transactional = true;
		Object instance = new Object();
		Object handler = new ManagedProxyHandler(getTransactionalResource(), managedClass, instance);

		assertNotNull(Classes.getFieldValue(handler, "transactionalResource"));
		assertNotNull(Classes.getFieldValue(handler, "managedClass"));
		assertNotNull(Classes.getFieldValue(handler, "managedInstance"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullManagedClass() throws Throwable {
		try {
			new ManagedProxyHandler(null, new Object());
		} catch (InvocationException e) {
			throw e.getCause();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullManagedInstance() throws Throwable {
		try {
			new ManagedProxyHandler(managedClass, null);
		} catch (InvocationException e) {
			throw e.getCause();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNonTransactionalResource() throws Throwable {
		try {
			getProxyHandler(managedClass, new Object());
		} catch (InvocationException e) {
			throw e.getCause();
		}
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED PROXY HANDLER INVOKER

	@Test
	public void invokeNonTransactional() throws Exception {
		Person instance = new Person();
		Object handler = new ManagedProxyHandler(managedClass, instance);

		Object proxy = new Object();
		Method method = Person.class.getMethod("setName", String.class);
		Object value = Classes.invoke(handler, "invoke", proxy, method, new Object[] { "John Doe" });

		assertEquals("JOHN DOE", value);
		assertEquals("John Doe", instance.name);
	}

	@Test(expected = IOException.class)
	public void invokeNonTransactionalException() throws Exception {
		Object handler = new ManagedProxyHandler(managedClass, new Person());

		Object proxy = new Object();
		Method method = Person.class.getMethod("exception");
		Classes.invoke(handler, "invoke", proxy, method, new Object[] {});
	}

	@Test
	public void invokeImmutableTransactional() throws Exception {
		MockTransactionManager transactionManager = (MockTransactionManager) Factory.getInstance(TransactionManager.class);
		boolean[][] flags = new boolean[][] { { false, false }, { false, true }, { true, false }, { true, true } };

		managedClass.transactional = true;
		managedClass.immutable = true;

		Person instance = new Person();
		Object proxy = new Object();
		Method method = Person.class.getMethod("setName", String.class);

		for (int i = 0; i < flags.length; ++i) {
			transactionManager.unusedTransaction = flags[i][0];
			transactionManager.closeTransaction = flags[i][1];

			Object handler = getProxyHandler(managedClass, instance);
			Classes.invoke(handler, "invoke", proxy, method, new Object[] { "John Doe" });
			assertEquals("John Doe", instance.name);
		}
	}

	@Test
	public void invokeImmutableTransactionalException() throws Exception {
		MockTransactionManager transactionManager = (MockTransactionManager) Factory.getInstance(TransactionManager.class);
		boolean[][] flags = new boolean[][] { { false, false }, { false, true }, { true, false }, { true, true } };

		managedClass.transactional = true;
		managedClass.immutable = true;

		Person instance = new Person();
		Object proxy = new Object();
		Method method = Person.class.getMethod("exception");

		for (int i = 0; i < flags.length; ++i) {
			transactionManager.unusedTransaction = flags[i][0];
			transactionManager.closeTransaction = flags[i][1];

			Object handler = getProxyHandler(managedClass, instance);
			try {
				Classes.invoke(handler, "invoke", proxy, method, new Object[] {});
				fail("User defined exception should propagate through transaction.");
			} catch (IOException unused) {
			}
		}
	}

	@Test
	public void invokeMutableTransactional() throws Exception {
		MockTransactionManager transactionManager = (MockTransactionManager) Factory.getInstance(TransactionManager.class);
		boolean[][] flags = new boolean[][] { { false, false }, { false, true }, { true, false }, { true, true } };

		managedClass.transactional = true;
		managedClass.immutable = false;

		Person instance = new Person();
		Object proxy = new Object();
		Method method = Person.class.getMethod("setName", String.class);

		for (int i = 0; i < flags.length; ++i) {
			transactionManager.unusedTransaction = flags[i][0];
			transactionManager.closeTransaction = flags[i][1];

			Object handler = getProxyHandler(managedClass, instance);
			Classes.invoke(handler, "invoke", proxy, method, new Object[] { "John Doe" });
			assertEquals("John Doe", instance.name);
		}
	}

	@Test
	public void invokeMutableTransactionalException() throws Exception {
		MockTransactionManager transactionManager = (MockTransactionManager) Factory.getInstance(TransactionManager.class);
		boolean[][] flags = new boolean[][] { { false, false }, { false, true }, { true, false }, { true, true } };

		managedClass.transactional = true;
		managedClass.immutable = false;

		Person instance = new Person();
		Object proxy = new Object();
		Method method = Person.class.getMethod("exception");

		for (int i = 0; i < flags.length; ++i) {
			transactionManager.unusedTransaction = flags[i][0];
			transactionManager.closeTransaction = flags[i][1];

			Object handler = getProxyHandler(managedClass, instance);
			try {
				Classes.invoke(handler, "invoke", proxy, method, new Object[] {});
				fail("User defined exception should propagate through transaction.");
			} catch (IOException unused) {
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// MANAGED PROXY HANDLER UTILITY

	@Test
	public void getWrappedInstance() {
		Object instance = new Object();
		InstanceInvocationHandler<Object> handler = new ManagedProxyHandler(managedClass, instance);
		assertEquals(instance, handler.getWrappedInstance());
	}

	@Test
	public void throwableFormat() throws Exception {
		Throwable throwable = Classes.invoke(ManagedProxyHandler.class, "throwable", new Object[] { new IOException("exception"), "message", new VarArgs<Object>() });

		assertNotNull(throwable);
		assertTrue(throwable instanceof IOException);
		assertEquals("exception", throwable.getMessage());
	}

	@Test
	public void throwableFormat_InvocationException() throws Exception {
		InvocationException exception = new InvocationException(new IOException("exception"));
		Throwable throwable = Classes.invoke(ManagedProxyHandler.class, "throwable", new Object[] { exception, "message", new VarArgs<Object>() });

		assertNotNull(throwable);
		assertTrue(throwable instanceof IOException);
		assertEquals("exception", throwable.getMessage());
	}

	@Test
	public void throwableFormat_InvocationException_NullCause() throws Exception {
		InvocationException exception = new InvocationException((Throwable) null);
		Throwable throwable = Classes.invoke(ManagedProxyHandler.class, "throwable", new Object[] { exception, "message", new VarArgs<Object>() });

		assertNotNull(throwable);
		assertTrue(throwable instanceof InvocationException);
		assertNull(throwable.getMessage());
	}

	@Test
	public void throwableFormat_InvocationTargetException() throws Exception {
		InvocationTargetException exception = new InvocationTargetException(new IOException("exception"));
		Throwable throwable = Classes.invoke(ManagedProxyHandler.class, "throwable", new Object[] { exception, "message", new VarArgs<Object>() });

		assertNotNull(throwable);
		assertTrue(throwable instanceof IOException);
		assertEquals("exception", throwable.getMessage());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY

	private TransactionalResource getTransactionalResource() {
		return (TransactionalResource) factory.getInstance(TransactionalResource.class);
	}

	private InstanceInvocationHandler<Object> getProxyHandler(IManagedClass managedClass, Object instance) {
		return new ManagedProxyHandler(getTransactionalResource(), managedClass, instance);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static final String DESCRIPTOR = "<?xml version='1.0' encoding='UTF-8'?>" + //
			"<test-config>" + //
			"	<managed-classes>" + //
			"		<transaction-manager interface='js.transaction.TransactionManager' class='js.tiny.container.unit.ManagedProxyHandlerUnitTest$MockTransactionManager' />" + //
			"	</managed-classes>" + //
			"</test-config>";

	private static class Person {
		private String name;

		public String setName(String name) {
			this.name = name;
			return name.toUpperCase();
		}

		public void exception() throws IOException {
			throw new IOException();
		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private boolean transactional;
		private boolean immutable;

		@Override
		public IManagedMethod getManagedMethod(Method method) throws NoSuchMethodException {
			MockManagedMethodSPI managedMethod = new MockManagedMethodSPI(method, transactional, immutable);
			return managedMethod;
		}

		@Override
		public boolean isTransactional() {
			return transactional;
		}

		@Override
		public String getTransactionalSchema() {
			return null;
		}
	}

	private static class MockManagedMethodSPI extends ManagedMethodSpiStub {
		private Method method;
		private boolean transactional;
		private boolean immutable;

		public MockManagedMethodSPI(Method method, boolean transactional, boolean immutable) {
			this.method = method;
			this.transactional = transactional;
			this.immutable = immutable;
		}

		@Override
		public boolean isTransactional() {
			return transactional;
		}

		@Override
		public boolean isImmutable() {
			return immutable;
		}

		@Override
		public Object proxyInvoke(Object object, Object... args) throws IllegalArgumentException, InvocationException, AuthorizationException {
			try {
				return method.invoke(object, args);
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
				throw new InvocationException(e.getTargetException());
			}
			return null;
		}
	}

	private static class MockTransactionManager extends TransactionManagerStub {
		private boolean closeTransaction = true;
		private boolean unusedTransaction;
		private MockTransaction transaction;

		@Override
		public void config(Config config) {
		}

		@Override
		public Transaction createTransaction(String schema) {
			return (transaction = new MockTransaction(closeTransaction, unusedTransaction));
		}

		@Override
		public Transaction createReadOnlyTransaction(String schema) {
			return (transaction = new MockTransaction(closeTransaction, unusedTransaction));
		}
	}

	private static class MockTransaction implements Transaction {
		private boolean close;
		private boolean unused;

		public MockTransaction(boolean close, boolean unused) {
			this.close = close;
			this.unused = unused;
		}

		@Override
		public void commit() {
		}

		@Override
		public void rollback() {
		}

		@Override
		public boolean close() {
			return close;
		}

		@Override
		public boolean unused() {
			return unused;
		}

		@Override
		public <T> T getResourceManager() {
			return null;
		}
	}
}