package js.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.io.StreamHandler;
import js.lang.BugError;
import js.tiny.container.http.ContentType;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.EncoderKey;
import js.tiny.container.http.encoder.HttpEncoderProvider;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.http.encoder.ValueWriter;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class ServerEncodersTest {
	private ServerEncoders encoders;

	@Mock
	private HttpServletRequest request;

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
					name = "js/tiny/container/http/unit/HttpEncoderProvider";
				}
				return super.getResources(name);
			}
		});

		try {
			encoders = Classes.newInstance("js.tiny.container.http.encoder.ServerEncoders");
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

		assertNotNull(encoders);

		when(request.getContentType()).thenReturn("text/html");
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });
		assertNotNull(reader);
		assertTrue(reader instanceof ArgumentsReader);

		ValueWriter writer = encoders.getValueWriter(ContentType.TEXT_HTML);
		assertNotNull(writer);
		assertTrue(writer instanceof ValueWriter);
	}

	@Test
	public void getArgumentsReader() {
		when(request.getContentType()).thenReturn("text/xml");
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_MultipleArguments() {
		when(request.getContentType()).thenReturn("text/xml");
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class, Object.class });

		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_EmptyArguments() {
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] {});

		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.EmptyArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_QueryString() {
		when(request.getQueryString()).thenReturn("?name=value");
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.UrlQueryArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentsReader_NullContentType() {
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.JsonArgumentsReader", reader.getClass().getName());
	}

	/** If attempt to retrieve arguments reader for not registered Java type uses content type base and ignore Java type. */
	@Test
	public void getArgumentsReader_NotRegisteredType() {
		when(request.getContentType()).thenReturn("text/xml");
		ArgumentsReader reader = encoders.getArgumentsReader(request, new Type[] { Object.class });

		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.XmlArgumentsReader", reader.getClass().getName());
	}

	@Test
	public void getArgumentPartReader() {
		ArgumentsReader reader = encoders.getArgumentPartReader("text/xml", Document.class);
		assertNotNull(reader);
		assertEquals("js.tiny.container.http.encoder.XmlArgumentsReader", reader.getClass().getName());
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
		assertEquals("js.tiny.container.http.encoder.XmlValueWriter", encoders.getValueWriter(ContentType.TEXT_XML).getClass().getName());
		assertEquals("js.tiny.container.http.encoder.StreamValueWriter", encoders.getValueWriter(ContentType.APPLICATION_STREAM).getClass().getName());
		assertEquals("js.tiny.container.http.encoder.JsonValueWriter", encoders.getValueWriter(ContentType.APPLICATION_JSON).getClass().getName());
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
		when(request.getContentType()).thenReturn("text/html");
		encoders.getArgumentsReader(request, new Type[] { Object.class });
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	/**
	 * Mock for encoder provider configured on service provider file. This mock cannot be managed by Mockito because is
	 * configured into external resource file. For the same reason it should be public.
	 * 
	 * @author Iulian Rotaru
	 */
	public static class MockHttpEncoderProvider implements HttpEncoderProvider {
		@Override
		public Map<EncoderKey, ArgumentsReader> getArgumentsReaders() {
			ArgumentsReader reader = mock(ArgumentsReader.class);
			Map<EncoderKey, ArgumentsReader> readers = new HashMap<>();
			readers.put(new EncoderKey(ContentType.TEXT_HTML, Object.class), reader);
			return readers;
		}

		@Override
		public Map<ContentType, ValueWriter> getValueWriters() {
			ValueWriter writer = mock(ValueWriter.class);
			Map<ContentType, ValueWriter> writers = new HashMap<>();
			writers.put(ContentType.TEXT_HTML, writer);
			return writers;
		}
	}
}
