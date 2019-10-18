package js.tiny.container.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Stack;

import javax.servlet.ServletContext;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.Container;
import js.util.Classes;
import js.util.Strings;

/**
 * Builder for configuration object used to configure tiny container. Create tiny container configuration object and load it
 * from external library and application descriptors. By convention configuration object has the same name as the web
 * application that creates the container. See {@link Container} for container descriptor description.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class TinyConfigBuilder extends ConfigBuilder {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(TinyConfigBuilder.class);

	/** Name for application deployed on ROOT context. */
	private static final String ROOT_APP_NAME = "ROOT";

	/**
	 * Configuration object for tiny container loaded from library and application descriptors. This is the root of
	 * configuration objects tree and is passed to loader to actually populate it from XML streams.
	 */
	private Config config;

	/**
	 * Test constructor creates and empty configuration object with given name.
	 * 
	 * @param configName configuration object name.
	 */
	protected TinyConfigBuilder(String configName) {
		config = new Config(configName);
	}

	/**
	 * Construct application configuration instance with information from given servlet context.
	 * 
	 * @param context Servlet context bound to application,
	 * @param properties context properties defining variables to inject on descriptors.
	 * @throws ConfigException if application descriptor is not well formed or invalid.
	 */
	public TinyConfigBuilder(ServletContext context, Properties properties) throws ConfigException {
		config = new Config(getAppName(context.getContextPath()));

		File contextDir = new File(context.getRealPath(""));
		File webinfDir = new File(contextDir, "WEB-INF");

		Loader loader = new Loader(config, properties);

		// first load library descriptor
		loadXML(Classes.getResourceAsStream("lib-descriptor.xml"), loader);

		// load application specific configuration document(s)
		File file = new File(webinfDir, "app.xml");
		try {
			loadXML(new FileInputStream(file), loader);
		} catch (FileNotFoundException e) {
			log.debug("Application |%s| has no descriptor. No application specific configuration.", config.getName());
			new ConfigException(e);
		}

		// load plugin configurations
		for (TinyConfigProvider configProvider : ServiceLoader.load(TinyConfigProvider.class)) {
			config.addChildren(configProvider.getConfig());
		}
	}

	/**
	 * Return configuration object. This method, on current implementation, is just a getter. Configuration object loading is
	 * performed by constructor.
	 * 
	 * @return tiny container configuration object.
	 */
	@Override
	public Config build() {
		return config;
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Get application name from servlet context path. Context path is used to identify web application within request URI.
	 * Application name is context path without leading path separator and with normalized root name. Normalized root name uses
	 * {@link #ROOT_APP_NAME} when context path is empty.
	 * 
	 * @param contextPath servlet context path with leading path separator.
	 * @return application name, never null.
	 */
	private static String getAppName(String contextPath) {
		// as result from api-doc context path always start with '/' and is empty for root context
		return contextPath.isEmpty() ? ROOT_APP_NAME : contextPath.substring(1);
	}

	/**
	 * Load configuration document from file.
	 * 
	 * @param inputStream configuration XML file,
	 * @param loader SAX handler in charge of configuration loading.
	 * @throws ConfigException if XML document loading fails.
	 */
	protected static void loadXML(InputStream inputStream, Loader loader) throws ConfigException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(loader);
			reader.parse(new InputSource(inputStream));
		} catch (Exception e) {
			throw new ConfigException("Fail to load configuration document from file |%s|: %s", inputStream, e);
		}
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASSES

	/**
	 * Parse XML stream and update configuration objects tree with the given root.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	public static class Loader extends DefaultHandler {
		/** Tag name for property element. */
		private static final String TAG_PROPERTY = "property";

		/**
		 * Stack of parent configuration objects updated at opening tags, respective closing tags. The top of this stack always
		 * contain current parent configuration object.
		 */
		private final Stack<Config> parentsStack = new Stack<>();

		/** Optional properties, default to empty, for variables resolving. */
		private final Properties properties;

		/** Nesting level. */
		private int nestingLevel;

		/**
		 * Construct loader with given root configuration object.
		 * 
		 * @param config root configuration object.
		 */
		public Loader(Config config) {
			this(config, new Properties());
		}

		/**
		 * Construct loader with given root configuration object and properties used to resolve variables.
		 * 
		 * @param config root configuration object,
		 * @param properties properties used to resolve variables.
		 */
		public Loader(Config config, Properties properties) {
			this.parentsStack.push(config);
			this.properties = properties;
		}

		/**
		 * Create configuration objects with name based on given qualified name and insert into configuration objects tree. Also
		 * updates nesting level and configuration objects stack.
		 * 
		 * @param uri name space URI, unused by current implementation,
		 * @param localName local name relative to name space, unused by current implementation,
		 * @param qName element qualified name,
		 * @param attributes element attributes.
		 * @throws SAXException if a required attribute or variable is missing or not with bad syntax.
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (nestingLevel == 0) {
				++nestingLevel;
				return;
			}

			if (TAG_PROPERTY.equals(qName)) {
				Config config = parentsStack.peek();
				config.setProperty(value(attributes, "name"), value(attributes, "value"));
			} else {
				Config config = new Config(qName);
				Config parent = parentsStack.peek();
				parent.addChild(config);
				parentsStack.push(config);

				for (int i = 0; i < attributes.getLength(); ++i) {
					final String name = attributes.getQName(i);
					config.setAttribute(name, value(attributes, name));
				}
			}

			++nestingLevel;
		}

		/**
		 * Decrease nesting level and remove elements stack top.
		 * 
		 * @param uri name space URI, unused by current implementation,
		 * @param localName local name relative to name space, unused by current implementation,
		 * @param qName element qualified name.
		 */
		@Override
		public void endElement(String uri, String localName, String qName) {
			--nestingLevel;
			if (!TAG_PROPERTY.equals(qName) && nestingLevel > 0) {
				parentsStack.pop();
			}
		}

		// ----------------------------------------------------------------------------------------
		// UTILITY METHODS

		/**
		 * Get attribute value resolving variable, if any. Return attribute value, never null. Throws exception if there is no
		 * attribute with requested name.
		 * <p>
		 * If attribute value denotes a variable resolve it from this loader {@link #properties} or system properties, in this
		 * order. Variable syntax is standard dollar notation, e.g. <code>${variable.name}</code>. Is good practice to use
		 * qualified names for variables.
		 * <p>
		 * Is legal for a variable to not be resolved in which case returned value is variable name itself, that is, attribute
		 * value as it is.
		 * 
		 * @param attributes attributes list,
		 * @param attributeName attribute name to load value for.
		 * @return variable value, never null.
		 * @throws SAXException if there is no attribute with requested name or variable with bad syntax.
		 */
		private String value(Attributes attributes, String attributeName) throws SAXException {
			String value = attributes.getValue(attributeName);
			if (value == null) {
				throw new SAXException(new ConfigException("Missing attribute |%s|.", attributeName));
			}

			String variableName = getVariableName(value);
			if (variableName == null) {
				return value;
			}
			String variableValue = properties.getProperty(variableName);
			if (variableValue == null) {
				variableValue = System.getProperty(variableName);
			}
			if (variableValue == null) {
				log.debug("Missing variable |%s|.", variableName);
				return value;
			}
			return value.replace(Strings.concat("${", variableName, '}'), variableValue);
		}

		/**
		 * Get variable name from string value or null if value does not contain a variable. Variable uses standard dollar
		 * syntax, that is, <code>${variable.name}</code>. This method returns variable name. If given string value is not a
		 * variable returns null.
		 * 
		 * @param value string value, not null.
		 * @return variable name or null is none found.
		 * @throws SAXException if variable name syntax is wrong.
		 * @throws NullPointerException if <code>value</code> argument is null.
		 */
		private static String getVariableName(String value) throws SAXException {
			int variableStartIndex = value.indexOf("${");
			if (variableStartIndex == -1) {
				return null;
			}
			int variableEndIndex = value.indexOf('}', variableStartIndex);
			if (variableEndIndex == -1) {
				throw new SAXException(String.format("Bad variable value |%s|. Missing closing mark.", value));
			}
			return value.substring(variableStartIndex + 2, variableEndIndex);
		}
	}
}
