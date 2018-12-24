package js.http.test;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
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

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.http.encoder.ArgumentsReader;
import js.io.FilesInputStream;
import js.lang.GType;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class StreamsTest {
	@Mock
	private InputStream stream;

	@Mock
	private HttpServletRequest request;

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
		when(stream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
		
		assertTrue(getInstance(stream, InputStream.class) instanceof InputStream);
		assertTrue(getInstance(stream, ZipInputStream.class) instanceof ZipInputStream);
		assertTrue(getInstance(stream, JarInputStream.class) instanceof JarInputStream);
		assertTrue(getInstance(stream, Reader.class) instanceof InputStreamReader);
		assertTrue(getInstance(stream, InputStreamReader.class) instanceof InputStreamReader);
		assertTrue(getInstance(stream, BufferedReader.class) instanceof BufferedReader);
		assertTrue(getInstance(stream, LineNumberReader.class) instanceof LineNumberReader);
		assertTrue(getInstance(stream, PushbackReader.class) instanceof PushbackReader);
	}

	@Test
	public void streamFactory_FilesInputStream() throws Exception {
		InputStream inputStream = new FileInputStream("fixture/files-stream/archive-file.zip");
		assertTrue(getInstance(inputStream, FilesInputStream.class) instanceof FilesInputStream);
	}

	@Test(expected = IllegalArgumentException.class)
	public void streamFactory_Unsupported() throws Exception {
		getInstance(stream, Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void streamFactory_NullInputStream() throws Exception {
		getInstance(null, Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void streamFactory_NullType() throws Exception {
		getInstance(stream, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void argumentsReader_read_EmptyTypes() throws Exception {
		ArgumentsReader reader = Classes.newInstance("js.http.encoder.StreamArgumentsReader");
		Classes.invoke(reader, "read", request, new Type[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void argumentsReader_read_ParameterizedType() throws Exception {
		ArgumentsReader reader = Classes.newInstance("js.http.encoder.StreamArgumentsReader");
		Classes.invoke(reader, "read", request, new Type[] { new GType(List.class, String.class) });
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	public static Closeable getInstance(InputStream inputStream, Type type) throws Exception {
		Class<?> clazz = Classes.forName("js.http.encoder.StreamFactory");
		return Classes.invoke(clazz, "getInstance", inputStream, type);
	}
}
