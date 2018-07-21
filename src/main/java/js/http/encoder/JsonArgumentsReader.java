package js.http.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;

import js.json.Json;
import js.json.JsonException;
import js.lang.IllegalArgumentException;
import js.util.Classes;
import js.util.Types;

/**
 * JSON arguments reader for both invocation and multipart body entity. This arguments reader implementation delegates
 * {@link Json} for actual arguments parsing. It is used by servlets for method invocation arguments read and by
 * {@link MultipartMixedArgumentsReader} for entity body parsing when encoded JSON.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class JsonArgumentsReader implements ArgumentsReader, ArgumentPartReader {
	/** JSON deserializer delegated for arguments parsing. */
	private final Json json;

	/** Create JSON arguments reader. */
	public JsonArgumentsReader() {
		json = Classes.loadService(Json.class);
	}

	/**
	 * Uses JSON deserializer to parse method invocation arguments accordingly formal parameters list.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return arguments array.
	 * @throws IOException if reading from request input stream fails.
	 * @throws IllegalArgumentException if arguments from HTTP request cannot be parsed.
	 * @throws JsonException if JSON parsing fails due to invalid stream format.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		JsonReader reader = new JsonReader(httpRequest.getInputStream(), expectedStartSequence(formalParameters));
		try {
			return json.parse(reader, formalParameters);
		} catch (JsonException e) {
			throw new IllegalArgumentException(e.getMessage());
		} finally {
			reader.close();
		}
	}

	/**
	 * Parse JSON from input stream accordingly given type. Return parsed object.
	 * 
	 * @param inputStream JSON input stream,
	 * @param type expected type.
	 * @return parsed object.
	 * @throws IllegalArgumentException if JSON parse or reading from stream fails.
	 */
	@Override
	public Object read(InputStream inputStream, Type type) throws IOException {
		try {
			return json.parse(new InputStreamReader(inputStream, "UTF-8"), type);
		} catch (JsonException | ClassCastException | UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/** This method does nothing but is requested by interface. */
	@Override
	public void clean() {
	}

	private static String expectedStartSequence(Type[] formalParameters) {
		if (formalParameters.length == 0) {
			return "[";
		}
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		Type parameter = formalParameters[0];
		while (Types.isArrayLike(parameter)) {
			builder.append('[');
			if (!(parameter instanceof ParameterizedType)) {
				break;
			}
			ParameterizedType parameterizedType = (ParameterizedType) parameter;
			parameter = parameterizedType.getActualTypeArguments()[0];
		}
		return builder.toString();
	}

	/**
	 * JSON stream reader with mark symbol detection. Mark symbol is the first character from JSON stream and is used to detect
	 * if JSON is an array. If is not an array it should be object or primitive.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static class JsonReader extends Reader {
		/** Underlying JSON reader. */
		private final Reader reader;

		private final String expectedStartSequence;

		private final StringBuilder startSequenceBuffer;

		private boolean correctStartSequence;

		private int startSequenceOffset;

		private State state;

		/**
		 * Create JSON reader for given bytes stream encoded UTF-8.
		 * 
		 * @param inputStream JSON bytes input stream.
		 * @throws IOException if mark symbol reading fails.
		 */
		public JsonReader(InputStream inputStream, String expectedStartSequence) throws IOException {
			this(new InputStreamReader(inputStream, Charset.forName("UTF-8")), expectedStartSequence);
		}

		/**
		 * Create JSON reader for given characters stream. Reads first character from JSON stream and uses it to detect if
		 * stream is an array.
		 * 
		 * @param reader JSON reader.
		 * @throws IOException if mark symbol reading fails.
		 */
		public JsonReader(Reader reader, String expectedStartSequence) throws IOException {
			super();
			this.reader = reader;
			this.expectedStartSequence = expectedStartSequence;
			this.startSequenceBuffer = new StringBuilder();
			this.state = State.BUFFER_FILL;
		}

		/**
		 * Read characters from JSON stream. On first invocation of this method uses inner {@link MarkReader}. After that
		 * internal reader reference is replaced with wrapped JSON reader provided to this instance constructor.
		 * 
		 * @param cbuf destination buffer,
		 * @param off offset at which to start storing characters,
		 * @param len maximum number of characters to read.
		 * @return number of characters read.
		 */
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			if (len == 0) {
				return 0;
			}

			int charsCount = -1;
			switch (state) {
			case BUFFER_FILL:
				// fill synchronously the start sequence buffer
				for (;;) {
					int c = reader.read();
					if (c == -1) {
						return -1;
					}
					startSequenceBuffer.append((char) c);
					if (c != '[') {
						break;
					}
				}

				// asses and cache loaded start sequence correctness
				correctStartSequence = expectedStartSequence.equals(startSequenceBuffer.substring(0, startSequenceBuffer.length() - 1));

				// if start sequence is not the correct assume is a JSON stream without arguments array markers
				if (!correctStartSequence) {
					startSequenceBuffer.insert(0, '[');
				}
				startSequenceOffset = 0;
				state = State.BUFFER_READ;
				// fall through buffer read

			case BUFFER_READ:
				charsCount = Math.min(startSequenceBuffer.length() - startSequenceOffset, len);
				for (int i = 0; i < charsCount; ++i, ++startSequenceOffset) {
					cbuf[off + i] = startSequenceBuffer.charAt(startSequenceOffset);
				}

				if (startSequenceOffset == startSequenceBuffer.length()) {
					// entire start sequence was sent to parser, that is, caller of this read method
					state = correctStartSequence ? State.STANDARD_READ : State.ENHANCED_READ;
				}
				break;

			case STANDARD_READ:
				return reader.read(cbuf, off, len);

			case ENHANCED_READ:
				charsCount = reader.read(cbuf, off, len);
				if (charsCount == -1) {
					state = State.EOF;
					cbuf[off] = ']';
					return 1;
				}
				break;

			case EOF:
				return -1;
			}
			return charsCount;
		}

		/** This method does nothing since JSON reader is closed by outer logic. */
		@Override
		public void close() throws IOException {
		}

		/**
		 * States for JSON reader finite states machine.
		 * 
		 * @author Iulian Rotaru
		 */
		private enum State {
			/**
			 * Fill start sequence buffer. On this state characters read from underlying stream are stored into start sequence
			 * buffer.
			 */
			BUFFER_FILL,
			/** Read characters from start sequence buffer. */
			BUFFER_READ,
			/** Read characters from underlying stream to caller buffer. */
			STANDARD_READ,
			/** The same as standard read but takes care to detect EOF and append ']' */
			ENHANCED_READ,
			/**
			 * Simply returns -1 to signal EOF condition. This state is reached only after enhanced reading was properly close
			 * stream with ']'.
			 */
			EOF
		}
	}
}
