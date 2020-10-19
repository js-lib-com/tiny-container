package js.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import js.lang.BugError;
import js.lang.GType;
import js.lang.InvocationException;
import js.lang.SyntaxException;
import js.util.Classes;

import org.junit.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class QueryParametersTest {
	private static final String QUERY_PARAMETERS = "fexp=907921&itag=43&ip=0.0.0.0&sver=3&ratebypass=yes&expire=1311728400&key=yt1&ipbits=0&view=b%26w";

	@Test
	public void constructor_QueryString() throws Exception {
		Object queryParameters = getQueryParametersParser(QUERY_PARAMETERS);
		List parameters = Classes.getFieldValue(queryParameters, "parameters");
		assertParameters(parameters);
	}

	@Test
	public void constructor_NullQueryString() throws Exception {
		Class clazz = getQueryParametersParserClass();
		Constructor constructor = clazz.getDeclaredConstructor(String.class);
		constructor.setAccessible(true);
		Object queryParameters = constructor.newInstance((String) null);

		List parameters = Classes.getFieldValue(queryParameters, "parameters");
		assertTrue(parameters.isEmpty());
	}

	@Test
	public void constructor_QueryStringTrailingQuestionMark() throws Exception {
		Object queryParameters = getQueryParametersParser('?' + QUERY_PARAMETERS);
		List parameters = Classes.getFieldValue(queryParameters, "parameters");
		assertParameters(parameters);
	}

	@Test
	public void constructor_FormStream() throws Exception {
		Object queryParameters = getQueryParametersParser(new ByteArrayInputStream(QUERY_PARAMETERS.getBytes()));
		List parameters = Classes.getFieldValue(queryParameters, "parameters");
		assertParameters(parameters);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emprtyReaderClose() throws IOException {
		Class clazz = getQueryParametersParserClass();
		Reader EMPTY_READER = Classes.getFieldValue(clazz, "EMPTY_READER");
		EMPTY_READER.close();
	}

	@Test
	public void asObject() throws Throwable {
		assertEquals((Byte) (byte) 64, asObject("64", Byte.class));
		assertEquals((Short) (short) 1964, asObject("1964", Short.class));
		assertEquals(65536, (int)asObject("65536", Integer.class));
		assertEquals(65536L, (long)asObject("65536", Long.class));
		assertEquals(65536F, (float)asObject("65536", Float.class), 0);
		assertEquals(65536.0, (double)asObject("65536", Double.class), 0);
		assertTrue((boolean) asObject("true", Boolean.class));
		assertEquals("string", asObject("string", String.class));
		assertEquals(new File("path"), asObject("path", File.class));
	}

	@Test
	public void asObject_NullValue() throws Throwable {
		assertEquals((byte) 0, (byte)asObject(null, Byte.class));
		assertEquals((short) 0, (short)asObject(null, Short.class));
		assertEquals(0, (int)asObject(null, Integer.class));
		assertEquals(0L, (long)asObject(null, Long.class));
		assertEquals(0.0F, (float)asObject(null, Float.class), 0);
		assertEquals(0.0, (double)asObject(null, Double.class), 0);
		assertFalse((boolean) asObject(null, Boolean.class));
		assertEquals("", asObject(null, String.class));
		assertNull(asObject(null, File.class));
	}

	/** Convert string with comma separated values into string collection and vice versa. */
	@Test
	public void asObject_arrays() throws Throwable {
		String bytes = "1, 2 ,3";
		Collection<Byte> byteCollection = asObject(bytes, new GType(Collection.class, Byte.class));
		assertTrue(byteCollection.contains((byte) 1));
		assertTrue(byteCollection.contains((byte) 3));

		List<Byte> byteList = asObject(bytes, new GType(List.class, Byte.class));
		assertEquals((byte) 1, (byte) byteList.get(0));
		assertEquals((byte) 3, (byte) byteList.get(2));

		String doubles = "1.1, 2.2 ,3.3";
		Collection<Double> doubleCollection = asObject(doubles, new GType(Collection.class, Double.class));
		assertTrue(doubleCollection.contains(1.1));
		assertTrue(doubleCollection.contains(3.3));

		List<Double> doubleList = asObject(doubles, new GType(List.class, Double.class));
		assertEquals(1.1, doubleList.get(0), 0.0);
		assertEquals(3.3, doubleList.get(2), 0.0);
	}

	@Test
	public void asObject_StringsArray() throws Throwable {
		String[] strings = asObject("one,two,three", String[].class);
		assertNotNull(strings);
		assertEquals(3, strings.length);
		assertEquals("one", strings[0]);
		assertEquals("two", strings[1]);
		assertEquals("three", strings[2]);
	}

	@Test
	public void asObject_NullArrayItems() throws Throwable {
		String bytes = "1, null ,3";
		List<Byte> byteList = asObject(bytes, new GType(List.class, Byte.class));
		assertEquals((byte) 1, (byte) byteList.get(0));
		assertNull(byteList.get(1));
		assertEquals((byte) 3, (byte) byteList.get(2));
	}

	@Test
	public void asObject_NoConverter() throws Throwable {
		assertNull(asObject("1", Person.class));
	}

	@Test(expected = BugError.class)
	public void asObject_ParameterizedType() throws Throwable {
		asObject("1", new GType(Person.class));
	}

	@Test(expected = BugError.class)
	public void asObject_ObjectsArray() throws Throwable {
		asObject("1,2,3", Person[].class);
	}

	/** Not parameterized collections use String. */
	@Test
	public void asObject_RawList() throws Throwable {
		List list = asObject("1,2,3", List.class);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEquals("1", list.get(0));
		assertEquals("2", list.get(1));
		assertEquals("3", list.get(2));
	}

	/** Test query parameters parser. Check order is preserved and special chars properly decoded. */
	@Test
	public void parser() throws Exception {
		Reader reader = new StringReader(QUERY_PARAMETERS);
		List parameters = Classes.invoke(getQueryParametersParserClass(), "parse", reader);
		assertParameters(parameters);
	}

	@Test
	public void parser_RawValue() throws Exception {
		Reader reader = new StringReader("this%20is%20a%20raw%20value");
		List p = Classes.invoke(getQueryParametersParserClass(), "parse", reader);
		assertEquals(1, p.size());
		assertParameter("this is a raw value", p, 0);
	}

	@Test
	public void parser_BadQueryString() throws Exception {
		for (String queryString : new String[] { "param1&param2=value2", "param1=value1&param2", "param1=value1&param2&param3=value3", "=valu1", "param1=value1&=value2", "param1=value1&&", "param1=val=ue1" }) {
			try {
				Classes.invoke(getQueryParametersParserClass(), "parse", new StringReader(queryString));
				fail("Invalid query string should rise exception.");
			} catch (SyntaxException e) {
				assertTrue(e.getMessage().startsWith("Invalid query string"));
			}
		}
	}

	/**
	 * Test query parameters logic. This test focus on arguments logic but also perform a shallow conversion test. Thoroughly
	 * converter test is carry out by converter unit tests. See js.web.QueryParametersParser#getArguments(Type[]) for arguments
	 * logic description.
	 */
	@Test
	public void getArguments() throws Throwable {
		Object queryParameters = getQueryParametersParser(QUERY_PARAMETERS);

		Type[] types = new Type[] { long.class, byte.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(9, arguments.length);
		assertEquals(907921L, arguments[0]);
		assertEquals((byte) 43, arguments[1]);
		assertEquals("0.0.0.0", arguments[2]);
		assertEquals("3", arguments[3]);
		assertEquals("yes", arguments[4]);
		assertEquals("1311728400", arguments[5]);
		assertEquals("yt1", arguments[6]);
		assertEquals("0", arguments[7]);
		assertEquals("b&w", arguments[8]);
	}

	@Test
	public void getArguments_IntegersList() throws Throwable {
		Object queryParameters = getQueryParametersParser("1, 2 ,3");

		Type[] types = new Type[] { new GType(List.class, Integer.class) };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(1, arguments.length);
		List<Integer> list = (List<Integer>) arguments[0];
		assertEquals(1, (int) list.get(0));
		assertEquals(2, (int) list.get(1));
		assertEquals(3, (int) list.get(2));
	}

	/** Raw list is processed as list of strings. */
	@Test
	public void getArguments_RawList() throws Throwable {
		Object queryParameters = getQueryParametersParser("1, 2 ,3");

		Type[] types = new Type[] { List.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(1, arguments.length);
		List<String> list = (List<String>) arguments[0];
		assertEquals("1", list.get(0));
		assertEquals("2", list.get(1));
		assertEquals("3", list.get(2));
	}

	@Test
	public void getArguments_RawValue() throws Throwable {
		Object queryParameters = getQueryParametersParser("this%20is%20a%20raw%20value");
		Type[] types = new Type[] { String.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(1, arguments.length);
		assertEquals("this is a raw value", arguments[0]);
	}

	@Test
	public void getArguments_EmptyValue() throws Exception {
		Object queryParameters = getQueryParametersParser("name1=value1&name2=&name3=value3");

		Type[] types = new Type[] { String.class, String.class, String.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(3, arguments.length);
		assertEquals("value1", arguments[0]);
		assertEquals("", arguments[1]);
		assertEquals("value3", arguments[2]);
	}

	@Test
	public void getArguments_NoFormalParameters() throws Exception {
		Object queryParameters = getQueryParametersParser("name1=value1&name2=&name3=value3");

		Type[] types = new Type[] {};
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(0, arguments.length);
	}

	@Test
	public void getArguments_ObjectFields() throws Exception {
		Object queryParameters = getQueryParametersParser("name=John Doe&age=54&active=true");

		Type[] types = new Type[] { Person.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(1, arguments.length);
		assertEquals(Person.class, arguments[0].getClass());

		Person person = (Person) arguments[0];
		assertEquals("John Doe", person.name);
		assertEquals(54, person.age);
		assertTrue(person.active);
	}

	@Test
	public void getArguments_MoreFormalParameters() throws Exception {
		Object queryParameters = getQueryParametersParser("name1=value1&name2=value2");

		Type[] types = new Type[] { String.class, String.class, String.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(3, arguments.length);
		assertEquals("value1", arguments[0]);
		assertEquals("value2", arguments[1]);
		assertNull(arguments[2]);
	}

	@Test
	public void getArguments_LessFormalParameters() throws Exception {
		Object queryParameters = getQueryParametersParser("name1=value1&name2=value2&name3=value3");

		Type[] types = new Type[] { String.class, String.class };
		Object[] arguments = Classes.invoke(queryParameters, "getArguments", (Object) types);

		assertEquals(2, arguments.length);
		assertEquals("value1", arguments[0]);
		assertEquals("value2", arguments[1]);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static <T> T asObject(String string, Type type) throws Throwable {
		try {
			return Classes.invoke(getQueryParametersParserClass(), "asObject", string, type);
		} catch (InvocationException e) {
			throw e.getCause();
		}
	}

	private static Object getQueryParametersParser(String queryString) {
		return Classes.newInstance("js.tiny.container.http.encoder.QueryParametersParser", queryString);
	}

	private static Object getQueryParametersParser(InputStream inputStream) {
		return Classes.newInstance("js.tiny.container.http.encoder.QueryParametersParser", inputStream);
	}

	private static Class getQueryParametersParserClass() {
		return Classes.forName("js.tiny.container.http.encoder.QueryParametersParser");
	}

	private static void assertParameters(List p) throws Exception {
		assertEquals(9, p.size());
		assertParameter("907921", p, 0);
		assertParameter("43", p, 1);
		assertParameter("0.0.0.0", p, 2);
		assertParameter("3", p, 3);
		assertParameter("yes", p, 4);
		assertParameter("1311728400", p, 5);
		assertParameter("yt1", p, 6);
		assertParameter("0", p, 7);
		assertParameter("b&w", p, 8);
	}

	private static void assertParameter(String expected, List parameters, int index) throws Exception {
		assertEquals(expected, Classes.invoke(parameters.get(index), "getValue"));
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Person {
		private String name;
		private int age;
		private boolean active;
	}
}
