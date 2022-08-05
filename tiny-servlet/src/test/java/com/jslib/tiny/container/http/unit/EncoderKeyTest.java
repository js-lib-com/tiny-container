package com.jslib.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.jslib.lang.GType;
import com.jslib.tiny.container.http.ContentType;
import com.jslib.tiny.container.http.encoder.EncoderKey;

public class EncoderKeyTest {
	@Test
	public void constructor() {
		EncoderKey key = new EncoderKey(ContentType.valueOf("text/html"), Object.class);
		assertEquals("text/html:java.lang.Object", key.toString());
	}

	@Test
	public void constructor_ParameterizedType() {
		EncoderKey key = new EncoderKey(ContentType.valueOf("text/html"), new GType(List.class, Object.class));
		assertEquals("text/html:java.util.List", key.toString());
	}

	@Test
	public void constructor_ContentType() {
		EncoderKey key = new EncoderKey(ContentType.valueOf("text/html"));
		assertEquals("text/html", key.toString());
	}

	@Test
	public void constructor_JavaType() {
		EncoderKey key = new EncoderKey(Object.class);
		assertEquals("java.lang.Object", key.toString());
	}

	@Test
	public void constructor_NullParameters() {
		EncoderKey key = new EncoderKey(null, null);
		assertEquals("", key.toString());
	}
	
	@Test
	public void valueHashCode() {
		assertEquals("text/html".hashCode(), new EncoderKey(ContentType.valueOf("text/html")).hashCode());
	}

	@Test
	public void valueEquals() {
		assertTrue(EncoderKey.APPLICATION_JSON.equals(EncoderKey.APPLICATION_JSON));
		assertTrue(EncoderKey.APPLICATION_JSON.equals(new EncoderKey(ContentType.valueOf("application/json"))));
		assertFalse(EncoderKey.APPLICATION_JSON.equals(null));
		assertFalse(EncoderKey.APPLICATION_JSON.equals(Object.class));
	}
}
