package js.tiny.container.mvc.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.ConfigBuilder;
import js.tiny.container.mvc.MethodsCache;
import js.tiny.container.mvc.ResourceServlet;
import js.tiny.container.service.InstancePreDestructor;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;

@SuppressWarnings({ "unused" })
@RunWith(MockitoJUnitRunner.class)
public class ResourceServletIntegrationTest {
	private static final String DESCRIPTOR = "" + //
			"<module package='js.tiny.container.mvc.it'>" + //
			"	<binding bind='App' in='javax.inject.Singleton' />" + //
			"	<binding bind='DefaultController' in='javax.inject.Singleton' />" + //
			"	<binding bind='IController' to='ControllerImpl' in='javax.inject.Singleton' />" + //
			"</module>";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/tomcat");
	}

	@Mock
	private ServletContext servletContext;
	@Mock
	private ServletConfig servletConfig;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	private OutputStream outputStream = new OutputStream();
	private InputStream inputStream = new InputStream();

	private TinyContainer container;

	private ResourceServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		InstancePreDestructor.resetCache();
		
		container = new TinyContainer();
		container.configure(new ConfigBuilder(DESCRIPTOR).build());
		// request context has thread scope and is critical to detach it
		// TODO: there are tests that leave not detached request context on thread !!!
		//container.getInstance(RequestContext.class).detach();

		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletConfig.getServletName()).thenReturn("ResourceServlet");
		when(servletContext.getServletContextName()).thenReturn("app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(httpRequest.getMethod()).thenReturn("GET");
		when(httpRequest.getContextPath()).thenReturn("/test");
		when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
		when(httpRequest.getInputStream()).thenReturn(inputStream);
		when(httpResponse.getOutputStream()).thenReturn(outputStream);

		servlet = new ResourceServlet();
		servlet.init(servletConfig);
	}

	@Test
	public void GivenActionPage_WhenService_ThenLoadHTML() throws Exception {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/index.xsp");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), equalTo("<html>\r\n</html>"));
	}

	@Test
	public void GivenActionText_WhenService_ThenLoadText() throws Exception {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/text.xsp");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/plain;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), equalTo("<html>\r\n</html>"));
	}

	@Test
	public void GivenActionPageOnDefaultController_WhenService_ThenLoadHTML() throws Exception {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/index.xsp");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), equalTo("<html>\r\n</html>"));
	}

	@Test
	public void GivenQueryParameters_WhenService_ThenLoadParameters() throws Exception {
		// given
		String query = "name=John%20Doe&profession=freelancer&age=48";
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/person-view.xsp?" + query);
		when(httpRequest.getQueryString()).thenReturn(query);

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), containsString("John Doe"));
		assertThat(outputStream.toString(), containsString("freelancer"));
		assertThat(outputStream.toString(), containsString("48"));
	}

	@Test
	public void GivenUrlEncodedForm_WhenService_ThenLoadAttributes() throws Exception {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/person-view.xsp");
		when(httpRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");
		inputStream.content("name=John Doe&profession=freelancer&age=48");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), containsString("John Doe"));
		assertThat(outputStream.toString(), containsString("freelancer"));
		assertThat(outputStream.toString(), containsString("48"));
	}

	@Test
	public void GivenMultipartForm_WhenService_ThenLoadFields() throws ServletException, IOException {
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/process-multipart-form.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
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
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), containsString("John Doe"));
		assertThat(outputStream.toString(), containsString("freelancer"));
		assertThat(outputStream.toString(), containsString("48"));
	}

	@Test
	public void GivenFormObject_WhenService_ThenLoadFields() throws ServletException, IOException {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/process-form-object.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
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
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		assertThat(outputStream.toString(), containsString("John Doe"));
		assertThat(outputStream.toString(), containsString("freelancer"));
		assertThat(outputStream.toString(), containsString("48"));
	}

	@Test
	public void GivenUploadForm_WhenService_ThenUploadFileContent() throws ServletException, IOException {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/upload-form.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		App app = container.getInstance(App.class);
		assertThat(app.content, containsString("some random plain text content"));
	}

	@Test
	public void GivenUploadMultipartForm_WhenService_ThenUploadFileContent() throws ServletException, IOException {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/upload-multipart-form.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setContentType("text/html;charset=UTF-8");
		verify(httpResponse, times(1)).setHeader("Cache-Control", "no-cache");
		verify(httpResponse, times(1)).addHeader("Cache-Control", "no-store");
		verify(httpResponse, times(1)).setHeader("Pragma", "no-cache");
		verify(httpResponse, times(1)).setDateHeader("Expires", 0);

		App app = container.getInstance(App.class);
		assertThat(app.content, containsString("some random plain text content"));
	}

	@Test
	public void GivenMissingAction_WhenService_Then404() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/missing-action.xsp");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).sendError(404, "/test/resource/missing-action.xsp");
		verify(httpResponse, times(0)).setContentType(any());
		verify(httpResponse, times(0)).setContentLength(anyInt());
		assertThat(outputStream.toString(), emptyString());
	}

	@Test
	public void GivenMissingForm_WhenService_Then404() throws IOException, ServletException {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/missing-form.xsp");
		// when(request.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).sendError(404, "/test/resource/missing-form.xsp");
		verify(httpResponse, times(0)).setContentType(any());
		verify(httpResponse, times(0)).setContentLength(anyInt());
		assertThat(outputStream.toString(), emptyString());
	}

	@Test
	public void GivenPrivateAction_WhenService_ThenRequestAuthentication() throws IOException, ServletException {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/private-action.xsp");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpRequest, times(1)).authenticate(httpResponse);
	}

	@Test
	public void GivenPrivateForm_WhenService_ThenRequestAuthentication() throws IOException, ServletException {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/private-form.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpRequest, times(1)).authenticate(httpResponse);
	}

	/**
	 * Providing wrong arguments to resource method will rise illegal argument exception. Arguments are part of method signature
	 * and considered method not found if not correct.
	 */
	@Test
	public void GivenIllegalArgument_WhenService_Then404() throws Exception {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/person-view.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).sendError(404, "/test/resource/person-view.xsp");
		verify(httpResponse, times(0)).setContentType(any());
		verify(httpResponse, times(0)).setContentLength(anyInt());
		assertThat(outputStream.toString(), emptyString());
	}

	@Test
	public void GivenActionException_WhenService_Then500() throws Exception {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/action-exception.xsp");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).sendError(500, "Action exception.");
		verify(httpResponse, times(0)).setContentType(any());
		verify(httpResponse, times(0)).setContentLength(anyInt());
		assertThat(outputStream.toString(), emptyString());
	}

	@Test
	public void GivenFormException_WhenService_Then500() throws Exception {
		// given
		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getRequestURI()).thenReturn("/test/resource/form-exception.xsp");
		when(httpRequest.getContentType()).thenReturn("multipart/form-data; boundary=XXX");
		inputStream.content("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).sendError(500, "Form exception.");
		verify(httpResponse, times(0)).setContentType(any());
		verify(httpResponse, times(0)).setContentLength(anyInt());
		assertThat(outputStream.toString(), emptyString());
	}

	// --------------------------------------------------------------------------------------------

	private static class InputStream extends ServletInputStream {
		private StringReader stringReader;

		public void content(String content) {
			this.stringReader = new StringReader(content);
		}

		@Override
		public int read() throws IOException {
			return stringReader.read();
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

	private static class OutputStream extends ServletOutputStream {
		private StringWriter stringWriter;

		public OutputStream() {
			this.stringWriter = new StringWriter();
		}

		@Override
		public void write(int b) throws IOException {
			stringWriter.append((char) b);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}

		@Override
		public String toString() {
			return stringWriter.toString();
		}
	}
}
