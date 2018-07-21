package js.http.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import js.http.form.Form;
import js.http.form.FormHandler;
import js.http.form.FormImpl;
import js.http.form.FormIterator;
import js.http.form.FormIteratorImpl;
import js.http.form.Part;
import js.http.form.UploadStream;
import js.http.form.UploadedFile;
import js.unit.HttpServletRequestStub;
import js.util.Classes;
import js.util.Strings;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Before;
import org.junit.Test;

public class FormUnitTest {
	private MockHttpServletRequest request;

	@Before
	public void beforeTest() {
		request = new MockHttpServletRequest();
	}

	@Test
	public void form_FieldsGetters() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"profession\"\r\n" + //
				"\r\n" + //
				"freelancer\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"age\"\r\n" + //
				"\r\n" + //
				"48\r\n" + //
				"--XXX--";

		Form form = new FormImpl(request);

		assertTrue(form.hasField("name"));
		assertFalse(form.hasField("file"));
		assertFalse(form.hasField("fake"));
		assertFalse(form.hasUpload("name"));

		assertEquals("John Doe", form.getValue("name"));
		assertEquals("freelancer", form.getValue("profession"));
		assertEquals("48", form.getValue("age"));
		assertEquals(48, (int) form.getValue("age", int.class));

		assertNull(form.getValue("fake"));
		assertEquals("", form.getValue("fake", String.class));
		assertEquals(0, (int) form.getValue("fake", int.class));
	}

	@Test
	public void form_getFile() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";

		Form form = new FormImpl(request);

		assertTrue(form.hasUpload("file"));
		assertFalse(form.hasUpload("name"));
		assertFalse(form.hasUpload("fake"));
		assertFalse(form.hasField("file"));

		assertEquals("some random plain text content\r\ncar return is part of content", Strings.load(form.getFile("file")));
		assertNull(form.getFile("fake"));
	}

	@Test
	public void form_getUploadedFile() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";

		Form form = new FormImpl(request);
		UploadedFile uploadedFile = form.getUploadedFile();

		assertTrue(uploadedFile.is("file"));
		assertEquals("some random plain text content\r\ncar return is part of content", Strings.load(uploadedFile.getFile()));
		assertNull(form.getUploadedFile("fake"));
		assertEquals("test-file.txt", uploadedFile.getFileName());
		assertTrue(uploadedFile.toString().startsWith("file:test-file.txt:text/plain:"));

		File file = File.createTempFile("test", null);
		File directory = file.getParentFile();
		file = new File(directory, uploadedFile.getFileName());
		file.deleteOnExit();

		uploadedFile.moveTo(directory);
		assertEquals("some random plain text content\r\ncar return is part of content", Strings.load(file));
	}

	@Test(expected = IllegalArgumentException.class)
	public void form_getUploadedFile_FormWithFields() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--";

		Form form = new FormImpl(request);
		form.getUploadedFile();
	}

	@Test(expected = IllegalArgumentException.class)
	public void form_getUploadedFile_Empty() throws IOException {
		request.stream.content = "--XXX--";

		Form form = new FormImpl(request);
		form.getUploadedFile();
	}

	@Test
	public void formIterator_Constructor() throws IOException {
		request.stream.content = "--XXX--";
		FormIterator form = new FormIteratorImpl(request);
		assertNotNull(Classes.getFieldValue(form, "fileItemIterator"));
	}

	@Test(expected = IOException.class)
	public void formIterator_ConstructorException() throws IOException {
		// missing boundary from content type throws FileUploadException
		request.contentType = "multipart/form-data";
		new FormIteratorImpl(request);
	}

	@Test
	public void formIterator_forEach() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";
		FormIterator form = new FormIteratorImpl(request);

		class MockFormHandler extends FormHandler {
			private int streamProbe;
			private int fieldProbe;

			@Override
			protected void stream(String name, String fileName, String contentType, InputStream inputStream) throws Throwable {
				super.stream(name, fileName, contentType, inputStream);
				++streamProbe;
				assertEquals("file", name);
				assertEquals("test-file.txt", fileName);
				assertEquals("text/plain", contentType);
				assertEquals("some random plain text content\r\ncar return is part of content", Strings.load(inputStream));
			}

			@Override
			protected void field(String name, String value) throws Throwable {
				super.field(name, value);
				++fieldProbe;
				assertEquals("name", name);
				assertEquals("John Doe", value);
			}
		}
		MockFormHandler handler = new MockFormHandler();
		form.forEach(handler);
		assertEquals(1, handler.streamProbe);
		assertEquals(1, handler.fieldProbe);
	}

	@Test(expected = RuntimeException.class)
	public void formIterator_forEach_FieldException() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--";
		FormIterator form = new FormIteratorImpl(request);
		form.forEach(new FormHandler() {
			@Override
			protected void field(String name, String value) throws Throwable {
				throw new RuntimeException();
			}
		});
	}

	@Test(expected = IOException.class)
	public void formIterator_forEach_StreamIOException() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";
		FormIterator form = new FormIteratorImpl(request);
		form.forEach(new FormHandler() {
			@Override
			protected void stream(String name, String fileName, String contentType, InputStream inputStream) throws Throwable {
				throw new IOException();
			}
		});
	}

	@Test(expected = RuntimeException.class)
	public void formIterator_forEach_StreamRuntimeException() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";
		FormIterator form = new FormIteratorImpl(request);
		form.forEach(new FormHandler() {
			@Override
			protected void stream(String name, String fileName, String contentType, InputStream inputStream) throws Throwable {
				throw new RuntimeException();
			}
		});
	}

	@Test
	public void formIterator_hasNext_FileUploadException() throws IOException {
		FormIterator form = new FormIteratorImpl(new FileItemIterator() {
			@Override
			public boolean hasNext() throws FileUploadException, IOException {
				throw new FileUploadException();
			}

			@Override
			public FileItemStream next() throws FileUploadException, IOException {
				return null;
			}
		});
		form.hasNext();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void formIterator_remove() throws IOException {
		FormIterator form = new FormIteratorImpl(request);
		form.remove();
	}

	@Test
	public void formIterator_PartIsName() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX\r\n" + //
				"--XXX--";
		FormIterator form = new FormIteratorImpl(request);

		boolean partFound = false;
		while (form.hasNext()) {
			Part part = form.next();
			if (part.is("name")) {
				partFound = true;
			}
		}

		assertTrue(partFound);
	}

	@Test
	public void formIterator_FileName() throws IOException {
		request.stream.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file1\"; filename=\"c:\\\\temp\\test-file1.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file2\"; filename=\"/var/test-file2.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"--XXX--";
		FormIterator form = new FormIteratorImpl(request);
		assertTrue(form.hasNext());
		assertEquals("test-file1.txt", ((UploadStream) form.next()).getFileName());
		assertTrue(form.hasNext());
		assertEquals("test-file2.txt", ((UploadStream) form.next()).getFileName());
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@SuppressWarnings("unchecked")
	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private String contentType = "multipart/form-data;boundary=XXX";
		private MockServletInputStream stream = new MockServletInputStream();

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public String getCharacterEncoding() {
			return "UTF-8";
		}

		@Override
		public int getContentLength() {
			return stream.content.getBytes().length;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return stream;
		}
	}

	private static class MockServletInputStream extends ServletInputStream {
		private String content = "";
		private int index;
		private boolean exception;

		@Override
		public int read() throws IOException {
			if (exception) {
				throw new IOException();
			}
			if (index == content.length()) {
				return -1;
			}
			return content.getBytes()[index++];
		}

		@Override
		public boolean isFinished() {
			return false;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
		}
	}
}
