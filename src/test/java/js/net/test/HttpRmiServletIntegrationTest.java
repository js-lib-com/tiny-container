package js.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import js.annotation.Private;
import js.annotation.Public;
import js.annotation.Remote;
import js.container.ContainerSPI;
import js.core.App;
import js.core.AppContext;
import js.core.Factory;
import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.w3c.DocumentBuilderImpl;
import js.http.form.Form;
import js.http.form.FormField;
import js.http.form.FormIterator;
import js.http.form.Part;
import js.http.form.UploadStream;
import js.http.form.UploadedFile;
import js.io.StreamHandler;
import js.io.WriterOutputStream;
import js.json.Json;
import js.net.HttpRmiServlet;
import js.rmi.RemoteException;
import js.servlet.AppServlet;
import js.servlet.RequestContext;
import js.servlet.TinyContainer;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.unit.HttpSessionStub;
import js.unit.ServletContextStub;
import js.unit.TestContext;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

@SuppressWarnings({ "unchecked", "unused", "rawtypes" })
public class HttpRmiServletIntegrationTest {
	private ContainerSPI container;
	private MockApp app;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private RequestContext context;
	private Json json;

	@Before
	public void beforeTest() throws Exception {
		container = (ContainerSPI) TestContext.start(DESCRIPTION);
		app = container.getInstance(App.class);
		json = container.getInstance(Json.class);
		request = new MockHttpServletRequest();
		request.servletContext = new MockServletContext();
		response = new MockHttpServletResponse();
		context = Factory.getInstance(RequestContext.class);
	}

	/** Decode parameters sent as URL query string. */
	@Test
	public void queryParameters() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveData.rmi?number=1.23&flag=true&string=this is a string&date=1964-03-15T13:40:00Z&file=/var/log/message");
		assertEquals("1.23:true:this is a string:Sun Mar 15 15:40:00 EET 1964:\\var\\log\\message", app.content);
	}

	/** Decode form encoded application/x-www-form-urlencoded. */
	@Test
	public void urlEncodedForm() throws Exception {
		request.headers.put("Content-Type", "application/x-www-form-urlencoded");
		request.setContent("number=1.23&flag=true&string=this is a string&date=1964-03-15T13:40:00Z&file=/var/log/message");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveData.rmi");
		assertEquals("1.23:true:this is a string:Sun Mar 15 15:40:00 EET 1964:\\var\\log\\message", app.content);
	}

	/** Decode object parameter encode JSON. */
	@Test
	public void objectParameter() throws Exception {
		request.headers.put("Content-Type", "application/json");
		request.setContent("[{\"name\":\"John Doe\",\"profession\":\"freelancer\",\"age\":48}]");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveObject.rmi");
		assertParameter(response, app.content);
	}

	/**
	 * When managed method has a single formal parameter is legal to send a single JSON object without explicit arguments array.
	 */
	@Test
	public void objectParameter_AsObject() throws Exception {
		request.headers.put("Content-Type", "application/json");
		request.setContent("{\"name\":\"John Doe\",\"profession\":\"freelancer\",\"age\":48}");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveObject.rmi");
		assertParameter(response, app.content);
	}

	/**
	 * Send JSON arguments as object instead of explicit arguments array but for method with multiple arguments. It should be
	 * rejected with illegal argument.
	 */
	@Test
	public void objectParameter_AsObject_NotSingleArgument() throws Exception {
		request.headers.put("Content-Type", "application/json");
		request.setContent("{\"name\":\"John Doe\",\"profession\":\"freelancer\",\"age\":48}");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveNoStream.rmi");

		assertEquals(500, response.status);
		assertEquals(207, response.contentLength);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"JSON parser error on char index #8 near ...[{\\\"name\\\":. Invalid value helper |class js.json.impl.PrimitiveValue| for target type |class java.io.File|.\"}", response.body());
	}

	/** Decode object parameter encode JSON. */
	@Test
	public void objectParameter_InvalidJson() throws Exception {
		request.headers.put("Content-Type", "application/json");
		request.setContent("[{\"name:\"John Doe\"}]");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveObject.rmi");

		assertEquals(500, response.status);
		assertEquals(161, response.contentLength);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"JSON parser error on char index #14 near ...[{\\\"name:\\\"John D. Invalid primitive value with white space.\"}", response.body());
	}

	/** This framework default request content type is JSON not octet stream as requested by W3C. */
	@Test
	public void objectParameterWithoutContentType() throws Exception {
		request.setContent("[{\"name\":\"John Doe\",\"profession\":\"freelancer\",\"age\":48}]");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveObject.rmi");
		assertParameter(response, app.content);
	}

	@Test
	public void getNullObject() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getNullPerson.rmi");
		assertEquals("null", response.body());
	}

	@Test
	public void testVoid() throws Exception {
		request.content = "";
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/voidMethod.rmi");
		assertEquals(0, response.body().length());
	}

	@Test
	public void documentParameter() throws Exception {
		request.headers.put("Content-Type", "text/xml");
		request.setContent("" + //
				"<?xml version=\"1.0\"?>\r\n" + //
				"<person>\r\n" + //
				"   <name>John Doe</name>\r\n" + //
				"   <profession>freelancer</profession>\r\n" + //
				"   <age>48</age>\r\n" + //
				"</person>");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveDocument.rmi");
		assertParameter(response, app.content);
	}

	@Test
	public void documentParameter_BadParametersCount() throws Exception {
		request.headers.put("Content-Type", "text/xml");
		request.setContent("" + //
				"<?xml version=\"1.0\"?>\r\n" + //
				"<person>\r\n" + //
				"   <name>John Doe</name>\r\n" + //
				"   <profession>freelancer</profession>\r\n" + //
				"   <age>48</age>\r\n" + //
				"</person>");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveNoStream.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Bad parameters count. Should be exactly one but is |2|.\"}", response.body());
	}

	@Test
	public void documentParameter_ParameteretizedType() throws Exception {
		request.headers.put("Content-Type", "text/xml");
		request.setContent("" + //
				"<?xml version=\"1.0\"?>\r\n" + //
				"<person>\r\n" + //
				"   <name>John Doe</name>\r\n" + //
				"   <profession>freelancer</profession>\r\n" + //
				"   <age>48</age>\r\n" + //
				"</person>");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveList.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Parameterized type |java.util.List<java.lang.String>| is not supported.\"}", response.body());
	}

	@Test
	public void formParameter() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
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
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveForm.rmi");
		assertParameter(response, app.content);
	}

	@Test
	public void formParameter_Empty() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveForm.rmi");

		assertEquals(204, response.status);
		assertEquals(0, response.contentLength);
		assertEquals("", response.body());
		Person p = (Person) app.content;
		assertNull(p.name);
		assertNull(p.profession);
		assertEquals(0, p.age);
	}

	@Test
	public void formParameter_InputStream() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadStream.rmi");
		assertEquals(204, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void formParameter_InputStream_BadContent() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"John Doe\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadStream.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Illegal form. Expected uploaded stream but got field |name|.\"}", response.body());
	}

	@Test
	public void formParameter_InputStream_Empty() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadStream.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Empty form.\"}", response.body());
	}

	@Test
	public void formParameter_UploadedFile() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadFormFile.rmi");
		assertEquals(204, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void formParameter_UploadStream() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadFormStream.rmi");
		assertEquals(204, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void formParameter_BadParametersCount() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
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
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveNoStream.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Bad parameters count. Should be exactly one but is |2|.\"}", response.body());
	}

	@Test
	public void formParameter_ParameterizedType() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
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
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveList.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Parameterized type |java.util.List<java.lang.String>| is not supported.\"}", response.body());
	}

	@Test
	public void multipartFormParameter() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
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
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveMultipartForm.rmi");
		assertParameter(response, app.content);
	}

	@Test
	public void formObjectParameter() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
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
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveFormObject.rmi");
		assertParameter(response, app.content);
	}

	private static void assertParameter(MockHttpServletResponse response, Object person) {
		assertEquals(204, response.status);
		assertEquals(0, response.contentLength);
		assertEquals("", response.body());
		Person p = (Person) person;
		assertEquals("John Doe", p.name);
		assertEquals("freelancer", p.profession);
		assertEquals(48, p.age);
	}

	@Test
	public void uploadForm() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadForm.rmi");
		assertEquals(204, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void uploadMultipartForm() throws Exception {
		request.headers.put("Content-Type", "multipart/form-data; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"test-file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"some random plain text content\r\ncar return is part of content\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadMultipartForm.rmi");
		assertEquals(204, response.status);
		assertEquals("some random plain text content\r\ncar return is part of content", app.content);
	}

	@Test
	public void uploadStream() throws Exception {
		request.headers.put("Content-Type", "application/octet-stream");
		request.setContent("stream");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadStream.rmi");
		assertEquals(204, response.status);
		assertEquals("stream", app.content);
	}

	@Test
	public void uploadNamedStream() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"Content-Type: application/json; charset=UTF-8\r\n" + //
				"\r\n" + //
				"1\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"inputStream\"\r\n" + //
				"Content-Type: application/octet-stream\r\n" + //
				"\r\n" + //
				"stream\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadNamedStream.rmi");
		assertEquals(204, response.status);
		assertEquals("1stream", app.content);
	}

	@Test
	public void uploadStreamWithMultipleParameters() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"Content-Type: application/json; charset=UTF-8\r\n" + //
				"\r\n" + //
				"\"john\"\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"date\"\r\n" + //
				"Content-Type: application/json; charset=UTF-8\r\n" + //
				"\r\n" + //
				"1964-03-15T13:40:00Z\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"average\"\r\n" + //
				"Content-Type: application/json; charset=UTF-8\r\n" + //
				"\r\n" + //
				"1.23\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"flag\"\r\n" + //
				"Content-Type: application/json; charset=UTF-8\r\n" + //
				"\r\n" + //
				"true\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"inputStream\"\r\n" + //
				"Content-Type: application/octet-stream\r\n" + //
				"\r\n" + //
				"stream\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadStreamWithMultipleParameters.rmi");
		assertEquals(204, response.status);
		assertEquals("john:Sun Mar 15 15:40:00 EET 1964:1.23:true:stream", app.content);
	}

	@Test
	public void mixedData() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"0\"\r\n" + //
				"Content-Type: application/json\r\n" + //
				"\r\n" + //
				"\"/var/log/message\"\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"1\"\r\n" + //
				"Content-Type: text/xml; charset=UTF-8\r\n" + //
				"\r\n" + //
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<root></root>\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"2\"\r\n" + //
				"Content-Type: application/octet-stream\r\n" + //
				"\r\n" + //
				"stream\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveMixedData.rmi");
		assertEquals(204, response.status);
		assertEquals("\\var\\log\\message:<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<root></root>:stream", app.content);
	}

	@Test
	public void mixedData_NoStream() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"0\"\r\n" + //
				"Content-Type: application/json\r\n" + //
				"\r\n" + //
				"\"/var/log/message\"\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"1\"\r\n" + //
				"Content-Type: text/xml; charset=UTF-8\r\n" + //
				"\r\n" + //
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<root></root>\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveNoStream.rmi");
		assertEquals(204, response.status);
		assertEquals("\\var\\log\\message:<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<root></root>", app.content);
	}

	@Test
	public void mixedData_BadStreamPosition() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"0\"\r\n" + //
				"Content-Type: application/octet-stream\r\n" + //
				"\r\n" + //
				"stream\r\n" + //
				"Content-Disposition: form-data; name=\"1\"\r\n" + //
				"Content-Type: application/json\r\n" + //
				"\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveBadStream.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Not all parameters processed due to stream argument that is not the last on arguments list.\"}", response.body());
	}

	@Test
	public void mixedData_IOException() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"0\"\r\n" + //
				"Content-Type: application/json\r\n" + //
				"\r\n" + //
				"\"/var/log/message\"\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveMixedDataException.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"java.io.IOException\",\"message\":\"exception\"}", response.body());
	}

	@Test
	public void mixedData_EmptyJson() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"0\"\r\n" + //
				"Content-Type: application/json\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"1\"\r\n" + //
				"Content-Type: text/xml; charset=UTF-8\r\n" + //
				"\r\n" + //
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<root></root>\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveNoStream.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"JSON parser error on char index #-1 near .... Closed reader. No data available for parsing.\"}", response.body());
	}

	@Test
	public void mixedData_JsonException() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"0\"\r\n" + //
				"Content-Type: application/json\r\n" + //
				"\r\n" + //
				"\"/var/log/message\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveMixedDataException.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"JSON parser error on char index #16 near ...\\\"/var/log/message. Cannot retrieve required character because of premature stream end.\"}", response.body());
	}

	@Test
	public void mixedData_XmlStream() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"1\"\r\n" + //
				"Content-Type: text/xml; charset=UTF-8\r\n" + //
				"\r\n" + //
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<root></root>\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/uploadStream.rmi");
		assertEquals(204, response.status);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<root></root>", app.content);
	}

	@Test
	public void mixedData_BadXmlFormalType() throws Exception {
		request.headers.put("Content-Type", "multipart/mixed; boundary=XXX");
		request.setContent("" + //
				"--XXX\r\n" + //
				"Content-Disposition: form-data; name=\"1\"\r\n" + //
				"Content-Type: text/xml; charset=UTF-8\r\n" + //
				"\r\n" + //
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<root></root>\r\n" + //
				"--XXX--");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/saveObject.rmi");
		assertEquals(500, response.status);
		assertEquals("{\"cause\":\"js.lang.IllegalArgumentException\",\"message\":\"Unsupported formal parameter type |class js.net.test.HttpRmiServletIntegrationTest$Person| for XML content type.\"}", response.body());
	}

	@Test
	public void downloadStream() throws Exception {
		request.content = "";
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/downloadStream.rmi", "application/octet-stream");
		assertEquals("application/octet-stream", response.getContentType());
		assertEquals("output stream", response.body());
	}

	@Test
	public void returnDocument() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getDocument.rmi");
		assertEquals(200, response.status);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<person>   <name>John Doe</name>   <profession>freelancer</profession>   <age>48</age></person>", response.body());
	}

	@Test
	public void returnObject() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getObject.rmi");
		assertEquals(200, response.status);
		String response = this.response.body();
		assertTrue(response.contains("\"age\":48"));
		assertTrue(response.contains("\"profession\":\"freelancer\""));
		assertTrue(response.contains("\"name\":\"John Doe\""));
	}

	@Test
	public void returnString() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getString.rmi");
		assertEquals(200, response.status);
		assertEquals("\"John Doe\"", response.body());
	}

	@Test
	public void returnStringWithDiacritics() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getStringWithDiacritics.rmi");
		assertEquals(200, response.status);
		assertEquals("\"Iași\"", response.body());
	}

	@Test
	public void returnInteger() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getInteger.rmi");
		assertEquals(200, response.status);
		assertEquals("48", response.body());
	}

	@Test
	public void returnBoolean() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getBoolean.rmi");
		assertEquals(200, response.status);
		assertEquals("true", response.body());
	}

	@Test
	public void returnDate() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getDate.rmi");
		assertEquals(200, response.status);
		assertEquals("\"1964-03-15T11:40:00Z\"", response.body());
	}

	@Test
	public void returnObjectsList() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getObjectsList.rmi");
		assertEquals(200, response.status);
		String response = this.response.body();
		assertTrue(response.startsWith("[{"));
		assertTrue(response.endsWith("}]"));
		assertTrue(response.contains("\"age\":48"));
		assertTrue(response.contains("\"profession\":\"freelancer\""));
		assertTrue(response.contains("\"name\":\"John Doe\""));
	}

	@Test
	public void returnStringsList() throws Exception {
		request.headers.put("Content-Type", "application/json");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getStringsList.rmi");
		assertEquals(200, response.status);
		assertEquals("[\"John Doe\"]", response.body());
	}

	@Test
	public void unauthenticatedAccess() throws Throwable {
		// set login page field directly does not benefit from conversion to absolute context path
		Classes.setFieldValue(container, TinyContainer.class, "loginPage", "/login-page.html");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getPrivateResource.rmi");
		assertEquals(401, response.status);
		assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic realm="));
	}

	/** For XHR request to private resource on app without login page servlet responds with unauthorized access (401). */
	@Test
	public void xhrUnauthenticatedAccessWithoutLogin() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getPrivateResource.rmi", "XMLHttpRequest");
		assertEquals(401, response.status);
		assertNull(response.headers.get("X-JSLIB-Location"));
	}

	/** For XHR request to private resource on app with login page servlet responds with OK and custom X-JSLIB-Location. */
	@Test
	public void xhrUnauthenticatedAccess() throws Exception {
		// set login page field directly does not benefit from conversion to absolute context path
		Classes.setFieldValue(container, TinyContainer.class, "loginPage", "/login-page.html");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getPrivateResource.rmi", "XMLHttpRequest");
		assertEquals(200, response.status);
		assertEquals("/login-page.html", response.headers.get("X-JSLIB-Location"));
	}

	@Test
	public void xhrPrivateAuthenticatedAccess() throws Exception {
		request.session = new MockHttpSession();
		request.session.setAttribute(TinyContainer.ATTR_PRINCIPAL, new Principal() {
			@Override
			public String getName() {
				return "johndoe";
			}
		});

		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getPrivateResource.rmi", "XMLHttpRequest");
		assertEquals(204, response.status);
		assertNull(response.headers.get("X-JSLIB-Location"));
		request.session.setAttribute(TinyContainer.ATTR_PRINCIPAL, null);
	}

	/** For Android request to private resource on app without login page servlet responds with unauthorized access (401). */
	@Test
	public void androidUnauthenticatedAccessWithoutLogin() throws Throwable {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getPrivateResource.rmi", "j(s)-lib android");
		assertEquals(401, response.status);
		assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic realm="));
	}

	/** For Android request to private resource on app with login page servlet responds with unauthorized access (401). */
	@Test
	public void androidUnauthenticatedAccess() throws Throwable {
		// set login page field directly does not benefit from conversion to absolute context path
		Classes.setFieldValue(container, TinyContainer.class, "loginPage", "/login-page.html");
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getPrivateResource.rmi", "j(s)-lib android");
		assertEquals(401, response.status);
		assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic realm="));
	}

	@Test
	public void uncheckedException() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/generateUncheckedException.rmi");
		assertEquals(500, response.status);
		assertEquals("application/json", response.headers.get("Content-Type"));
		RemoteException e = json.parse(new StringReader(response.body()), RemoteException.class);
		assertEquals(RuntimeException.class.getCanonicalName(), e.getCause());
		assertEquals("Unchecked exception.", e.getMessage());
	}

	@Test
	public void checkedException() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/generateCheckedException.rmi");
		assertEquals(500, response.status);
		assertEquals("application/json", response.headers.get("Content-Type"));
		RemoteException e = json.parse(new StringReader(response.body()), RemoteException.class);
		assertEquals(Exception.class.getCanonicalName(), e.getCause());
		assertEquals("Checked exception.", e.getMessage());
	}

	@Test
	public void missingClass() throws Exception {
		execise("/js/test/rmi/FakeController/getPerson.rmi");
		assertEquals(500, response.status);
		assertEquals("application/json", response.headers.get("Content-Type"));
		RemoteException e = json.parse(new StringReader(response.body()), RemoteException.class);
		assertEquals(ClassNotFoundException.class.getCanonicalName(), e.getCause());
		assertEquals("/tests/js/test/rmi/FakeController/getPerson.rmi", e.getMessage());
	}

	@Test
	public void missingMethod() throws Exception {
		execise("/js/net/test/HttpRmiServletIntegrationTest/Controller/getFakeMethod.rmi");
		assertEquals(500, response.status);
		assertEquals("application/json", response.headers.get("Content-Type"));
		RemoteException e = json.parse(new StringReader(response.body()), RemoteException.class);
		assertEquals(NoSuchMethodException.class.getCanonicalName(), e.getCause());
		assertEquals("/tests/js/net/test/HttpRmiServletIntegrationTest/Controller/getFakeMethod.rmi", e.getMessage());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private void execise(String requestPath, String... headers) throws Exception {
		HttpRmiServlet servlet = new HttpRmiServlet();
		Classes.setFieldValue(servlet, AppServlet.class, "container", container);

		request.method = "POST";
		request.requestURI = "/tests" + requestPath;
		context.setRequestPath(requestPath);

		for (String header : headers) {
			// exercise method uses 2 headers: 'Accept' in with case header value is something like 'type/subtype' or
			// 'X-Requested-With' if header value pattern is not like 'Accept'
			if (header.contains("/")) {
				request.headers.put("Accept", header);
			} else {
				request.headers.put("X-Requested-With", header);
			}
		}

		context.attach(request, response);
		servlet.service(request, response);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static final String DESCRIPTION = "" + //
			"<?xml version='1.0' encoding='UTF-8'?>" + //
			"<config>" + //
			"    <managed-classes>" + //
			"        <app interface='js.core.App' class='js.net.test.HttpRmiServletIntegrationTest$MockApp' />" + //
			"        <controller interface='js.net.test.HttpRmiServletIntegrationTest$Controller' class='js.net.test.HttpRmiServletIntegrationTest$ControllerImpl' type='PROXY' scope='APPLICATION' />" + //
			"    </managed-classes>" + //
			"</config>";

	private static class MockApp extends App {
		public Object content;

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
	@Public
	private static interface Controller {
		void saveData(double number, boolean flag, String string, Date date, File file);

		void saveObject(Person person);

		void saveDocument(Document doc);

		void saveInputSource(InputSource inputSource);

		void saveMixedData(File file, Document document, InputStream inputStream) throws IOException;

		void saveMixedDataException(File file) throws IOException;

		void saveNoStream(File file, Document document) throws IOException;

		void saveBadStream(InputStream inputStream, File file) throws IOException;

		void saveForm(Form form);

		void saveMultipartForm(FormIterator form);

		void saveFormObject(Person person);

		void saveList(List<String> strings);

		void uploadForm(Form form) throws FileNotFoundException, IOException;

		void uploadFormFile(UploadedFile uploadedFile) throws IOException;

		void uploadFormStream(UploadStream uploadStream) throws IOException;

		void uploadMultipartForm(FormIterator form) throws IOException;

		void uploadStream(InputStream inputStream) throws IOException;

		void uploadNamedStream(String name, InputStream inputStream) throws IOException;

		void uploadStreamWithMultipleParameters(String name, Date date, double average, boolean flag, InputStream inputStream) throws IOException;

		StreamHandler<OutputStream> downloadStream();

		Document getDocument();

		Person getObject();

		String getString();

		String getStringWithDiacritics();

		int getInteger();

		boolean getBoolean();

		Date getDate() throws ParseException;

		List<Person> getObjectsList();

		List<String> getStringsList();

		@Private
		void getPrivateResource();

		void generateUncheckedException();

		void generateCheckedException() throws Exception;

		Person getNullPerson();

		void voidMethod();
	}

	public class ControllerImpl implements Controller {
		@Override
		public void saveData(double number, boolean flag, String string, Date date, File file) {
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = Strings.toString(number, flag, string, date, file);
		}

		@Override
		public void saveObject(Person person) {
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = person;
		}

		@Override
		public void saveDocument(Document doc) {
			Person p = new Person();
			p.name = doc.getByTag("name").getText();
			p.profession = doc.getByTag("profession").getText();
			p.age = Integer.parseInt(doc.getByTag("age").getText());
			saveObject(p);
		}

		@Override
		public void saveInputSource(InputSource inputSource) {
			DocumentBuilder builder = new DocumentBuilderImpl();
			saveDocument(builder.loadXML(inputSource));
		}

		@Override
		public void saveMixedData(File file, Document document, InputStream inputStream) throws IOException {
			StringWriter documentWriter = new StringWriter();
			document.serialize(documentWriter);

			StringWriter streamWriter = new StringWriter();
			Files.copy(new InputStreamReader(inputStream, "UTF-8"), streamWriter);

			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = Strings.toString(file, documentWriter, streamWriter);
		}

		@Override
		public void saveMixedDataException(File file) throws IOException {
			throw new IOException("exception");
		}

		@Override
		public void saveNoStream(File file, Document document) throws IOException {
			StringWriter documentWriter = new StringWriter();
			document.serialize(documentWriter);

			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = Strings.toString(file, documentWriter);
		}

		@Override
		public void saveBadStream(InputStream inputStream, File file) throws IOException {
		}

		@Override
		public void saveForm(Form form) {
			Person p = new Person();
			p.name = form.getValue("name");
			p.profession = form.getValue("profession");
			p.age = form.getValue("age", int.class);
			saveObject(p);
		}

		@Override
		public void saveMultipartForm(FormIterator form) {
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
			saveObject(p);
		}

		@Override
		public void saveFormObject(Person person) {
			saveObject(person);
		}

		@Override
		public void saveList(List<String> strings) {
		}

		@Override
		public void uploadForm(Form form) throws FileNotFoundException, IOException {
			UploadedFile upload = form.getUploadedFile("file");
			if (!upload.getContentType().equals("text/plain"))
				return;
			if (!upload.getFileName().equals("test-file.txt"))
				return;
			StringWriter writer = new StringWriter();
			Files.copy(new FileReader(upload.getFile()), writer);
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = writer.toString();
		}

		@Override
		public void uploadFormFile(UploadedFile uploadedFile) throws IOException {
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = Strings.load(uploadedFile.getFile());
		}

		@Override
		public void uploadFormStream(UploadStream uploadStream) throws IOException {
			StringWriter writer = new StringWriter();
			Files.copy(new InputStreamReader(uploadStream.openStream(), "UTF-8"), writer);
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = writer.toString();
		}

		@Override
		public void uploadMultipartForm(FormIterator form) throws IOException {
			for (Part item : form) {
				if (item instanceof UploadStream) {
					UploadStream upload = (UploadStream) item;
					if (!upload.getContentType().equals("text/plain"))
						return;
					if (!upload.getFileName().equals("test-file.txt"))
						return;
					StringWriter writer = new StringWriter();
					Files.copy(new InputStreamReader(upload.openStream(), "UTF-8"), writer);
					MockApp app = (MockApp) Factory.getInstance(App.class);
					app.content = writer.toString();
				}
			}
		}

		@Override
		public void uploadStream(InputStream inputStream) throws IOException {
			StringWriter writer = new StringWriter();
			Files.copy(new InputStreamReader(inputStream, "UTF-8"), writer);
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = writer.toString();
		}

		@Override
		public void uploadNamedStream(String name, InputStream inputStream) throws IOException {
			StringWriter writer = new StringWriter();
			Files.copy(new InputStreamReader(inputStream, "UTF-8"), writer);
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = name + writer.toString();
		}

		@Override
		public void uploadStreamWithMultipleParameters(String name, Date date, double average, boolean flag, InputStream inputStream) throws IOException {
			StringWriter writer = new StringWriter();
			Files.copy(new InputStreamReader(inputStream, "UTF-8"), writer);
			MockApp app = (MockApp) Factory.getInstance(App.class);
			app.content = Strings.toString(name, date, average, flag, writer);
		}

		@Override
		public StreamHandler<OutputStream> downloadStream() {
			return new StreamHandler<OutputStream>(OutputStream.class) {
				@Override
				public void handle(OutputStream outputStream) throws IOException {
					outputStream.write("output stream".getBytes("UTF-8"));
				}
			};
		}

		@Override
		public Document getDocument() {
			String xml = "<?xml version=\"1.0\"?>" + //
					"<person>" + //
					"   <name>John Doe</name>" + //
					"   <profession>freelancer</profession>" + //
					"   <age>48</age>" + //
					"</person>";
			DocumentBuilder builder = new DocumentBuilderImpl();
			return builder.parseXML(xml);
		}

		@Override
		public Person getObject() {
			return new Person("John Doe", "freelancer", 48);
		}

		@Override
		public String getString() {
			return "John Doe";
		}

		@Override
		public String getStringWithDiacritics() {
			return "Iași";
		}

		@Override
		public int getInteger() {
			return 48;
		}

		@Override
		public boolean getBoolean() {
			return true;
		}

		@Override
		public Date getDate() throws ParseException {
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			return df.parse("15-03-1964 13:40:00");
		}

		@Override
		public List<Person> getObjectsList() {
			List<Person> persons = new ArrayList<Person>();
			persons.add(new Person("John Doe", "freelancer", 48));
			return persons;
		}

		@Override
		public List<String> getStringsList() {
			List<String> strings = new ArrayList<String>();
			strings.add("John Doe");
			return strings;
		}

		@Override
		@Private
		public void getPrivateResource() {
		}

		@Override
		public void generateUncheckedException() {
			throw new RuntimeException("Unchecked exception.");
		}

		@Override
		public void generateCheckedException() throws Exception {
			throw new Exception("Checked exception.");
		}

		@Override
		public Person getNullPerson() {
			return null;
		}

		@Override
		public void voidMethod() {
		}
	}

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		HttpServletResponse httpResponse;
		ServletContext servletContext;
		HttpSession session;
		ServletInputStream servletInputStream;

		String method;
		String requestURI;
		Map<String, String> headers = new HashMap<>();
		String content;

		public void setContent(String content) {
			this.content = content;
			this.headers.put("Content-Length", Integer.toString(content.length()));
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
		public String getMethod() {
			return method;
		}

		@Override
		public String getRequestURI() {
			return requestURI;
		}

		@Override
		public String getContextPath() {
			return "/tests";
		}

		@Override
		public Enumeration getHeaderNames() {
			return new HeaderNames(headers.keySet().iterator());
		}

		@Override
		public String getHeader(String name) {
			return headers.get(name);
		}

		@Override
		public String getQueryString() {
			if (requestURI == null) {
				return null;
			}
			int beginIndex = requestURI.lastIndexOf('?');
			if (beginIndex == -1) {
				return null;
			}
			return requestURI.substring(beginIndex);
		}

		@Override
		public String getContentType() {
			return headers.get("Content-Type");
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (servletInputStream == null) {
				servletInputStream = new MockServletInputStream(new ByteArrayInputStream(content.getBytes("UTF-8")));
			}
			return servletInputStream;
		}

		@Override
		public String getCharacterEncoding() {
			return "UTF-8";
		}

		@Override
		public int getContentLength() {
			String contentLength = headers.get("Content-Length");
			return contentLength != null ? Integer.parseInt(contentLength) : 0;
		}

		@Override
		public Principal getUserPrincipal() {
			return null;
		}

		@Override
		public HttpSession getSession() {
			if (session == null) {
				session = new MockHttpSession();
			}
			return session;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		int status;
		Map<String, String> headers = new HashMap<>();
		StringWriter writer = new StringWriter();
		int contentLength;
		MockServletOutputStream servletOutputStream;
		String encoding;

		@Override
		public boolean isCommitted() {
			return servletOutputStream != null ? servletOutputStream.isCommited() : false;
		}

		@Override
		public void setStatus(int status) {
			this.status = status;
		}

		@Override
		public void setContentType(String contentType) {
			headers.put("Content-Type", contentType);
		}

		@Override
		public void setContentLength(int contentLegth) {
			this.contentLength = contentLegth;
		}

		@Override
		public void setHeader(String header, String value) {
			headers.put(header, value);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (servletOutputStream == null) {
				servletOutputStream = new MockServletOutputStream(writer);
			}
			return servletOutputStream;
		}

		@Override
		public String getContentType() {
			return headers.get("Content-Type");
		}

		@Override
		public void setCharacterEncoding(String encoding) {
			this.encoding = encoding;
		}

		@Override
		public String getHeader(String header) {
			return headers.get(header);
		}

		public String body() {
			return writer.toString();
		}
	}

	private static class MockServletContext extends ServletContextStub {
		Map<String, Object> attributes = new HashMap<>();

		@Override
		public String getRealPath(String resource) {
			return ".";
		}

		@Override
		public Object getAttribute(String name) {
			return attributes.get(name);
		}
	}

	private static class MockHttpSession extends HttpSessionStub {
		Map<String, Object> attributes = new HashMap<>();

		@Override
		public Object getAttribute(String name) {
			return this.attributes.get(name);
		}

		@Override
		public void setAttribute(String name, Object value) {
			this.attributes.put(name, value);
		}
	}

	private static class HeaderNames implements Enumeration<String> {
		private Iterator<String> iterator;

		public HeaderNames(Iterator<String> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		@Override
		public String nextElement() {
			return iterator.next();
		}
	}

	private static class MockServletOutputStream extends ServletOutputStream {
		private static final String HEADER = "" + //
				"HTTP/1.1 %d OK\r\n" + //
				"Content-Type: %s\r\n" + //
				"Content-Length: %s\r\n" + //
				"\r\n";

		private HttpServletResponse response;
		private OutputStream outputStream;
		private boolean commited;

		public MockServletOutputStream(HttpServletResponse response, OutputStream outputStream) {
			this.response = response;
			this.outputStream = outputStream;
		}

		public MockServletOutputStream(Writer writer) {
			this.outputStream = new WriterOutputStream(writer);
		}

		@Override
		public void write(int b) throws IOException {
			if (!commited) {
				writeHeader();
			}
			outputStream.write(b);
		}

		@Override
		public void flush() throws IOException {
			if (!commited) {
				writeHeader();
			}
			outputStream.flush();
			super.flush();
		}

		public boolean isCommited() {
			return commited;
		}

		private void writeHeader() throws IOException {
			if (response == null) {
				return;
			}

			String header = String.format(HEADER, response.getStatus(), response.getContentType(), response.getHeader("Content-Length"));
			for (byte h : header.getBytes()) {
				outputStream.write(h);
			}
			commited = true;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}
	}

	private static class MockServletInputStream extends ServletInputStream {
		int contentLegth;
		int index;
		InputStream inputStream;

		public MockServletInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		public void setContentLegth(int contentLegth) {
			this.contentLegth = contentLegth;
			index = 0;
		}

		@Override
		public int read() throws IOException {
			if (contentLegth != 0 && index++ >= contentLegth) {
				return -1;
			}
			return inputStream.read();
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
