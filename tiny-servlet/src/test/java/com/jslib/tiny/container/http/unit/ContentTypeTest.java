package com.jslib.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.jslib.lang.SyntaxException;
import com.jslib.tiny.container.http.ContentType;
import com.jslib.util.Classes;

public class ContentTypeTest {
	@Test
	public void constructor() {
		ContentType contentType = new ContentType("text/html; charset=UTF-8");
		assertTrue(contentType.isHTML());
		assertTrue(contentType.isMIME("text/html"));

		assertEquals("text", contentType.getType());
		assertEquals("html", contentType.getSubtype());
		assertEquals("text/html", contentType.getMIME());
		assertEquals("text/html;charset=UTF-8", contentType.getValue());
		assertEquals("text/html;charset=UTF-8", contentType.toString());

		assertTrue(contentType.hasParameter("charset"));
		assertTrue(contentType.hasParameter("charset", "UTF-8"));
		assertEquals("UTF-8", contentType.getParameter("charset"));

		assertTrue(contentType.hashCode() != 0);
	}

	@Test
	public void constructor_TextCharset() {
		ContentType contentType = new ContentType("text/html");
		assertTrue(contentType.isHTML());
		assertTrue(contentType.isMIME("text/html"));

		assertEquals("text", contentType.getType());
		assertEquals("html", contentType.getSubtype());
		assertEquals("text/html", contentType.getMIME());
		assertEquals("text/html", contentType.getValue());
		assertEquals("text/html", contentType.toString());

		assertFalse(contentType.hasParameter("charset"));
		assertFalse(contentType.hasParameter("charset", "UTF-8"));
		assertNull(contentType.getParameter("charset"));

		assertTrue(contentType.hashCode() != 0);
	}

	@Test
	public void constructor_NoParameters() {
		ContentType contentType = new ContentType("application/json");
		assertTrue(contentType.isJSON());
		assertTrue(contentType.isMIME("application/json"));

		assertEquals("application", contentType.getType());
		assertEquals("json", contentType.getSubtype());
		assertEquals("application/json", contentType.getMIME());
		assertEquals("application/json", contentType.getValue());
		assertEquals("application/json", contentType.toString());

		assertFalse(contentType.hasParameter("charset"));
		assertFalse(contentType.hasParameter("charset", "UTF-8"));
		assertNull(contentType.getParameter("charset"));

		assertTrue(contentType.hashCode() != 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructor_NullValue() {
		new ContentType(null);
	}

	@Test(expected = SyntaxException.class)
	public void constructor_BadSyntax() {
		new ContentType("fake-content-type");
	}

	@Test
	public void fromFile() {
		assertEquals(ContentType.TEXT_HTML, ContentType.forFile(new File("index.html")));
		assertEquals(ContentType.APPLICATION_JSON, ContentType.forFile(new File("index.json")));
		assertEquals(ContentType.TEXT_HTML, ContentType.forFile(new File("index.fake.extension")));
	}

	@Test
	public void valueOf() {
		assertEquals(ContentType.TEXT_HTML, ContentType.valueOf("text/html"));
		assertEquals("type/subtype", ContentType.valueOf("type/subtype").getValue());
		assertEquals(ContentType.APPLICATION_JSON, ContentType.valueOf(null));
	}

	@Test
	public void predicates() {
		assertTrue(ContentType.TEXT_HTML.isMIME("text/html"));
		assertFalse(ContentType.TEXT_HTML.isMIME("text/xml"));

		assertTrue(ContentType.TEXT_HTML.isHTML());
		assertTrue(ContentType.TEXT_XML.isXML());
		assertTrue(ContentType.APPLICATION_JSON.isJSON());
		assertTrue(ContentType.MULTIPART_FORM.isMultipartForm());
		assertTrue(ContentType.MULTIPART_MIXED.isMultipartMixed());
		assertTrue(ContentType.URLENCODED_FORM.isUrlEncodedForm());
		assertTrue(ContentType.APPLICATION_STREAM.isByteStream());
	}

	@Test
	public void equals() {
		assertTrue(ContentType.TEXT_HTML.equals(ContentType.TEXT_HTML));
		assertFalse(ContentType.TEXT_HTML.equals(ContentType.TEXT_XML));
		assertFalse(ContentType.TEXT_XML.equals(new ContentType("application/xml")));
		assertFalse(ContentType.TEXT_HTML.equals(ContentType.APPLICATION_JSON));
		assertFalse(ContentType.TEXT_HTML.equals(new Object()));
		assertFalse(ContentType.TEXT_HTML.equals(null));
	}

	@Test
	public void parseParameters() throws Exception {
		for (String expression : new String[] { "charset=UTF-8", "charset =UTF-8", "charset= UTF-8", " charset = UTF-8 " }) {
			Map<String, String> parameters = parseParameters(expression);
			assertNotNull(parameters);
			assertEquals(1, parameters.size());
			assertEquals("UTF-8", parameters.get("charset"));
		}
	}

	@Test(expected = SyntaxException.class)
	public void parseParameters_EmptyExpression() throws Exception {
		parseParameters("");
	}

	@Test(expected = SyntaxException.class)
	public void parseParameters_EmptyName() throws Exception {
		parseParameters("charset=");
	}

	@Test(expected = SyntaxException.class)
	public void parseParameters_NameOverride() throws Exception {
		parseParameters("charset=UTF-8;charset=ASCII");
	}

	private static Map<String, String> parseParameters(String expression) throws Exception {
		return Classes.invoke(ContentType.class, "parseParameters", expression);
	}
}
