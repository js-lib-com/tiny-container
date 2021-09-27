package js.tiny.container.http.encoder;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;

import js.io.FilesInputStream;
import js.lang.IllegalArgumentException;
import js.util.Params;
import js.util.Types;

/**
 * Factory for byte and character streams wrapping a given input stream.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class StreamFactory {
	/** Private constructor. */
	private StreamFactory() {
	}

	/**
	 * Create stream of requested type wrapping given input stream. Both returned bytes and character streams are closeable.
	 * 
	 * @param inputStream input stream to wrap,
	 * @param type the type of stream, byte or character.
	 * @return newly create stream.
	 * @throws IOException if newly stream creation fails.
	 * @throws IllegalArgumentException if there requested type is not supported.
	 */
	public static Closeable getInstance(InputStream inputStream, Type type) throws IOException {
		Params.notNull(inputStream, "Input stream");
		Params.notNull(type, "Type");

		// TODO: construct instance reflexively to allow for user defined input stream
		// an user defined input stream should have a constructor with a single parameter of type InputStream

		if (type == InputStream.class) {
			return inputStream;
		}
		if (type == ZipInputStream.class) {
			return new ZipInputStream(inputStream);
		}
		if (type == JarInputStream.class) {
			return new JarInputStream(inputStream);
		}
		if (type == FilesInputStream.class) {
			return new FilesInputStream(inputStream);
		}
		if (type == Reader.class || type == InputStreamReader.class) {
			return new InputStreamReader(inputStream, "UTF-8");
		}
		if (type == BufferedReader.class) {
			return new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		}
		if (type == LineNumberReader.class) {
			return new LineNumberReader(new InputStreamReader(inputStream, "UTF-8"));
		}
		if (type == PushbackReader.class) {
			return new PushbackReader(new InputStreamReader(inputStream, "UTF-8"));
		}
		throw new IllegalArgumentException("Unsupported stream type |%s|.", type);
	}

	/**
	 * Test if type is a stream, either bytes or characters stream.
	 * 
	 * @param type type to test.
	 * @return true type is a stream.
	 */
	public static boolean isStream(Type type) {
		return Types.isKindOf(type, InputStream.class, Reader.class);
	}
}
