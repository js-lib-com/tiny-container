package js.http.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;

import js.http.encoder.ArgumentsReader;
import js.io.FilesInputStream;
import js.lang.GType;
import js.unit.HttpServletRequestStub;
import js.util.Classes;

import org.junit.Test;

public class StreamsUnitTest {
	@Test
	public void streamFactory_Constructor() throws Exception {
		Class<?> clazz = Classes.forName("js.http.encoder.StreamFactory");
		Constructor<?> constructor = clazz.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void streamFactory_GetInstance() throws Exception {
		InputStream inputStream = new MockInputStream();
		assertTrue(getInstance(inputStream, InputStream.class) instanceof InputStream);
		assertTrue(getInstance(inputStream, ZipInputStream.class) instanceof ZipInputStream);
		assertTrue(getInstance(inputStream, JarInputStream.class) instanceof JarInputStream);
		assertTrue(getInstance(inputStream, Reader.class) instanceof InputStreamReader);
		assertTrue(getInstance(inputStream, InputStreamReader.class) instanceof InputStreamReader);
		assertTrue(getInstance(inputStream, BufferedReader.class) instanceof BufferedReader);
		assertTrue(getInstance(inputStream, LineNumberReader.class) instanceof LineNumberReader);
		assertTrue(getInstance(inputStream, PushbackReader.class) instanceof PushbackReader);
	}

	@Test
	public void streamFactory_FilesInputStream() throws Exception {
		InputStream inputStream = new FileInputStream("fixture/files-stream/archive-file.zip");
		assertTrue(getInstance(inputStream, FilesInputStream.class) instanceof FilesInputStream);
	}

	@Test(expected = IllegalArgumentException.class)
	public void streamFactory_Unsupported() throws Exception {
		getInstance(new MockInputStream(), Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void streamFactory_NullInputStream() throws Exception {
		getInstance(null, Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void streamFactory_NullType() throws Exception {
		getInstance(new MockInputStream(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void argumentsReader_read_EmptyTypes() throws Exception {
		ArgumentsReader reader = Classes.newInstance("js.http.encoder.StreamArgumentsReader");
		Classes.invoke(reader, "read", new MockHttpServletRequest(), new Type[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void argumentsReader_read_ParameterizedType() throws Exception {
		ArgumentsReader reader = Classes.newInstance("js.http.encoder.StreamArgumentsReader");
		Classes.invoke(reader, "read", new MockHttpServletRequest(), new Type[] { new GType(List.class, String.class) });
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	public static Closeable getInstance(InputStream inputStream, Type type) throws Exception {
		Class<?> clazz = Classes.forName("js.http.encoder.StreamFactory");
		return Classes.invoke(clazz, "getInstance", inputStream, type);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	private static class MockHttpServletRequest extends HttpServletRequestStub {

	}
}
