package js.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import js.tiny.container.http.form.FormObject;
import js.tiny.container.http.form.UploadedFile;
import js.util.Classes;

public class FormObjectTest {
	@Test
	public void namedNodes() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("name", "SC Gnotis SRL");
		fields.put("taxID.attribute", "RO");
		fields.put("taxID.code", "15485745");
		fields.put("taxID.index", "45");
		fields.put("tradeRegister", "J22/945/04.06.2003");
		fields.put("address", "str. Cuza Voda nr.3, Corp B 700036, Iasi, Romania");
		fields.put("subsidiaries", "2");
		Partner p = run(fields, Partner.class);

		assertEquals("SC Gnotis SRL", p.name);
		assertEquals("RO", p.taxID.attribute);
		assertEquals("15485745", p.taxID.code);
		assertEquals(45, p.taxID.index);
		assertEquals("J22/945/04.06.2003", p.tradeRegister);
		assertEquals("str. Cuza Voda nr.3, Corp B 700036, Iasi, Romania", p.address);
		assertEquals(2, p.subsidiaries);
	}

	@Test
	public void arrayNode() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("bankAccounts.0", "RO27BREL0002000090990100");
		fields.put("bankAccounts.1", "RO76BRDE450SV05283104500");
		fields.put("bankAccounts.2", "RO06BPOS85002717789ROL01");
		Partner p = run(fields, Partner.class);

		assertEquals("RO27BREL0002000090990100", p.bankAccounts[0]);
		assertEquals("RO76BRDE450SV05283104500", p.bankAccounts[1]);
		assertEquals("RO06BPOS85002717789ROL01", p.bankAccounts[2]);
	}

	@Test
	public void arrayNode_FieldsOverflow() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("bankAccounts.0", "RO27BREL0002000090990100");
		fields.put("bankAccounts.1", "RO76BRDE450SV05283104500");
		fields.put("bankAccounts.2", "RO06BPOS85002717789ROL01");
		fields.put("bankAccounts.3", "RO06RFZB85002717789ROL01");
		Partner p = run(fields, Partner.class);

		assertEquals(3, p.bankAccounts.length);
		assertEquals("RO27BREL0002000090990100", p.bankAccounts[0]);
		assertEquals("RO76BRDE450SV05283104500", p.bankAccounts[1]);
		assertEquals("RO06BPOS85002717789ROL01", p.bankAccounts[2]);
	}

	@Test
	public void arrayOfStrings() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("strings.0", "John Doe");
		fields.put("strings.1", "Jane Doe");
		PrimitiveArray p = run(fields, PrimitiveArray.class);

		assertEquals(2, p.strings.length);
		assertEquals("John Doe", p.strings[0]);
		assertEquals("Jane Doe", p.strings[1]);
	}

	@Test
	public void arrayOfIntegers() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("integers.0", Integer.toString(1964));
		fields.put("integers.1", Integer.toString(2018));
		PrimitiveArray p = run(fields, PrimitiveArray.class);

		assertEquals(2, p.integers.length);
		assertEquals(1964, p.integers[0]);
		assertEquals(2018, p.integers[1]);
	}

	@Test
	public void listNode() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("contacts.0.name", "Blascu Lucian");
		fields.put("contacts.0.phone", "0744.123.456");
		fields.put("contacts.0.email", "luci@gnotis.ro");
		fields.put("contacts.1.name", "Gavriliu Mihaela");
		fields.put("contacts.1.phone", "0721.556.070");
		fields.put("contacts.1.email", "mica@gnotis.ro");
		Partner p = run(fields, Partner.class);

		assertEquals("Blascu Lucian", p.contacts.get(0).name);
		assertEquals("0744.123.456", p.contacts.get(0).phone);
		assertEquals("luci@gnotis.ro", p.contacts.get(0).email);
		assertEquals("Gavriliu Mihaela", p.contacts.get(1).name);
		assertEquals("0721.556.070", p.contacts.get(1).phone);
		assertEquals("mica@gnotis.ro", p.contacts.get(1).email);
	}

	@Test
	public void listOfStrings() throws Exception {
		Map<String, String> fields = new HashMap<>();
		for (int i = 0; i < 100; ++i) {
			fields.put("strings." + i, "string #" + i);
		}
		PrimitiveList p = run(fields, PrimitiveList.class);

		assertEquals(100, p.strings.size());
		for (int i = 0; i < p.strings.size(); ++i) {
			assertEquals("string #" + i, p.strings.get(i));
		}
	}

	@Test
	public void listOfIntegers() throws Exception {
		Map<String, String> fields = new HashMap<>();
		for (int i = 0; i < 100; ++i) {
			fields.put("integers." + i, Integer.toString(i));
		}
		PrimitiveList p = run(fields, PrimitiveList.class);

		assertEquals(100, p.integers.size());
		for (int i = 0; i < p.integers.size(); ++i) {
			assertEquals(i, (int) p.integers.get(i));
		}
	}

	@Test
	public void booleanValues() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("supplier", "false");
		fields.put("customer", "true");
		Partner p = run(fields, Partner.class);
		assertFalse(p.supplier);
		assertTrue(p.customer);
	}

	@Test
	public void dateValues() throws Exception {
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		Map<String, String> fields = new HashMap<>();
		fields.put("manufacturingDate", "2010-03-15T12:00:00Z");
		Car c = run(fields, Car.class);
		assertEquals("15-03-2010 12:00:00", df.format(c.manufacturingDate));
	}

	@Test
	public void superFields() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("manufacturer", "OPEL");
		fields.put("model", "Corsa 1.2");
		fields.put("manufacturingDate", "2010-03-15T12:00:00Z");
		fields.put("weighting", "22.5");
		fields.put("length", "17.8");

		DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		Truck t = run(fields, Truck.class);
		assertEquals(Manufacturer.OPEL, t.manufacturer);
		assertEquals("Corsa 1.2", t.model);
		assertEquals("15-03-2010 12:00:00", df.format(t.manufacturingDate));
		assertEquals(22.5, t.weighting, 0.0);
		assertEquals(17.8, t.length, 0.0);
	}

	@Test
	public void complexGraph() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("name", "SC Gnotis SRL");
		fields.put("taxID.attribute", "RO");
		fields.put("taxID.code", "15485745");
		fields.put("taxID.index", "45");
		fields.put("tradeRegister", "J22/945/04.06.2003");
		fields.put("bankAccounts.0", "RO27BREL0002000090990100");
		fields.put("bankAccounts.1", "RO76BRDE450SV05283104500");
		fields.put("bankAccounts.2", "RO06BPOS85002717789ROL01");
		fields.put("address", "str. Cuza Voda nr.3, Corp B 700036, Iasi, Romania");
		fields.put("contacts.0.name", "Blascu Lucian");
		fields.put("contacts.0.phone", "0744.123.456");
		fields.put("contacts.0.email", "luci@gnotis.ro");
		fields.put("contacts.1.name", "Gavriliu Mihaela");
		fields.put("contacts.1.phone", "0721.556.070");
		fields.put("contacts.1.email", "mica@gnotis.ro");
		fields.put("subsidiaries", "2");
		fields.put("supplier", "false");
		fields.put("customer", "true");
		Partner p = run(fields, Partner.class);

		assertEquals("SC Gnotis SRL", p.name);
		assertEquals("RO", p.taxID.attribute);
		assertEquals("15485745", p.taxID.code);
		assertEquals(45, p.taxID.index);
		assertEquals("J22/945/04.06.2003", p.tradeRegister);
		assertEquals("RO27BREL0002000090990100", p.bankAccounts[0]);
		assertEquals("RO76BRDE450SV05283104500", p.bankAccounts[1]);
		assertEquals("RO06BPOS85002717789ROL01", p.bankAccounts[2]);
		assertEquals("str. Cuza Voda nr.3, Corp B 700036, Iasi, Romania", p.address);
		assertEquals("Blascu Lucian", p.contacts.get(0).name);
		assertEquals("0744.123.456", p.contacts.get(0).phone);
		assertEquals("luci@gnotis.ro", p.contacts.get(0).email);
		assertEquals("Gavriliu Mihaela", p.contacts.get(1).name);
		assertEquals("0721.556.070", p.contacts.get(1).phone);
		assertEquals("mica@gnotis.ro", p.contacts.get(1).email);
		assertEquals(2, p.subsidiaries);
		assertFalse(p.supplier);
		assertTrue(p.customer);
	}

	@Test
	public void car() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("manufacturer", "OPEL");
		fields.put("model", "Corsa 1.2");
		fields.put("manufacturingDate", "2010-03-15T12:00:00Z");
		fields.put("wheels.0.tire.manufacturer", "Pirelli");
		fields.put("wheels.0.tire.expiration", "2011-06-30T12:00:00Z");
		fields.put("wheels.0.tire.price", "250");
		fields.put("wheels.0.pressure", "2.1");
		fields.put("wheels.0.diameter", "14");
		fields.put("wheels.1.tire.manufacturer", "Pirelli");
		fields.put("wheels.1.tire.expiration", "2011-06-30T12:00:00Z");
		fields.put("wheels.1.tire.price", "250");
		fields.put("wheels.1.pressure", "2.1");
		fields.put("wheels.1.diameter", "14");
		fields.put("wheels.2.tire.manufacturer", "Michelin");
		fields.put("wheels.2.tire.expiration", "2012-12-31T23:59:59Z");
		fields.put("wheels.2.tire.price", "310");
		fields.put("wheels.2.pressure", "1.8");
		fields.put("wheels.2.diameter", "15");
		fields.put("wheels.3.tire.manufacturer", "Michelin");
		fields.put("wheels.3.tire.expiration", "2012-12-31T23:59:59Z");
		fields.put("wheels.3.tire.price", "310");
		fields.put("wheels.3.pressure", "1.8");
		fields.put("wheels.3.diameter", "15");
		Car c = run(fields, Car.class);

		DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		assertEquals(Manufacturer.OPEL, c.manufacturer);
		assertEquals("Corsa 1.2", c.model);
		assertEquals("15-03-2010 12:00:00", df.format(c.manufacturingDate));

		assertEquals("Pirelli", c.wheels[0].tire.manufacturer);
		assertEquals("30-06-2011 12:00:00", df.format(c.wheels[0].tire.expiration));
		assertEquals(250D, c.wheels[0].tire.price, 0.0);
		assertEquals(2.1F, c.wheels[0].pressure, 0.0);
		assertEquals(14, c.wheels[0].diameter);

		assertEquals("Pirelli", c.wheels[1].tire.manufacturer);
		assertEquals("30-06-2011 12:00:00", df.format(c.wheels[1].tire.expiration));
		assertEquals(250D, c.wheels[1].tire.price, 0.0);
		assertEquals(2.1F, c.wheels[1].pressure, 2.1);
		assertEquals(14, c.wheels[1].diameter);

		assertEquals("Michelin", c.wheels[2].tire.manufacturer);
		assertEquals("31-12-2012 23:59:59", df.format(c.wheels[2].tire.expiration));
		assertEquals(310D, c.wheels[2].tire.price, 0.0);
		assertEquals(1.8F, c.wheels[2].pressure, 1.8);
		assertEquals(15, c.wheels[2].diameter);

		assertEquals("Michelin", c.wheels[3].tire.manufacturer);
		assertEquals("31-12-2012 23:59:59", df.format(c.wheels[3].tire.expiration));
		assertEquals(310D, c.wheels[3].tire.price, 0.0);
		assertEquals(1.8F, c.wheels[3].pressure, 0.0);
		assertEquals(15, c.wheels[3].diameter);
	}

	@Test
	public void listsOfList() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("parents.0.name", "Rotaru Iulian");
		fields.put("parents.0.children.0.name", "Rotaru Lucian");
		fields.put("parents.0.children.1.name", "Rotaru Deliana");
		fields.put("parents.0.children.2.name", "Rotaru Octavian");
		fields.put("parents.1.name", "Gogalniceanu Mioara");
		fields.put("parents.1.children.0.name", "Gogalniceanu Stefan");
		GeneTree geneTree = run(fields, GeneTree.class);

		assertEquals("Rotaru Iulian", geneTree.parents.get(0).name);
		assertEquals("Rotaru Lucian", geneTree.parents.get(0).children.get(0).name);
		assertEquals("Rotaru Deliana", geneTree.parents.get(0).children.get(1).name);
		assertEquals("Rotaru Octavian", geneTree.parents.get(0).children.get(2).name);
		assertEquals("Gogalniceanu Mioara", geneTree.parents.get(1).name);
		assertEquals("Gogalniceanu Stefan", geneTree.parents.get(1).children.get(0).name);
	}

	@Test
	public void multipartForm() throws Exception {
		final String FORM = "" + //
				"--AaB03x\r\n" + //
				"Content-Disposition: form-data; name=\"name\"\r\n" + //
				"\r\n" + //
				"Project Name\r\n" + //
				"--AaB03x\r\n" + //
				"Content-Disposition: form-data; name=\"upload\"; filename=\"upload.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"contents of upload.txt\r\n" + //
				"--AaB03x\r\n" + //
				"Content-Disposition: form-data; name=\"file\"; filename=\"file.txt\"\r\n" + //
				"Content-Type: text/plain\r\n" + //
				"\r\n" + //
				"contents of file.txt\r\n" + //
				"--AaB03x--\r\n";

		ServletInputStream stream = mock(ServletInputStream.class, Mockito.CALLS_REAL_METHODS);
		when(stream.read()).thenAnswer(new Answer<Integer>() {
			private final InputStream stream = new ByteArrayInputStream(FORM.getBytes());

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				return stream.read();
			}
		});

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getContentType()).thenReturn("multipart/form-data; boundary=AaB03x");
		when(request.getInputStream()).thenReturn(stream);

		Object formObject = new FormObject(request, Project.class);
		Project p = Classes.getFieldValue(formObject, "object");

		assertNotNull(p);
		assertEquals(p.name, "Project Name");
		assertNotNull(p.upload);
		assertEquals(p.upload.getContentType(), "text/plain");
		assertEquals(p.upload.getName(), "upload");
		assertEquals(p.upload.getFileName(), "upload.txt");
		assertNotNull(p.upload.getFile());
		assertTrue(p.upload.getFile().getName().endsWith(".tmp"));
		assertNotNull(p.file);
		assertTrue(p.file.getName().endsWith(".tmp"));
	}

	@Test
	public void notExistingObjectProperty() throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("fake", "SC Gnotis SRL");
		fields.put("fake.fake", "fake value");
		fields.put("taxID.fake", "RO");
		Partner p = run(fields, Partner.class);

		assertNull(p.name);
		assertNull(p.tradeRegister);
		assertNull(p.address);
		assertEquals(0, p.subsidiaries);

		assertNotNull(p.taxID);
		assertNull(p.taxID.attribute);
		assertNull(p.taxID.code);
		assertEquals(0, p.taxID.index);
	}

	@Test
	public void getVakue() throws Exception {
		Object instance = new Object();

		Class<?> clazz = Class.forName("js.tiny.container.http.form.FormObject");
		Constructor<?> constructor = clazz.getDeclaredConstructor(Object.class);
		constructor.setAccessible(true);
		FormObject formObject = (FormObject) constructor.newInstance(instance);

		assertEquals(instance, formObject.getValue());
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static <T> T run(Map<String, String> fields, Class<T> type) throws Exception {
		T instance = Classes.newInstance(type);

		Class<?> clazz = Class.forName("js.tiny.container.http.form.FormObject");
		Constructor<?> constructor = clazz.getDeclaredConstructor(Object.class);
		constructor.setAccessible(true);
		Object formObject = constructor.newInstance(instance);

		Method method = clazz.getDeclaredMethod("setValue", String.class, Object.class);
		method.setAccessible(true);

		for (String key : fields.keySet()) {
			method.invoke(formObject, key, fields.get(key));
		}
		return instance;
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class Project {
		String name;
		UploadedFile upload;
		File file;
	}

	private static class Contact {
		String name;
		String phone;
		String email;
	}

	private static class TaxID {
		String attribute;
		String code;
		int index;
	}

	private static class Partner {
		String name;
		TaxID taxID;
		String tradeRegister;
		String[] bankAccounts = new String[3];
		String address;
		List<Contact> contacts;
		int subsidiaries;
		boolean supplier;
		boolean customer;
	}

	private static class Child {
		String name;
	}

	private static class Parent {
		String name;
		List<Child> children;
	}

	private static class GeneTree {
		List<Parent> parents;
	}

	private static enum Manufacturer {
		OPEL, WOLFSVAGEN, AUDI
	}

	private static class Tire {
		String manufacturer;
		Date expiration;
		double price;
	}

	private static class Wheel {
		Tire tire;
		float pressure;
		short diameter;
	}

	private static class Car {
		Manufacturer manufacturer;
		String model;
		Date manufacturingDate;
		Wheel[] wheels = new Wheel[4];
	}

	private static class Truck extends Car {
		double weighting;
		double length;
	}

	private static class PrimitiveList {
		private List<String> strings;
		private List<Integer> integers;
	}

	private static class PrimitiveArray {
		private String[] strings = new String[2];
		private int[] integers = new int[2];
	}
}
