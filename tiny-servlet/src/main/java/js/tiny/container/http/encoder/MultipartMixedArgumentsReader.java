package js.tiny.container.http.encoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import jakarta.servlet.http.HttpServletRequest;
import js.json.Json;
import js.lang.IllegalArgumentException;
import js.tiny.container.http.ContentType;
import js.util.Files;

/**
 * Reader for arguments encoded with HTTP multipart mixed format. This arguments reader is invoked with a HTTP request with
 * {@link ContentType#MULTIPART_MIXED}.
 * <p>
 * Basically mutipart mixed format is a collection of body entities in the same message. This reader is able to process a mix of
 * {@link ContentType#APPLICATION_JSON}, {@link ContentType#TEXT_XML} and {@link ContentType#APPLICATION_STREAM} body entities.
 * If bytes stream body entity is present it should be the last one. Formal parameters order should be compatible with mixed
 * body entities order. JSON body entity is deserialized to any Java type supported by {@link Json} implementation, XML to
 * document instance and bytes stream to a type supported by {@link StreamArgumentsReader}.
 * <p>
 * This arguments reader implementation is heavily based on {@link js.tiny.container.http.form} package.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class MultipartMixedArgumentsReader implements ArgumentsReader {
	/**
	 * Store input stream on current thread till HTTP request is processed by application logic, in order to close it after. See
	 * {@link #clean()}.
	 */
	private final ThreadLocal<Closeable> threadLocal = new ThreadLocal<>();

	/** Arguments reader factory for part argument readers. */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	/**
	 * Construct multipart mixed arguments reader.
	 * 
	 * @param argumentsReaderFactory arguments reader factory.
	 */
	public MultipartMixedArgumentsReader(ArgumentsReaderFactory argumentsReaderFactory) {
		this.argumentsReaderFactory = argumentsReaderFactory;
	}

	/**
	 * Read mixed body entities from HTTP request, creating and initializing arguments accordingly given formal parameters. The
	 * number and order of arguments from multipart mixed message should respect formal parameter types. Also stream argument,
	 * if present, should be a single one and the last on arguments list.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return arguments array.
	 * @throws IOException if reading from request input stream fails.
	 * @throws IllegalArgumentException if arguments from HTTP request cannot be parsed.
	 * @throws IllegalArgumentException if content type for a mixed entity body is not supported.
	 * @throws IllegalArgumentException if there is a stream argument but is not the last on arguments list.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		try {
			Object[] arguments = new Object[formalParameters.length];
			int argumentIndex = 0;

			ServletFileUpload multipart = new ServletFileUpload();
			FileItemIterator iterator = multipart.getItemIterator(httpRequest);

			FileItemStream fileItemStream = null;
			while (iterator.hasNext()) {
				fileItemStream = iterator.next();
				String contentType = fileItemStream.getContentType();
				Type parameterType = formalParameters[argumentIndex];

				ArgumentPartReader reader = argumentsReaderFactory.getArgumentPartReader(contentType, parameterType);
				boolean streamArgument = StreamFactory.isStream(parameterType);

				ArgumentPartReader argumentPartReader = (ArgumentPartReader) reader;
				InputStream inputStream = streamArgument ? new LazyFileItemStream(fileItemStream) : fileItemStream.openStream();

				arguments[argumentIndex] = argumentPartReader.read(inputStream, parameterType);
				++argumentIndex;

				// stream argument should be last on mixed arguments list
				// save it to local thread storage for clean-up after arguments processed by application
				if (streamArgument) {
					threadLocal.set(inputStream);
					break;
				}
				inputStream.close();
			}

			if (argumentIndex != formalParameters.length) {
				throw new IllegalArgumentException("Not all parameters processed due to stream argument that is not the last on arguments list.");
			}
			return arguments;
		} catch (FileUploadException e) {
			throw new IOException(e.getMessage());
		}
	}

	/** Close bytes stream from current thread storage then remove the storage. */
	@Override
	public void clean() {
		Files.close(threadLocal.get());
		threadLocal.remove();
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASS

	/**
	 * Thin wrapper for {@link FileItemStream} that opens input stream on the fly, on first read.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static final class LazyFileItemStream extends InputStream {
		/** Wrapped file item stream. */
		private FileItemStream fileItemStream;
		/** File item input stream. */
		private InputStream inputStream;

		/**
		 * Construct lazy file item stream.
		 * 
		 * @param fileItemStream source file item stream.
		 */
		public LazyFileItemStream(FileItemStream fileItemStream) {
			this.fileItemStream = fileItemStream;
		}

		/**
		 * Read next byte from file item input stream, creating it on the fly if necessary.
		 * 
		 * @return next byte from input stream or -1 on stream end.
		 * @throws IOException if read operation fails.
		 */
		@Override
		public int read() throws IOException {
			if (inputStream == null) {
				inputStream = fileItemStream.openStream();
			}
			return inputStream.read();
		}

		/**
		 * Close file item input stream.
		 * 
		 * @throws IOException if close operation fails.
		 */
		@Override
		public void close() throws IOException {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
}
