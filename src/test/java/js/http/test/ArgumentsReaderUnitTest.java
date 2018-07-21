package js.http.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import js.http.encoder.ArgumentsReader;
import js.http.encoder.ArgumentsReaderFactory;
import js.http.encoder.ServerEncoders;
import js.lang.GType;
import js.unit.HttpServletRequestStub;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class ArgumentsReaderUnitTest {
	private MockHttpServletRequest request;
	private ArgumentsReaderFactory argumentsReaderFactory;

	@Before
	public void beforeTest() {
		request = new MockHttpServletRequest();
		argumentsReaderFactory = ServerEncoders.getInstance();
	}

	@Test
	public void readJsonArguments_Object() throws Exception {
		request.contentType = "application/json";
		request.body = "[{\"name\":\"John Doe\"}]";

		Object[] arguments = exercise(new Type[] { Person.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
	}

	@Test
	public void readJsonArguments_Object_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "{\"name\":\"John Doe\"}";

		Object[] arguments = exercise(new Type[] { Person.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
	}

	@Test
	public void readJsonArguments_TwoObjects() throws Exception {
		request.contentType = "application/json";
		request.body = "[{\"name\":\"John Doe\"},{\"name\":\"Jane Doe\"}]";

		Object[] arguments = exercise(new Type[] { Person.class, Person.class });

		assertNotNull(arguments);
		assertEquals(2, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
		assertTrue(arguments[1] instanceof Person);
		assertEquals("Jane Doe", ((Person) arguments[1]).name);
	}

	@Test
	public void readJsonArguments_TwoObjects_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "{\"name\":\"John Doe\"},{\"name\":\"Jane Doe\"}";

		Object[] arguments = exercise(new Type[] { Person.class, Person.class });

		assertNotNull(arguments);
		assertEquals(2, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
		assertTrue(arguments[1] instanceof Person);
		assertEquals("Jane Doe", ((Person) arguments[1]).name);
	}

	@Test
	public void readJsonArguments_ObjectWithObject() throws Exception {
		request.contentType = "application/json";
		request.body = "[{\"godFather\":{\"name\":\"John Doe\"}}]";

		Object[] arguments = exercise(new Type[] { CosaNostra.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof CosaNostra);
		assertEquals("John Doe", ((CosaNostra) arguments[0]).godFather.name);
	}

	@Test
	public void readJsonArguments_ObjectWithObject_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "{\"godFather\":{\"name\":\"John Doe\"}}";

		Object[] arguments = exercise(new Type[] { CosaNostra.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof CosaNostra);
		assertEquals("John Doe", ((CosaNostra) arguments[0]).godFather.name);
	}

	@Test
	public void readJsonArguments_ObjectWithArray() throws Exception {
		request.contentType = "application/json";
		request.body = "[{\"members\":[{\"name\":\"John Doe\"}]}]";

		Object[] arguments = exercise(new Type[] { Family.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Family);
		assertEquals("John Doe", ((Family) arguments[0]).members[0].name);
	}

	@Test
	public void readJsonArguments_ObjectWithArray_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "{\"members\":[{\"name\":\"John Doe\"}]}";

		Object[] arguments = exercise(new Type[] { Family.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Family);
		assertEquals("John Doe", ((Family) arguments[0]).members[0].name);
	}

	@Test
	public void readJsonArguments_List() throws Exception {
		request.contentType = "application/json";
		request.body = "[[{\"name\":\"John Doe\"}]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person> persons = (List<Person>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0).name);
	}

	@Test
	public void readJsonArguments_List_NoArgumentsArray() throws IOException {
		request.contentType = "application/json";
		request.body = "[{\"name\":\"John Doe\"}]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person> persons = (List<Person>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0).name);
	}

	@Test
	public void readJsonArguments_ListOfArray() throws Exception {
		request.contentType = "application/json";
		request.body = "[[[{\"name\":\"John Doe\"}]]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person[].class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person[]> persons = (List<Person[]>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0)[0].name);
	}

	@Test
	public void readJsonArguments_ListOfArray_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "[[{\"name\":\"John Doe\"}]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person[].class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person[]> persons = (List<Person[]>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0)[0].name);
	}

	@Test
	public void readJsonArguments_ListOfStrings() throws Exception {
		request.contentType = "application/json";
		request.body = "[[\"John Doe\",\"Jane Doe\"]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, String.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<String> persons = (List<String>) arguments[0];
		assertEquals(2, persons.size());
		assertEquals("John Doe", persons.get(0));
		assertEquals("Jane Doe", persons.get(1));
	}

	@Test
	public void readJsonArguments_ListOfStrings_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "[\"John Doe\",\"Jane Doe\"]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, String.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<String> persons = (List<String>) arguments[0];
		assertEquals(2, persons.size());
		assertEquals("John Doe", persons.get(0));
		assertEquals("Jane Doe", persons.get(1));
	}

	@Test
	public void readJsonArguments_MapOfObjects() throws Exception {
		request.contentType = "application/json";
		request.body = "[{\"first\":{\"name\":\"John Doe\"},\"second\":{\"name\":\"Jane Doe\"}}]";

		Object[] arguments = exercise(new Type[] { new GType(Map.class, String.class, Person.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Map);

		Map<String, Person> persons = (Map<String, Person>) arguments[0];
		assertEquals(2, persons.size());
		assertEquals("John Doe", persons.get("first").name);
		assertEquals("Jane Doe", persons.get("second").name);
	}

	@Test
	public void readJsonArguments_MapOfObjects_NoArgumentsArray() throws Exception {
		request.contentType = "application/json";
		request.body = "{\"first\":{\"name\":\"John Doe\"},\"second\":{\"name\":\"Jane Doe\"}}";

		Object[] arguments = exercise(new Type[] { new GType(Map.class, String.class, Person.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Map);

		Map<String, Person> persons = (Map<String, Person>) arguments[0];
		assertEquals(2, persons.size());
		assertEquals("John Doe", persons.get("first").name);
		assertEquals("Jane Doe", persons.get("second").name);
	}

	private Object[] exercise(Type[] formalParameters) throws IOException {
		ArgumentsReader argumentsReader = argumentsReaderFactory.getArgumentsReader(request, formalParameters);
		return argumentsReader.read(request, formalParameters);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private String contentType;
		private String queryString;
		private String body;

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public String getQueryString() {
			return queryString;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return new ServletInputStream() {
				private InputStream stream = new ByteArrayInputStream(body.getBytes());

				@Override
				public int read() throws IOException {
					return stream.read();
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
			};
		}
	}

	private static class Person {
		private String name;
	}

	private static class Family {
		private Person[] members;
	}

	private static class CosaNostra {
		private Person godFather;
	}
}
