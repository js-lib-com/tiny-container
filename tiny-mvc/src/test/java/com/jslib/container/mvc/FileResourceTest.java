package com.jslib.container.mvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.http.ContentType;
import com.jslib.util.Classes;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class FileResourceTest {
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private ServletOutputStream outputStream;

	@Before
	public void beforeTest() throws IOException {
		when(httpResponse.getOutputStream()).thenReturn(outputStream);
	}

	@Test
	public void GivenPathStringAndNoContentType_WhenConstructor_ThenValidInstance() {
		assertResource(new FileResource("src/test/resources/page.html"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenNullPath_WhenConstructor_ThenIllegalArgument() {
		new FileResource((String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenEmptyPath_WhenConstructor_ThenIllegalArgument() {
		new FileResource("");
	}

	@Test
	public void GivenPathStringAndContentType_WhenConstructor_ThenValidInstance() {
		assertResource(new FileResource("src/test/resources/page.html", ContentType.TEXT_HTML));
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenNullContentType_WhenConstructor_ThenIllegalArgument() {
		new FileResource("fixture/mvc/page.html", null);
	}

	@Test
	public void GivenFileAndNoContentType_WhenConstructor_ThenValidInstance() {
		assertResource(new FileResource(new File("src/test/resources/page.html")));
	}

	@Test
	public void GivenFileAndContentType_WhenConstructor_ThenValidInstance() {
		assertResource(new FileResource(new File("src/test/resources/page.html"), ContentType.TEXT_HTML));
	}

	private static void assertResource(FileResource resource) {
		assertEquals(new File("src/test/resources/page.html"), Classes.getFieldValue(resource, "file"));
		assertEquals("text/html;charset=UTF-8", Classes.getFieldValue(resource, "contentType"));
	}

	@Test
	public void GivenHttpResponse_WhenSerialize_ThenWriteHeadersAndOutputStream() throws IOException {
		// given

		// when
		FileResource resource = new FileResource(new File("src/test/resources/page.html"), ContentType.TEXT_HTML);
		resource.serialize(httpResponse);

		// then
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Content-Length", "15");
		verify(httpResponse, times(1)).getOutputStream();

		ArgumentCaptor<byte[]> bytesArg = ArgumentCaptor.forClass(byte[].class);
		ArgumentCaptor<Integer> offsetArg = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<Integer> lengthArg = ArgumentCaptor.forClass(Integer.class);
		verify(outputStream).write(bytesArg.capture(), offsetArg.capture(), lengthArg.capture());
		assertThat(new String(bytesArg.getValue(), offsetArg.getValue(), lengthArg.getValue()), equalTo("<html>\r\n</html>"));
	}

	@Test
	public void GivenOutputStreamIOException_WhenSerialize_ThenException() throws IOException {
		// given
		doThrow(IOException.class).when(outputStream).write(any(byte[].class), eq(0), anyInt());

		// when
		FileResource resource = new FileResource(new File("src/test/resources/page.html"), ContentType.TEXT_HTML);
		resource.serialize(httpResponse);
	}
}
