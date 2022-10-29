package com.jslib.container.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.jslib.util.Classes;

public class JavaConformityTest {
	/**
	 * Static field is 'static' only inside a class loader. If uses two class loaders to load the same class, static field is in
	 * fact in two instances that can be changed independently.
	 * 
	 * This is particularly important if we remember that every web application has its own class loader. So a 'shared' static
	 * field has its own instance on every web application.
	 */
	@Test
	public void GivenDifferentClassLoaders_WhenStaticFieldAssigment_ThenDifferentValues() throws Throwable {
		// given
		TestClassLoader loader1 = new TestClassLoader();
		TestClassLoader loader2 = new TestClassLoader();
		assertFalse(loader1.equals(loader2));

		// when
		Class<?> clsA = Class.forName("com.jslib.container.api.JavaConformityTest$TestClass", true, loader1);
		Class<?> clsB = Class.forName("com.jslib.container.api.JavaConformityTest$TestClass", true, loader2);
		assertFalse(clsA.equals(clsB));

		Object instanceA = clsA.getConstructor().newInstance();
		Object instanceB = clsB.getConstructor().newInstance();
		Classes.invoke(instanceA, "setField", 1);

		// then
		assertFalse(Classes.getFieldValue(instanceA, "field") == Classes.getFieldValue(instanceB, "field"));
		assertEquals((int) Classes.getFieldValue(instanceA, "field"), 1);
		assertEquals((int) Classes.getFieldValue(instanceB, "field"), 0);
	}

	/** New operator as a whole fails with exception if constructor throw it. */
	@Test(expected = RuntimeException.class)
	public void GivenConstructorException_WhenNewOperator_ThenException() {
		// given
		class ExceptionalObject {
			ExceptionalObject() {
				throw new RuntimeException();
			}
		}

		// when
		new ExceptionalObject();
		
		// then
	}

	/** Null value does not qualify for any type on instanceof operator. */
	@Test
	public void GivenInstanceOfOperator_WhenNull_ThenFalse() {
		assertFalse(null instanceof Object);
	}

	@Test
	public void GivenThreadWaitingOnLock_WhenInterrupt_ThenLockInterruptException() throws InterruptedException {
		// given
		Object lock = new Object();
		AtomicBoolean interrupted = new AtomicBoolean(false);

		Thread thread = new Thread(() -> {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					interrupted.set(true);
				}
			}
		});

		thread.setDaemon(false);
		thread.start();

		// when
		while (!thread.isAlive())
			;
		synchronized (lock) {
			thread.interrupt();
		}

		// then
		thread.join();
		assertTrue("Thread lock not interrupted.", interrupted.get());
	}

	@Test
	public void GivenSleepingThread_WhenInterrupt_ThenSleepInterruptException() throws InterruptedException {
		// given
		AtomicBoolean interrupted = new AtomicBoolean(false);

		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(31536000000L); // sleep for one year
			} catch (InterruptedException e) {
				interrupted.set(true);
			}
		});

		thread.setDaemon(false);
		thread.start();

		// when
		while (!thread.isAlive())
			;
		thread.interrupt();

		// then
		thread.join();
		assertTrue("Thread sleep not interrupted.", interrupted.get());
	}

	// ------------------------------------------------------
	// FIXTURE

	private static class TestClassLoader extends ClassLoader {
		public TestClassLoader() {
			super(TestClassLoader.class.getClassLoader());
		}

		private synchronized Class<?> getClass(String name) throws ClassNotFoundException {
			Class<?> cls = findLoadedClass(name);
			if (cls != null) {
				System.out.println("Class " + name + " has been loaded.");
				return cls;
			}
			System.out.println("Class " + name + " has not been loaded. Loading now.");

			String file = name.replace('.', File.separatorChar) + ".class";
			byte[] b = null;
			try {
				b = loadClassData(file);
				cls = defineClass(name, b, 0, b.length);
				resolveClass(cls);
				return cls;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			System.out.println("Loading class '" + name + "' ...");
			if (name.startsWith("com.jslib.")) {
				return getClass(name);
			}
			return super.loadClass(name);
		}

		private byte[] loadClassData(String name) throws IOException {
			InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
			int size = stream.available();
			byte buff[] = new byte[size];
			DataInputStream in = new DataInputStream(stream);
			in.readFully(buff);
			in.close();
			return buff;
		}
	}

	public static class TestClass {
		public static int field;

		protected static int getField() {
			return TestClass.field;
		}

		protected static void setField(int field) {
			TestClass.field = field;
		}

		public static void print(String instanceName) {
			System.out.println("The static member value in " + instanceName + " is " + field);
		}
	}
}
