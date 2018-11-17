package js.mvc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import js.container.ContainerSPI;
import js.core.Factory;
import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.w3c.DocumentBuilderImpl;
import js.format.StandardDateTime;
import js.io.WriterOutputStream;
import js.json.Json;
import js.mvc.View;
import js.mvc.ViewManager;
import js.servlet.RequestContext;
import js.unit.HttpServletResponseStub;
import js.unit.TestContext;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

@SuppressWarnings("unused")
public class ViewUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	private MockHttpServletResponse httpResponse;

	@Before
	public void beforeTest() throws Exception {
		httpResponse = new MockHttpServletResponse();
	}

	@Test
	public void ssiView_EscapeWriter() throws IOException {
		StringReader reader = new StringReader("alpha\"'&<>omega");
		StringWriter writer = new StringWriter();
		Writer escapeWriter = Classes.newInstance("js.mvc.SsiView$EscapeWriter", writer);
		Files.copy(reader, escapeWriter);
		assertEquals("alpha&quot;&apos;&amp;&lt;&gt;omega", writer.toString());
	}

	@Test
	public void ssiView_EscapeWriter_JSON() throws IOException {
		Person person = new Person();
		Json json = Classes.loadService(Json.class);
		StringWriter writer = new StringWriter();
		Writer escapeWriter = Classes.newInstance("js.mvc.SsiView$EscapeWriter", writer);
		json.stringify(escapeWriter, person);
		assertEquals(Strings.load(new File("fixture/mvc/ssi-model")), writer.toString());
	}

	@Test
	public void ssiView() throws Exception {
		String config = "" + //
				"<config>" + //
				"	<views>" + //
				"		<repository path='fixture/mvc' files-pattern='*.html' class='js.mvc.SsiView' />" + //
				"	</views>" + //
				"</config>";
		setupTestContext(config);

		Person sourcePerson = new Person();
		ViewManager manager = Factory.getInstance(ViewManager.class);
		View view = manager.getView("page").setModel(sourcePerson);
		view.serialize(httpResponse);
		assertEquals("no-cache;no-store", httpResponse.getHeader("Cache-Control"));

		httpResponse.buffer.close();

		Document doc = httpResponse.getDocument();
		// doc.dump();
		String ssiContentText = doc.getById("js.SSI-CONTENT").getText();

		Json json = Factory.getInstance(Json.class);
		Person targetPerson = json.parse(new StringReader(ssiContentText), Person.class);
		assertEquals(sourcePerson.id, targetPerson.id);
		assertEquals(sourcePerson.name, targetPerson.name);
		assertEquals(sourcePerson.surname, targetPerson.surname);
		assertEquals(sourcePerson.emailAddr, targetPerson.emailAddr);
		assertTheSame(sourcePerson.birthday, targetPerson.birthday);
		assertEquals(sourcePerson.state, targetPerson.state);
	}

	/** Exception from HTTP response output stream should bubble up. */
	@Test(expected = IOException.class)
	public void ssiView_Exception() throws Exception {
		httpResponse.stream = new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				throw new IOException();
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
			}
		};

		String config = "" + //
				"<config>" + //
				"	<views>" + //
				"		<repository path='fixture/mvc' files-pattern='*.html' class='js.mvc.SsiView' />" + //
				"	</views>" + //
				"</config>";
		setupTestContext(config);

		ViewManager manager = Factory.getInstance(ViewManager.class);
		View view = manager.getView("page").setModel(new Person());
		view.serialize(httpResponse);
	}

	/** Generate XSP view without template operators, default settings. Test model is injected but not operators. */
	@Test
	public void xspView() throws Exception {
		String config = "" + //
				"<config>" + //
				"	<views>" + //
				"		<repository path='fixture/mvc' files-pattern='*.html' />" + //
				"	</views>" + //
				"</config>";
		setupTestContext(config);

		Person person = new Person(true);
		ViewManager manager = Factory.getInstance(ViewManager.class);
		View view = manager.getView("page").setModel(person);
		view.serialize(httpResponse);
		assertEquals("no-cache;no-store", httpResponse.getHeader("Cache-Control"));

		Document doc = httpResponse.getDocument();
		EList spans = doc.findByTag("span");
		assertEquals(2, spans.size());
		assertEquals(person.name, spans.item(0).getText());
		assertEquals(person.surname, spans.item(1).getText());

		EList dds = doc.findByTag("dd");
		assertEquals(3, dds.size());
		assertEquals(person.landline, dds.item(0).getText());
		assertEquals(person.mobile, dds.item(1).getText());
		assertEquals("1964-03-15 13:40:00", dds.item(2).getText());

		assertTrue(doc.findByAttr("data-text").isEmpty());
		assertTrue(doc.findByAttr("data-format").isEmpty());
	}

	/** Serialize XSP view with not injected model. Test elements injected values are empty. */
	@Test
	public void xspView_NullModel() throws Exception {
		String config = "" + //
				"<config>" + //
				"	<views>" + //
				"		<repository path='fixture/mvc' files-pattern='*.html' />" + //
				"	</views>" + //
				"</config>";
		setupTestContext(config);

		ViewManager manager = Factory.getInstance(ViewManager.class);
		View view = manager.getView("page");
		view.serialize(httpResponse);
		assertEquals("no-cache;no-store", httpResponse.getHeader("Cache-Control"));

		Document doc = httpResponse.getDocument();
		EList spans = doc.findByTag("span");
		assertEquals(2, spans.size());
		assertEquals("", spans.item(0).getText());
		assertEquals("", spans.item(1).getText());

		EList dds = doc.findByTag("dd");
		assertEquals(3, dds.size());
		assertEquals("", dds.item(0).getText());
		assertEquals("", dds.item(1).getText());
		assertEquals("", dds.item(2).getText());
	}

	/** Generate XSP view with template operators and test if present into HTTP response. */
	@Test
	public void xspView_OperatorSerialization() throws Exception {
		String config = "" + //
				"<config>" + //
				"	<views>" + //
				"		<repository path='fixture/mvc' files-pattern='*.html'>" + //
				"			<property name='template.operator.serialization' value='true' />" + //
				"		</repository>" + //
				"	</views>" + //
				"</config>";
		setupTestContext(config);

		Person person = new Person(true);
		ViewManager manager = Factory.getInstance(ViewManager.class);
		View view = manager.getView("page").setModel(person);
		view.serialize(httpResponse);
		assertEquals("no-cache;no-store", httpResponse.getHeader("Cache-Control"));

		Document doc = httpResponse.getDocument();
		EList spans = doc.findByTag("span");
		assertEquals(2, spans.size());
		assertEquals("John", spans.item(0).getText());
		assertEquals("Doe", spans.item(1).getText());

		EList dds = doc.findByTag("dd");
		assertEquals(3, dds.size());
		assertEquals("0232555666", dds.item(0).getText());
		assertEquals("0721555666", dds.item(1).getText());
		assertEquals("1964-03-15 13:40:00", dds.item(2).getText());

		assertFalse(doc.findByAttr("data-text").isEmpty());
		assertFalse(doc.findByAttr("data-format").isEmpty());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static void assertTheSame(Date expected, Date concrete) {
		if (expected == null) {
			assertNull(concrete);
			return;
		}
		Calendar expectedCalendar = Calendar.getInstance();
		expectedCalendar.setTime(expected);
		expectedCalendar.set(Calendar.MILLISECOND, 0);
		Calendar concreteCalendar = Calendar.getInstance();
		concreteCalendar.setTime(expected);
		concreteCalendar.set(Calendar.MILLISECOND, 0);
		assertEquals(expectedCalendar.getTime().getTime(), concreteCalendar.getTime().getTime());
	}

	private static void setupTestContext(String config) throws Exception {
		ContainerSPI container = (ContainerSPI) TestContext.start(config);
		RequestContext context = container.getInstance(RequestContext.class);
		Classes.setFieldValue(context, "attached", true);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static enum State {
		NONE, ACTIVE
	}

	private static class Person {
		int id;
		String name;
		String surname;
		URL webPage;
		String landline;
		String mobile;
		String emailAddr;
		Date birthday;
		State state = State.NONE;

		public Person() {
		}

		public Person(boolean initialize) throws MalformedURLException, ParseException {
			if (!initialize) {
				return;
			}
			id = 1964;
			name = "John";
			surname = "Doe";
			webPage = new URL("http://site.com/");
			landline = "0232555666";
			mobile = "0721555666";
			emailAddr = "john.doe@email.com";
			birthday = (Date) new StandardDateTime().parse("1964-03-15 13:40:00");
			state = State.ACTIVE;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		StringWriter buffer = new StringWriter();
		PrintWriter writer = new PrintWriter(this.buffer);
		ServletOutputStream stream = new MockOutputStream(buffer);
		Map<String, String> headers = new HashMap<String, String>();
		String contentType;

		@Override
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public void setHeader(String header, String value) {
			if (headers.containsKey(header)) {
				throw new IllegalStateException("Set existing header");
			}
			headers.put(header, value);
		}

		@Override
		public void setDateHeader(String header, long value) {
			Date date = new Date(value);
			setHeader(header, df.format(date));
		}

		@Override
		public void addHeader(String header, String value) {
			String existingValue = headers.get(header);
			if (existingValue == null) {
				throw new IllegalStateException("Add to an not existing header.");
			}
			headers.put(header, existingValue + ";" + value);
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return writer;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return stream;
		}

		public String getHeader(String header) {
			return headers.get(header);
		}

		public Document getDocument() {
			DocumentBuilder builder = new DocumentBuilderImpl();
			return builder.loadHTML(new InputSource(new StringReader(buffer.toString())));
		}
	}

	private static class MockOutputStream extends ServletOutputStream {
		OutputStream stream;

		public MockOutputStream(StringWriter writer) {
			stream = new WriterOutputStream(writer);
		}

		@Override
		public void write(int b) throws IOException {
			stream.write(b);
		}

		@Override
		public void flush() throws IOException {
			stream.flush();
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
