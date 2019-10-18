package js.tiny.container.unit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import js.lang.ConfigException;
import js.tiny.container.servlet.TinyConfigBuilder;
import js.util.Classes;

public class TestConfigBuilder extends TinyConfigBuilder {
	public TestConfigBuilder() throws ConfigException {
		this((InputStream) null);
	}

	public TestConfigBuilder(String config) throws ConfigException {
		this(new ByteArrayInputStream(config.getBytes()));
	}

	public TestConfigBuilder(String config, Properties properties) throws ConfigException {
		this(new ByteArrayInputStream(config.getBytes()), properties);
	}

	public TestConfigBuilder(File config) throws FileNotFoundException, ConfigException {
		this(new FileInputStream(config));
	}

	public TestConfigBuilder(InputStream config) throws ConfigException {
		this(config, new Properties());
	}

	public TestConfigBuilder(InputStream config, Properties properties) throws ConfigException {
		super("test-app");
		Loader loader = new Loader(build(), properties);
		loadXML(Classes.getResourceAsStream("lib-descriptor.xml"), loader);
		if (config != null) {
			loadXML(config, loader);
		}
	}
}
