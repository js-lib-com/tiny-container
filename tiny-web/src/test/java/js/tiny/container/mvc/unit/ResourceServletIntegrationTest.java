package js.tiny.container.mvc.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Path;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import js.tiny.container.ContainerSPI;
import js.tiny.container.core.App;
import js.tiny.container.core.AppContext;
import js.tiny.container.core.Factory;
import js.tiny.container.http.ContentType;
import js.tiny.container.http.form.Form;
import js.tiny.container.http.form.FormField;
import js.tiny.container.http.form.FormIterator;
import js.tiny.container.http.form.Part;
import js.tiny.container.http.form.UploadStream;
import js.tiny.container.http.form.UploadedFile;
import js.tiny.container.mvc.AbstractView;
import js.tiny.container.mvc.ResourceServlet;
import js.tiny.container.mvc.View;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.unit.HttpServletRequestStub;
import js.tiny.container.unit.HttpServletResponseStub;
import js.tiny.container.unit.HttpSessionStub;
import js.tiny.container.unit.ServletConfigStub;
import js.tiny.container.unit.ServletContextStub;
import js.tiny.container.unit.TestContext;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

@SuppressWarnings({ "unused" })
public class ResourceServletIntegrationTest {
	private static final String DESCRIPTOR = "" + //
			"<?xml version='1.0' encoding='UTF-8'?>" + //
			"<test-config>" + //
			"	<managed-classes>" + //
			"		<app interface='js.tiny.container.core.App' class='js.tiny.container.mvc.unit.ResourceServletIntegrationTest$MockApp' />" + //
			"		<controller class='js.tiny.container.mvc.unit.ResourceServletIntegrationTest$DefaultController' />" + //
			"		<controller interface='js.tiny.container.mvc.unit.ResourceServletIntegrationTest$ControllerInterface' class='js.tiny.container.mvc.unit.ResourceServletIntegrationTest$ControllerImpl' type='PROXY' />" + //
			"	</managed-classes>" + //
			"</test-config>";

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private ContainerSPI container;
	private MockApp app;
	private MockServletContext servletContext;
	private MockServletConfig servletConfig;
	private RequestContext requestContext;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@Before
	public void beforeTest() throws Exception {
		container = (ContainerSPI) TestContext.start(DESCRIPTOR);

		app = (MockApp) Factory.getInstance(App.class);

		servletContext = new MockServletContext();
		servletContext.attributes.put(TinyContainer.ATTR_INSTANCE, container);

		servletConfig = new MockServletConfig();
		servletConfig.servletContext = servletContext;

		request = new MockHttpServletRequest();
		request.servletContext = servletContext;
		response = new MockHttpServletResponse();

		requestContext = Factory.getInstance(RequestContext.class);
	}

	@Test
	public void action() throws Exception {
		request.headers.put("Content-Type", null);
		execute("GET", "/resource/index.xsp");

		assertEquals(200, response.status);
		assertEquals("text/html;charset=UTF-8", response.headers.get("Content-Type"));
		assertEquals("no-cache, no-store", response.headers.get("Cache-Control"));
		assertEquals("no-cache", response.headers.get("Pragma"));
		assertTrue(response.getBody().startsWith("<!DOCTYPE html"));
	}

	@Test
	public void actionOnDefaultController() throws Exception {
		request.headers.put("Content-Type", null);
		execute("GET", "/index.xsp");

		assertEquals(200, response.status);
		assertEquals("text/html;charset=UTF-8", response.headers.get("Content-Type"));
		assertEquals("no-cache, no-store", response.headers.get("Cache-Control"));
		assertEquals("no-cache", response.headers.get("Pragma"));
		assertTrue(response.getBody().startsWith("<!DOCTYPE html"));
	}

	@Test
	public void urlParameters() throws Exception {
		request.headers.put("Content-Type", null);
		execute("GET", "/resource/person-view.xsp?name=John%20Doe&profession=freelancer&age=48");

		assertEquals(200, response.status);
		assertEquals("text/html;charset=UTF-8", response.headers.get("Content-Type"));
		assertTrue(response.getBody().startsWith("<!DOCTYPE html"));
		Person p = (Person) app.content;
		assertEquals("John Doe", p.name);
		assertEquals("freelancer", p.profession);
		assertEquals(48, p.age);
	}

	@Test
	public void urlEncodedForm() throws Exception {
		request.headers.put("Content-Type", "application/x-www-form-urlencoded");
		request.content = "name=John Doe&profession=freelancer&age=48";
		execute("POST", "/resource/person-view.xsp");

		assertEquals(200, response.status);
		assertEquals("text/html;charset=UTF-8", response.headers.get("Content-Type"));
		assertTrue(response.getBody().startsWith("<!DOCTYPE html"));
		Person p = (Person) app.content;
		assertEquals("John Doe", p.name);
		assertEquals("freelancer", p.profession);
		assertEquals(48, p.age);
	}

	@Test
	public void multipartForm() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
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

		execute("POST", "/resource/process-multipart-form.xsp");

		assertEquals(200, response.status);
		assertEquals("text/html;charset=UTF-8", response.headers.get("Content-Type"));
		assertTrue(response.getBody().startsWith("<!DOCTYPE html"));
		Person p = (Person) app.content;
		assertEquals("John Doe", p.name);
		assertEquals("freelancer", p.profession);
		assertEquals(48, p.age);
	}

	@Test
	public void formObject() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
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

		execute("POST", "/resource/process-form-object.xsp");

		assertEquals(200, response.status);
		assertEquals("text/html;charset=UTF-8", response.headers.get("Content-Type"));
		assertTrue(response.getBody().startsWith("<!DOCTYPE html"));
		Person p = (Person) app.content;
		assertEquals("John Doe", p.name);
		assertEquals("freelancer", p.profession);
		assertEquals(48, p.age);
	}

	@Test
	public void uploadForm() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";

		execute("POST", "/resource/upload-form.xsp");

		assertEquals(200, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void uploadMultipartForm() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--";

		execute("POST", "/resource/upload-multipart-form.xsp");

		assertEquals(200, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void missingAction() throws Exception {
		execute("GET", "/resource/fake-action.xsp");

		assertEquals(404, response.status);
		assertEquals("/tests/resource/fake-action.xsp", response.getBody());
		assertEquals(0, response.contentLegth);
		assertNull(response.getContentType());
	}

	@Test
	public void missingForm() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--";

		execute("POST", "/resource/fake-form.xsp");

		assertEquals(404, response.status);
		assertEquals("/tests/resource/fake-form.xsp", response.getBody());
		assertEquals(0, response.contentLegth);
		assertNull(response.getContentType());
	}

	@Test
	public void notAuthorizedAction() throws Throwable {
		Classes.setFieldValue(container, TinyContainer.class, "loginPage", "/test/login-page.html");
		execute("GET", "/resource/private-action.xsp");

		assertEquals(307, response.status);
		assertEquals("/test/login-page.html", response.redirectLocation);
	}

	@Test
	public void notAuthorizedForm() throws Throwable {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--";

		Classes.setFieldValue(container, TinyContainer.class, "loginPage", "/test/login-page.html");
		execute("POST", "/resource/private-form.xsp");

		assertEquals(307, response.status);
		assertEquals("/test/login-page.html", response.redirectLocation);
	}

	/**
	 * Providing wrong arguments to resource method will rise illegal argument exception. Arguments are part of method signature
	 * and considered method not found if not correct.
	 */
	@Test
	public void illegalArgumentException() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--";

		execute("POST", "/resource/person-view.xsp");

		assertEquals(404, response.status);
		assertEquals(0, response.contentLegth);
		assertNull(response.getContentType());
		assertEquals("/tests/resource/person-view.xsp", response.getBody());
	}

	@Test
	public void actionException() throws Exception {
		execute("GET", "/resource/action-exception.xsp");

		assertEquals(500, response.status);
		assertEquals("Generated exception.", response.getBody());
		assertNull(response.getContentType());
		assertEquals(0, response.contentLegth);
	}

	@Test
	public void formException() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.content = "" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--";

		execute("POST", "/resource/form-exception.xsp");

		assertEquals(500, response.status);
		assertEquals("Generated exception.", response.getBody());
		assertNull(response.getContentType());
		assertEquals(0, response.contentLegth);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void execute(String httpMethod, String requestPath) throws Exception {
		ResourceServlet servlet = new ResourceServlet();
		servlet.init(servletConfig);

		request.method = httpMethod;
		request.requestURI = "/tests" + requestPath;
		request.headers.put("X-Requested-With", null);
		request.headers.put("Accept", "text/html");
		requestContext.setRequestPath(requestPath);
		requestContext.attach(request, response);

		servlet.service(request, response);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockApp extends App {
		Object content;

		public MockApp(AppContext context) {
			super(context);
		}
	}

	private static class Person {
		String name;
		String profession;
		int age;

		public Person() {
		}

		public Person(String name, String profession, int age) {
			this.name = name;
			this.profession = profession;
			this.age = age;
		}
	}

	@Remote
	@PermitAll
	private static class DefaultController {
		public View index() {
			return new MockView();
		}
	}

	private static interface ControllerInterface {
		View index();

		View personView(String name, String profession, int age);

		View processForm(Form form);

		View processMultipartForm(FormIterator form);

		View processFormObject(Person person);

		View uploadForm(Form form) throws FileNotFoundException, IOException;

		View uploadMultipartForm(FormIterator form) throws IOException;

		@RolesAllowed("*")
		View privateAction();

		@RolesAllowed("*")
		View privateForm(Form form);

		View actionException();

		View formException(Form form);
	}

	@Remote
	@Path("resource")
	@PermitAll
	private static class ControllerImpl implements ControllerInterface {
		@Override
		public View index() {
			return new MockView();
		}

		@Override
		public View personView(String name, String profession, int age) {
			Person p = new Person();
			p.name = name;
			p.profession = profession;
			p.age = age;

			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = p;
			return new MockView();
		}

		@Override
		public View processForm(Form form) {
			Person p = new Person();
			p.name = form.getValue("name");
			p.profession = form.getValue("profession");
			p.age = form.getValue("age", Integer.class);

			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = p;
			return new MockView();
		}

		@Override
		public View processMultipartForm(FormIterator form) {
			Person p = new Person();
			for (Part item : form) {
				if (item instanceof FormField) {
					FormField f = (FormField) item;
					if (f.getName().equals("name")) {
						p.name = f.getValue();
					} else if (f.getName().equals("profession")) {
						p.profession = f.getValue();
					} else if (f.getName().equals("age")) {
						p.age = f.getValue(Integer.class);
					}
				}
			}

			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = p;
			return new MockView();
		}

		@Override
		public View processFormObject(Person person) {
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = person;
			return new MockView();
		}

		@Override
		public View uploadForm(Form form) throws FileNotFoundException, IOException {
			View view = new MockView();
			UploadedFile upload = form.getUploadedFile("file");
			if (!upload.getContentType().equals("text/plain"))
				return view;
			if (!upload.getFileName().equals("test-file.txt"))
				return view;
			StringWriter writer = new StringWriter();
			Files.copy(new FileReader(upload.getFile()), writer);
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = writer.toString();
			return view;
		}

		@Override
		public View uploadMultipartForm(FormIterator form) throws IOException {
			View view = new MockView();
			for (Part item : form) {
				if (item instanceof UploadStream) {
					UploadStream upload = (UploadStream) item;
					if (!upload.getContentType().equals("text/plain")) {
						return view;
					}
					if (!upload.getFileName().equals("test-file.txt")) {
						return view;
					}
					StringWriter writer = new StringWriter();
					Files.copy(new InputStreamReader(upload.openStream(), "UTF-8"), writer);
					MockApp app = (MockApp) Factory.getInstance(App.class);
					app.content = writer.toString();
				}
			}
			return view;
		}

		@Override
		public View privateAction() {
			return new MockView();
		}

		@Override
		public View privateForm(Form form) {
			return new MockView();
		}

		@Override
		public View actionException() {
			throw new RuntimeException("Generated exception.");
		}

		@Override
		public View formException(Form form) {
			throw new RuntimeException("Generated exception.");
		}
	}

	private static class MockView extends AbstractView {
		@Override
		protected ContentType getContentType() {
			return ContentType.TEXT_HTML;
		}

		@Override
		public void serialize(java.io.OutputStream stream) throws IOException {
			Files.copy(new FileInputStream("fixture/mvc/page.html"), stream);
		}
	}

	private static class MockServletContext extends ServletContextStub {
		Map<String, Object> attributes = new HashMap<>();

		@Override
		public String getServletContextName() {
			return "app";
		}

		@Override
		public String getRealPath(String resource) {
			return ".";
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		@Override
		public String getInitParameter(String name) {
			return null;
		}
	}

	private static class MockServletConfig extends ServletConfigStub {
		private ServletContext servletContext = new MockServletContext();

		@Override
		public ServletContext getServletContext() {
			return servletContext;
		}

		@Override
		public String getServletName() {
			return "ResourceServlet";
		}

		@Override
		public String getInitParameter(String name) {
			return null;
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		HttpServletResponse httpResponse;
		ServletContext servletContext;

		String method;
		String requestURI;
		Map<String, String> headers = new HashMap<>();
		HttpSession session;
		String content;

		@Override
		public String getMethod() {
			return method;
		}

		@Override
		public String getContextPath() {
			if (requestURI == null) {
				return null;
			}
			int beginIndex = 0;
			if (requestURI.startsWith("/")) {
				beginIndex++;
			}
			int endIndex = this.requestURI.indexOf("/", beginIndex);
			if (endIndex == -1) {
				return "";
			}
			return requestURI.substring(0, endIndex);
		}

		@Override
		public String getHeader(String name) {
			return headers.get(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return new Headers(headers.keySet());
		}

		@Override
		public String getContentType() {
			return headers.get("Content-Type");
		}

		@Override
		public int getContentLength() {
			return content != null ? content.length() : 0;
		}

		@Override
		public String getCharacterEncoding() {
			return "UTF-8";
		}

		@Override
		public String getRequestURI() {
			if (requestURI == null) {
				return null;
			}
			int index = requestURI.lastIndexOf('?');
			if (index == -1) {
				index = requestURI.length();
			}
			return requestURI.substring(0, index);
		}

		@Override
		public String getQueryString() {
			if (this.requestURI == null) {
				return null;
			}
			int beginIndex = this.requestURI.lastIndexOf('?');
			if (beginIndex == -1) {
				return null;
			}
			return this.requestURI.substring(beginIndex);
		}

		@Override
		public Locale getLocale() {
			return Locale.getDefault();
		}

		@Override
		public Cookie[] getCookies() {
			return null;
		}

		@Override
		public String getRemoteHost() {
			return "localhost";
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return new InputStream(new StringReader(this.content));
		}

		@Override
		public HttpSession getSession() {
			if (session == null) {
				session = new MockHttpSession();
			}
			return session;
		}

		@Override
		public Principal getUserPrincipal() {
			return null;
		}

		@Override
		public Object getAttribute(String name) {
			return null;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		int status;
		Map<String, String> headers = new HashMap<>();
		int contentLegth;
		String redirectLocation;
		StringWriter stringWriter = new StringWriter();

		@Override
		public void setStatus(int status) {
			this.status = status;
		}

		@Override
		public void setHeader(String header, String value) {
			headers.put(header, value);
		}

		@Override
		public void addHeader(String header, String value) {
			String headerValue = headers.get(header);
			if (headerValue == null) {
				headerValue = value;
			} else {
				headerValue = Strings.concat(headerValue, ", ", value);
			}
			headers.put(header, headerValue);
		}

		@Override
		public void setDateHeader(String header, long value) {
			headers.put(header, new Date(new Date().getTime() + value).toString());
		}

		@Override
		public void setContentType(String contentType) {
			headers.put("Content-Type", contentType);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return new OutputStream(stringWriter);
		}

		@Override
		public void sendError(int errorCode, String message) throws IOException {
			status = errorCode;
			stringWriter.write(message);
		}

		@Override
		public String getContentType() {
			return headers.get("Content-Type");
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			status = 307;
			redirectLocation = location;
		}

		String getBody() {
			return stringWriter.toString();
		}
	}

	private static class Headers implements Enumeration<String> {
		Iterator<String> headerNames;

		public Headers(Collection<String> headerNames) {
			this.headerNames = headerNames.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return headerNames.hasNext();
		}

		@Override
		public String nextElement() {
			return headerNames.next();
		}
	}

	private static class InputStream extends ServletInputStream {
		StringReader stringReader;

		public InputStream(StringReader stringReader) {
			this.stringReader = stringReader;
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
		StringWriter stringWriter;

		public OutputStream(StringWriter stringWriter) {
			this.stringWriter = stringWriter;
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
	}

	private static class MockHttpSession extends HttpSessionStub {
		Map<String, Object> attributes = new HashMap<>();

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}
	}
}
