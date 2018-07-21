package js.http.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.http.ContentType;
import js.http.encoder.ArgumentsReader;
import js.http.encoder.EncoderKey;
import js.http.encoder.HttpEncoderProvider;
import js.http.encoder.ServerEncoders;
import js.http.encoder.ValueWriter;
import js.io.StreamHandler;
import js.lang.BugError;
import js.unit.HttpServletRequestStub;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

public class ServerEncodersUnitTest {
	private ServerEncoders encoders;

	@Before
	public void beforeTest() {
		encoders = ServerEncoders.getInstance();
	}

	@Test
	public void constructor() {
		assertNotNull(encoders);
	}

	@Test
	public void constructor_EncoderProvider() throws Exception {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new ClassLoader() {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				if (name.contains("HttpEncoderProvider")) {
					name = "js/http/test/HttpEncoderProvider";
				}
				return super.getResources(name);
			}
		});

		try {
			encoders = Classes.newInstance("js.http.encoder.ServerEncoders");
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

		assertNotNull(encoders);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.contentType = "text/html";
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });
		assertNotNull(reader);
		assertTrue(reader instanceof MockArgumentsReader);

		ValueWriter writer = encoders.getValueWriter(ContentType.TEXT_HTML);
		assertNotNull(writer);
		assertTrue(writer instanceof MockValueWriter);
	}

	@Test
	public void getArgumentsReader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.contentType = "text/xml";
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_MultipleArguments() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.contentType = "text/xml";
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class, Object.class });

		assertNotNull(reader);
		assertEquals("js.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_EmptyArguments() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.contentType = "text/xml";
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] {});

		assertNotNull(reader);
		assertEquals("js.http.encoder.EmptyArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_QueryString() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.queryString = "?name=value";
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.http.encoder.UrlQueryArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_NullContentType() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.http.encoder.JsonArgumentsReader", reader.getClass().getName());
	}

	/** If attempt to retrieve arguments reader for not registered Java type uses content type base and ignore Java type. */
	@Test
	public void getArgumentsReader_NotRegisteredType() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.contentType = "text/xml";
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentPartReader() {
		ArgumentsReader reader = encoders.getArgumentPartReader("text/xml", Document.class);
		assertNotNull(reader);
		assertEquals("js.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getArgumentPartReader_NotPartReader() {
		encoders.getArgumentPartReader("multipart/form-data", Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getArgumentPartReader_NotRegisteredType() {
		encoders.getArgumentPartReader("text/html", Object.class);
	}

	@Test
	public void getValueWriter() {
		assertEquals("js.http.encoder.XmlValueWriter", encoders.getValueWriter(ContentType.TEXT_XML).getClass().getName());
		assertEquals("js.http.encoder.StreamValueWriter", encoders.getValueWriter(ContentType.APPLICATION_STREAM).getClass().getName());
		assertEquals("js.http.encoder.JsonValueWriter", encoders.getValueWriter(ContentType.APPLICATION_JSON).getClass().getName());
	}

	@Test(expected = BugError.class)
	public void getValueWriter_NotFound() {
		encoders.getValueWriter(ContentType.TEXT_HTML);
	}

	@Test
	public void getContentTypeForValue() {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		assertEquals(ContentType.TEXT_XML, encoders.getContentTypeForValue(builder.createXML("root")));

		assertEquals(ContentType.APPLICATION_STREAM, encoders.getContentTypeForValue(new StreamHandler<OutputStream>(null) {
			@Override
			protected void handle(OutputStream outputStream) throws IOException {

			}
		}));

		assertEquals(ContentType.APPLICATION_JSON, encoders.getContentTypeForValue(new Object()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getArgumentsReader_BadContentType() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.contentType = "text/html";
		encoders.getArgumentsReader(request, new Type[] { Object.class });
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@SuppressWarnings("unchecked")
	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private String contentType;
		private String queryString;

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public String getQueryString() {
			return queryString;
		}
	}

	public static class MockHttpEncoderProvider implements HttpEncoderProvider {
		@Override
		public Map<EncoderKey, ArgumentsReader> getArgumentsReaders() {
			Map<EncoderKey, ArgumentsReader> readers = new HashMap<>();
			readers.put(new EncoderKey(ContentType.TEXT_HTML, Object.class), new MockArgumentsReader());
			return readers;
		}

		@Override
		public Map<ContentType, ValueWriter> getValueWriters() {
			Map<ContentType, ValueWriter> writers = new HashMap<>();
			writers.put(ContentType.TEXT_HTML, new MockValueWriter());
			return writers;
		}
	}

	private static class MockArgumentsReader implements ArgumentsReader {
		@Override
		public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
			return null;
		}

		@Override
		public void clean() {
		}
	}

	private static class MockValueWriter implements ValueWriter {
		@Override
		public void write(HttpServletResponse httpResponse, Object value) throws IOException {
		}
	}
}
