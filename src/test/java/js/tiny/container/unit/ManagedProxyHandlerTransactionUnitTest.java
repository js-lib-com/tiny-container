package js.tiny.container.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import js.lang.InvocationException;
import js.tiny.container.AuthorizationException;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.ManagedProxyHandler;
import js.tiny.container.TransactionalResource;
import js.tiny.container.stub.ManagedClassSpiStub;
import js.tiny.container.stub.ManagedMethodSpiStub;
import js.transaction.Transaction;
import js.transaction.TransactionException;
import js.transaction.TransactionManager;
import js.util.Classes;

/**
 * Complement unit test for {@link ManagedProxyHandlerUnitTest} but specialized for transaction life cycle.
 * 
 * @author Iulian Rotaru
 */
public class ManagedProxyHandlerTransactionUnitTest {
	private MockTransactionalResource transactionalResource;
	private MockManagedClassSPI managedClass;

	@Before
	public void beforeTest() {
		managedClass = new MockManagedClassSPI();
	}

	/** Immutable transaction should not invoke {@link Transaction#commit()}. */
	@Test
	public void commitImmutableTransaction() throws Throwable {
		managedClass.transactional = true;
		managedClass.immutable = true;

		ManagedProxyHandler handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("setName", String.class);
		handler.invoke(new Object(), method, new Object[] { "John Doe" });

		assertEquals(1, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(0, transaction.commitProbe);
		assertEquals(0, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/** Exception on immutable transaction should not invoke {@link Transaction#rollback()}. */
	@Test
	public void rollbackImmutableTransaction() throws NoSuchMethodException {
		managedClass.transactional = true;
		managedClass.immutable = true;

		ManagedProxyHandler handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("exception");
		try {
			handler.invoke(new Object(), method, new Object[] {});
			fail("Method exception should be re-thrown.");
		} catch (Throwable unused) {
		}

		assertEquals(1, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(0, transaction.commitProbe);
		assertEquals(0, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/**
	 * Mutable transaction should invoke {@link Transaction#commit()} and {@link Transaction#close()} but not
	 * {@link Transaction#rollback()}.
	 */
	@Test
	public void commitMutableTransaction() throws Exception {
		managedClass.transactional = true;
		managedClass.immutable = false;

		Object handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("setName", String.class);
		Classes.invoke(handler, "invoke", new Object(), method, new Object[] { "John Doe" });

		assertEquals(1, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(1, transaction.commitProbe);
		assertEquals(0, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/**
	 * Exception on mutable transaction should invoke {@link Transaction#rollback()} and {@link Transaction#close()} but not
	 * {@link Transaction#commit()}.
	 */
	@Test
	public void rollbackMutableTransaction() throws Exception {
		managedClass.transactional = true;
		managedClass.immutable = false;

		Object handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("exception");
		try {
			Classes.invoke(handler, "invoke", new Object(), method, new Object[] {});
			fail("Method exception should be re-thrown.");
		} catch (IOException unused) {
		}

		assertEquals(1, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(0, transaction.commitProbe);
		assertEquals(1, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/** Like {@link #commitImmutableTransaction()} but ensure session stored on transactional resource is released only once. */
	@Test
	public void commitNestedImmutableTransaction() throws Exception {
		managedClass.transactional = true;
		managedClass.immutable = true;

		Object handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("setNestedName", String.class);
		Classes.invoke(handler, "invoke", new Object(), method, new Object[] { "John Doe" });

		assertEquals(2, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(0, transaction.commitProbe);
		assertEquals(0, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/**
	 * Like {@link #rollbackImmutableTransaction()} but ensure session stored on transactional resource is released only once.
	 */
	@Test
	public void roolbackNestedImmutableTransaction() throws Throwable {
		managedClass.transactional = true;
		managedClass.immutable = true;

		ManagedProxyHandler handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("nestedException");
		try {
			handler.invoke(new Object(), method, new Object[] {});
			fail("Method exception should be re-thrown.");
		} catch (IOException unused) {
		}

		assertEquals(2, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(0, transaction.commitProbe);
		assertEquals(0, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/** Like {@link #commitMutableTransaction()} but ensure session stored on transactional resource is released only once. */
	@Test
	public void commitNestedMutableTransaction() throws Exception {
		managedClass.transactional = true;
		managedClass.immutable = false;

		Object handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("setNestedName", String.class);
		Classes.invoke(handler, "invoke", new Object(), method, new Object[] { "John Doe" });

		assertEquals(2, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(1, transaction.commitProbe);
		assertEquals(0, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	/** Like {@link #rollbackMutableTransaction()} but ensure session stored on transactional resource is released only once. */
	@Test
	public void roolbackNestedMutableTransaction() throws Exception {
		managedClass.transactional = true;
		managedClass.immutable = false;

		Object handler = getMockProxyHandler(new Person());
		Method method = Person.class.getMethod("nestedException");
		try {
			Classes.invoke(handler, "invoke", new Object(), method, new Object[] {});
			fail("Method exception should be re-thrown.");
		} catch (IOException unused) {
		}

		assertEquals(2, transactionalResource.storeSessionProbe);
		assertEquals(1, transactionalResource.releaseSessionProbe);

		MockTransaction transaction = transactionalResource.transaction;
		assertEquals(0, transaction.commitProbe);
		assertEquals(1, transaction.rollbackProbe);
		assertEquals(1, transaction.closeProbe);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY

	private ManagedProxyHandler getMockProxyHandler(Object instance) {
		transactionalResource = new MockTransactionalResource();
		return new ManagedProxyHandler(transactionalResource, managedClass, instance);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@SuppressWarnings("unused")
	private class Person {
		private String name;

		public String setName(String name) {
			this.name = name;
			return name.toUpperCase();
		}

		public void exception() throws IOException {
			throw new IOException();
		}

		public String setNestedName(String name) throws Throwable {
			ManagedProxyHandler handler = new ManagedProxyHandler(transactionalResource, managedClass, new Person());
			Method method = getClass().getMethod("setName", String.class);
			return (String) handler.invoke(new Object(), method, new Object[] { name });
		}

		public void nestedException() throws Throwable {
			ManagedProxyHandler handler = new ManagedProxyHandler(transactionalResource, managedClass, new Person());
			Method method = getClass().getMethod("exception");
			handler.invoke(new Object(), method, new Object[] {});
		}
	}

	private static class MockManagedClassSPI extends ManagedClassSpiStub {
		private boolean transactional;
		private boolean immutable;

		@Override
		public ManagedMethodSPI getManagedMethod(Method method) throws NoSuchMethodException {
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

		@SuppressWarnings("unchecked")
		@Override
		public <T> T invoke(Object object, Object... args) throws IllegalArgumentException, InvocationException, AuthorizationException {
			try {
				return (T) method.invoke(object, args);
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
				throw new InvocationException(e.getTargetException());
			}
			return null;
		}
	}

	private static class MockTransactionalResource implements TransactionalResource {
		private MockTransaction transaction;
		private int storeSessionProbe;
		private int releaseSessionProbe;

		@Override
		public TransactionManager getTransactionManager() {
			throw new UnsupportedOperationException("getTransactionManager()");
		}

		@Override
		public void storeSession(Object session) {
			++storeSessionProbe;
		}

		@Override
		public void releaseSession() {
			++releaseSessionProbe;
		}

		@Override
		public <T> T getSession() {
			return null;
		}

		@Override
		public Transaction createTransaction(String resourceUnit) {
			return (transaction = new MockTransaction());
		}

		@Override
		public Transaction createReadOnlyTransaction(String resourceUnit) {
			return (transaction = new MockTransaction());
		}
	}

	private static class MockTransaction implements Transaction {
		// use nesting level to close only outermost transaction
		private static ThreadLocal<Integer> nestingLevel = new ThreadLocal<>();
		static {
			nestingLevel.set(0);
		}

		public int commitProbe;
		public int rollbackProbe;
		public int closeProbe;

		public MockTransaction() {
			Integer level = nestingLevel.get();
			++level;
			nestingLevel.set(level);
		}

		@Override
		public void commit() {
			++commitProbe;
		}

		@Override
		public void rollback() {
			++rollbackProbe;
		}

		/**
		 * Close transaction after commit or even roolback to ensure resources used by transaction are released. Implementation
		 * should track nested transactions and actually release resources only for the outermost transaction.
		 * 
		 * @return true if transaction was closed and resources released; return false if this transaction is a nested one.
		 * @throws TransactionException if close fails.
		 */
		@Override
		public boolean close() {
			++closeProbe;
			Integer level = nestingLevel.get();
			--level;
			nestingLevel.set(level);
			return level == 0;
		}

		@Override
		public boolean unused() {
			return false;
		}

		@Override
		public <T> T getSession() {
			return null;
		}
	}
}
