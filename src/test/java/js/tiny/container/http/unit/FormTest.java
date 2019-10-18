package js.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import js.tiny.container.http.form.Form;
import js.tiny.container.http.form.FormHandler;
import js.tiny.container.http.form.FormImpl;
import js.tiny.container.http.form.FormIterator;
import js.tiny.container.http.form.FormIteratorImpl;
import js.tiny.container.http.form.Part;
import js.tiny.container.http.form.UploadStream;
import js.tiny.container.http.form.UploadedFile;
import js.util.Classes;
import js.util.Strings;

@RunWith(MockitoJUnitRunner.class)
public class FormTest {
	private String content;
	private ServletInputStream stream;

	@Mock
	private HttpServletRequest request;

	@Before
	public void beforeTest() throws IOException {
		content = "";
		// in order for created mock to call read(byte[],int,int) from superclass need to set CALLS_REAL_METHODS
		stream = mock(ServletInputStream.class, Mockito.CALLS_REAL_METHODS);

		when(stream.read()).thenAnswer(new Answer<Integer>() {
			private int index;

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				if (index == content.length()) {
					return -1;
				}
				return (int) content.getBytes()[index++];
			}
		});

		when(request.getContentType()).thenReturn("multipart/form-data;boundary=XXX");
		when(request.getCharacterEncoding()).thenReturn("UTF-8");
		when(request.getInputStream()).thenReturn(stream);
		when(request.getContentLength()).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				return content.getBytes().length;
			}
		});
	}

	@Test
	public void form_FieldsGetters() throws IOException {
		content = "" + //
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
		content = "" + //
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
		content = "" + //
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
		content = "" + //
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
		content = "--XXX--";

		Form form = new FormImpl(request);
		form.getUploadedFile();
	}

	@Test
	public void formIterator_Constructor() throws IOException {
		content = "--XXX--";
		FormIterator form = new FormIteratorImpl(request);
		assertNotNull(Classes.getFieldValue(form, "fileItemIterator"));
	}

	@Test(expected = IOException.class)
	public void formIterator_ConstructorException() throws IOException {
		// missing boundary from content type throws FileUploadException
		when(request.getContentType()).thenReturn("multipart/form-data");
		new FormIteratorImpl(request);
	}

	@Test
	public void formIterator_forEach() throws IOException {
		content = "" + //
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
		content = "" + //
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
		content = "" + //
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
		content = "" + //
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
		content = "" + //
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
		content = "" + //
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
}
