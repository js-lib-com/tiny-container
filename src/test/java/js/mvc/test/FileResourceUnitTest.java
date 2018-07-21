package js.mvc.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import js.http.ContentType;
import js.mvc.FileResource;
import js.unit.HttpServletResponseStub;
import js.util.Classes;
import js.util.Strings;

import org.junit.Test;

public class FileResourceUnitTest {
	@Test
	public void constructor_Path() {
		assertResource(new FileResource("fixture/mvc/page.html"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructor_NullPath() {
		new FileResource((String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructor_EmptyPath() {
		new FileResource("");
	}

	@Test
	public void constructor_PathContentType() {
		assertResource(new FileResource("fixture/mvc/page.html", ContentType.TEXT_HTML));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructor_PathNullContentType() {
		new FileResource("fixture/mvc/page.html", null);
	}

	@Test
	public void constructor_File() {
		assertResource(new FileResource(new File("fixture/mvc/page.html")));
	}

	@Test
	public void constructor_FileContentType() {
		assertResource(new FileResource(new File("fixture/mvc/page.html"), ContentType.TEXT_HTML));
	}

	private static void assertResource(FileResource resource) {
		assertEquals(new File("fixture/mvc/page.html"), Classes.getFieldValue(resource, "file"));
		assertEquals("text/html;charset=UTF-8", Classes.getFieldValue(resource, "contentType"));
	}

	@Test
	public void serialize() throws IOException {
		MockHttpServletResponse httpResponse = new MockHttpServletResponse();
		FileResource resource = new FileResource(new File("fixture/mvc/page.html"), ContentType.TEXT_HTML);
		resource.serialize(httpResponse);

		assertEquals("no-cache;no-store", httpResponse.headers.get("Cache-Control"));
		assertEquals("no-cache", httpResponse.headers.get("Pragma"));
		assertEquals("Thu, 01 Jan 1970 02:00:00 GMT", httpResponse.headers.get("Expires"));
		assertEquals("text/html;charset=UTF-8", httpResponse.headers.get("Content-Type"));
		assertEquals("497", httpResponse.headers.get("Content-Length"));
		assertEquals(497, httpResponse.stream.writeProbe);
	}

	@Test
	public void serialize_IOException() {

	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private Map<String, String> headers = new HashMap<>();
		private MockServletOutputStream stream = new MockServletOutputStream();

		@Override
		public void setContentType(String type) {
			headers.put("Content-Type", type);
		}

		@Override
		public void setHeader(String name, String value) {
			headers.put(name, value);
		}

		@Override
		public void addHeader(String name, String value) {
			headers.put(name, Strings.concat(headers.get(name), ';', value));
		}

		@Override
		public void setDateHeader(String name, long value) {
			DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
			headers.put(name, dateFormat.format(new Date(value)));
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return stream;
		}
	}

	private static class MockServletOutputStream extends ServletOutputStream {
		private int writeProbe;

		@Override
		public void write(int b) throws IOException {
			++writeProbe;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}
	}
}
