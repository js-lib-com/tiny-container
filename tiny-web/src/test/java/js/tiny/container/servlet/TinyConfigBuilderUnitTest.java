package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.servlet.TinyConfigBuilder.Loader;
import js.tiny.container.unit.SaxAttributesStub;
import js.tiny.container.unit.ServletContextStub;
import js.util.Classes;

public class TinyConfigBuilderUnitTest {
	private Properties properties;

	@Before
	public void beforeTest() {
		properties = new Properties();
	}

	@Test
	public void constructor() throws ConfigException {
		MockServletContext context = new MockServletContext();
		context.contextPath = "/test-app";
		TinyConfigBuilder builder = new TinyConfigBuilder(context, properties);
		Config config = builder.build();

		assertNotNull(config);
		assertEquals(context.contextPath.substring(1), config.getName());
		assertEquals(3, config.getChildrenCount());
		assertEquals("managed-classes", config.getChild(0).getName());
	}

	@Test
	public void constructor_MissingAppDescriptor() throws ConfigException {
		MockServletContext context = new MockServletContext();
		context.contextPath = "/null-app";
		TinyConfigBuilder builder = new TinyConfigBuilder(context, properties);
		Config config = builder.build();

		assertNotNull(config);
		assertEquals(context.contextPath.substring(1), config.getName());
		assertEquals(1, config.getChildrenCount());
		assertEquals("managed-classes", config.getChild(0).getName());
	}

	@Test(expected = ConfigException.class)
	public void constructor_BadAppDescriptor() throws ConfigException {
		MockServletContext context = new MockServletContext();
		context.contextPath = "/bad-app";
		new TinyConfigBuilder(context, properties);
	}

	@Test
	public void subclass() {
		class MockTinyConfigBuilder extends TinyConfigBuilder {
			protected MockTinyConfigBuilder() {
				super("config");
			}
		}
		MockTinyConfigBuilder builder = new MockTinyConfigBuilder();
		Config config = builder.build();

		assertNotNull(config);
		assertEquals("config", config.getName());
		assertEquals(0, config.getChildrenCount());
	}

	@Test
	public void getAppName() throws Exception {
		String appName = Classes.invoke(TinyConfigBuilder.class, "getAppName", "/test-app");
		assertNotNull(appName);
		assertEquals("test-app", appName);
	}

	@Test
	public void getAppName_ROOT() throws Exception {
		String appName = Classes.invoke(TinyConfigBuilder.class, "getAppName", "");
		assertNotNull(appName);
		assertEquals("ROOT", appName);
	}

	@Test
	public void loadXML() throws Exception {
		String descriptor = "" + //
				"<config>" + //
				"	<managed-classes>" + //
				"		<app class='js.core.App' />" + //
				"	</managed-classes>" + //
				"	<views>" + //
				"		<repository>" + //
				"			<property name='name' value='value' />" + //
				"		</repository>" + //
				"	</views>" + //
				"	<managed-classes>" + //
				"		<app-context class='js.core.AppContext' />" + //
				"	</managed-classes>" + //
				"</config>";

		Config config = new Config("config");
		TinyConfigBuilder.Loader loader = new TinyConfigBuilder.Loader(config);

		Classes.invoke(TinyConfigBuilder.class, "loadXML", new ByteArrayInputStream(descriptor.getBytes()), loader);

		assertEquals(3, config.getChildrenCount());
		assertEquals(1, config.getChild(0).getChildrenCount());
		assertEquals(1, config.getChild(1).getChildrenCount());
		assertEquals(1, config.getChild(2).getChildrenCount());

		assertEquals("managed-classes", config.getChild(0).getName());
		assertEquals("views", config.getChild(1).getName());
		assertEquals("managed-classes", config.getChild(2).getName());
		assertEquals("app", config.getChild(0).getChild(0).getName());
		assertEquals("repository", config.getChild(1).getChild(0).getName());
		assertEquals("app-context", config.getChild(2).getChild(0).getName());

		assertEquals("value", config.getChild(1).getChild(0).getProperty("name"));
		Properties properties = config.getChild(1).getChild(0).getProperties();
		assertEquals(1, properties.size());
		assertEquals("value", properties.getProperty("name"));
	}

	@Test
	public void getVariableName() throws Exception {
		assertEquals("js.variable.name", getVariableName("${js.variable.name}"));
		assertNull(getVariableName("$js.variable.name"));
		assertNull(getVariableName("js.variable.name"));
		assertNull(getVariableName(""));
	}

	@Test(expected = SAXException.class)
	public void getVariableName_BadSyntax() throws Exception {
		getVariableName("${js.variable.name");
	}

	@Test(expected = NullPointerException.class)
	public void getVariableName_NullValue() throws Exception {
		getVariableName(null);
	}

	@Test
	public void value() throws Exception {
		MockAttributes attributes = new MockAttributes();
		attributes.attributes.put("name", "value");

		TinyConfigBuilder.Loader loader = new TinyConfigBuilder.Loader(null);
		assertEquals("value", value(loader, attributes, "name"));
	}

	@Test
	public void value_Variable() throws Exception {
		MockAttributes attributes = new MockAttributes();
		attributes.attributes.put("name", "${js.variable.name}");

		properties.put("js.variable.name", "variable-value");
		TinyConfigBuilder.Loader loader = new TinyConfigBuilder.Loader(null, properties);

		assertEquals("variable-value", value(loader, attributes, "name"));
	}

	@Test
	public void value_SystemVariable() throws Exception {
		MockAttributes attributes = new MockAttributes();
		attributes.attributes.put("name", "${js.variable.name}");

		System.setProperty("js.variable.name", "variable-value");
		TinyConfigBuilder.Loader loader = new TinyConfigBuilder.Loader(null, properties);

		assertEquals("variable-value", value(loader, attributes, "name"));
		System.clearProperty("js.variable.name");
	}

	@Test(expected = SAXException.class)
	public void value_MissingAttribute() throws Exception {
		MockAttributes attributes = new MockAttributes();

		TinyConfigBuilder.Loader loader = new TinyConfigBuilder.Loader(null, properties);
		value(loader, attributes, "name");
	}

	/**
	 * Is legal for a variable to not be resolved in which case returned value is variable name itself, that is, attribute value
	 * as it is.
	 */
	@Test
	public void value_MissingVariable() throws Exception {
		MockAttributes attributes = new MockAttributes();
		attributes.attributes.put("name", "${js.variable.name}");

		TinyConfigBuilder.Loader loader = new TinyConfigBuilder.Loader(null, properties);
		assertEquals("${js.variable.name}", value(loader, attributes, "name"));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static String getVariableName(String value) throws Exception {
		return Classes.invoke(TinyConfigBuilder.Loader.class, "getVariableName", value);
	}

	private static String value(Loader loader, Attributes attributes, String attributeName) throws Exception {
		return Classes.invoke(loader, "value", attributes, attributeName);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockServletContext extends ServletContextStub {
		private File appDir = new File("fixture/tomcat/webapps");
		private String contextPath;

		@Override
		public String getContextPath() {
			return contextPath;
		}

		@Override
		public String getRealPath(String resource) {
			return new File(new File(appDir, contextPath), resource).getPath();
		}
	}

	private static class MockAttributes extends SaxAttributesStub {
		private Map<String, String> attributes = new HashMap<>();

		@Override
		public String getValue(String qName) {
			return attributes.get(qName);
		}
	}
}
