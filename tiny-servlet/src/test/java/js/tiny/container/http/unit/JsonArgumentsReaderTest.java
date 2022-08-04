package js.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import js.json.Json;
import js.lang.GType;
import js.tiny.container.http.encoder.JsonArgumentsReader;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class JsonArgumentsReaderTest {
	private String content;
	private ServletInputStream stream;

	@Mock
	private HttpServletRequest request;

	@Mock
	private Json json;
	
	private JsonArgumentsReader argumentsReader;

	@Before
	public void beforeTest() throws IOException {
		// in order for created mock to call read(byte[],int,int) from superclass need to set CALLS_REAL_METHODS
		stream = mock(ServletInputStream.class, Mockito.CALLS_REAL_METHODS);

		when(stream.read()).thenAnswer(new Answer<Integer>() {
			private InputStream stream;

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				if(stream == null) {
					stream = new ByteArrayInputStream(content.getBytes());
				}
				return stream.read();
			}
		});

		when(request.getInputStream()).thenReturn(stream);
		
		argumentsReader = new JsonArgumentsReader();
	}

	@Test
	public void read_Object() throws Exception {
		content = "[{\"name\":\"John Doe\"}]";

		Object[] arguments = exercise(new Type[] { Person.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertNotNull(arguments[0]);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
	}

	@Test
	public void read_Object_NoArgumentsArray() throws Exception {
		content = "{\"name\":\"John Doe\"}";

		Object[] arguments = exercise(new Type[] { Person.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
	}

	@Test
	public void read_TwoObjects() throws Exception {
		content = "[{\"name\":\"John Doe\"},{\"name\":\"Jane Doe\"}]";

		Object[] arguments = exercise(new Type[] { Person.class, Person.class });

		assertNotNull(arguments);
		assertEquals(2, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
		assertTrue(arguments[1] instanceof Person);
		assertEquals("Jane Doe", ((Person) arguments[1]).name);
	}

	@Test
	public void read_TwoObjects_NoArgumentsArray() throws Exception {
		content = "{\"name\":\"John Doe\"},{\"name\":\"Jane Doe\"}";

		Object[] arguments = exercise(new Type[] { Person.class, Person.class });

		assertNotNull(arguments);
		assertEquals(2, arguments.length);
		assertTrue(arguments[0] instanceof Person);
		assertEquals("John Doe", ((Person) arguments[0]).name);
		assertTrue(arguments[1] instanceof Person);
		assertEquals("Jane Doe", ((Person) arguments[1]).name);
	}

	@Test
	public void read_ObjectWithObject() throws Exception {
		content = "[{\"godFather\":{\"name\":\"John Doe\"}}]";

		Object[] arguments = exercise(new Type[] { CosaNostra.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof CosaNostra);
		assertEquals("John Doe", ((CosaNostra) arguments[0]).godFather.name);
	}

	@Test
	public void read_ObjectWithObject_NoArgumentsArray() throws Exception {
		content = "{\"godFather\":{\"name\":\"John Doe\"}}";

		Object[] arguments = exercise(new Type[] { CosaNostra.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof CosaNostra);
		assertEquals("John Doe", ((CosaNostra) arguments[0]).godFather.name);
	}

	@Test
	public void read_ObjectWithArray() throws Exception {
		content = "[{\"members\":[{\"name\":\"John Doe\"}]}]";

		Object[] arguments = exercise(new Type[] { Family.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof Family);
		assertEquals("John Doe", ((Family) arguments[0]).members[0].name);
	}

	@Test
	public void read_ObjectWithArray_NoArgumentsArray() throws Exception {
		content = "{\"members\":[{\"name\":\"John Doe\"}]}";

		Object[] arguments = exercise(new Type[] { Family.class });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertNotNull(arguments[0]);
		assertTrue(arguments[0] instanceof Family);
		assertEquals("John Doe", ((Family) arguments[0]).members[0].name);
	}

	@Test
	public void read_List() throws Exception {
		content = "[[{\"name\":\"John Doe\"}]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person> persons = (List<Person>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0).name);
	}

	@Test
	public void read_List_NoArgumentsArray() throws IOException {
		content = "[{\"name\":\"John Doe\"}]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person.class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person> persons = (List<Person>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0).name);
	}

	@Test
	public void read_ListOfArray() throws Exception {
		content = "[[[{\"name\":\"John Doe\"}]]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person[].class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person[]> persons = (List<Person[]>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0)[0].name);
	}

	@Test
	public void read_ListOfArray_NoArgumentsArray() throws Exception {
		content = "[[{\"name\":\"John Doe\"}]]";

		Object[] arguments = exercise(new Type[] { new GType(List.class, Person[].class) });

		assertNotNull(arguments);
		assertEquals(1, arguments.length);
		assertTrue(arguments[0] instanceof List);

		List<Person[]> persons = (List<Person[]>) arguments[0];
		assertEquals(1, persons.size());
		assertEquals("John Doe", persons.get(0)[0].name);
	}

	@Test
	public void read_ListOfStrings() throws Exception {
		content = "[[\"John Doe\",\"Jane Doe\"]]";

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
	public void read_ListOfStrings_NoArgumentsArray() throws Exception {
		content = "[\"John Doe\",\"Jane Doe\"]";

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
	public void read_MapOfObjects() throws Exception {
		content = "[{\"first\":{\"name\":\"John Doe\"},\"second\":{\"name\":\"Jane Doe\"}}]";

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
	public void read_MapOfObjects_NoArgumentsArray() throws Exception {
		content = "{\"first\":{\"name\":\"John Doe\"},\"second\":{\"name\":\"Jane Doe\"}}";

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
		return argumentsReader.read(request, formalParameters);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

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
