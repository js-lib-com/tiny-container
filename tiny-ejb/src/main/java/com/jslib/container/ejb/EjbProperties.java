package com.jslib.container.ejb;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.loadbalancer.ILoadBalancerConfig;
import com.jslib.loadbalancer.INodeConfig;
import com.jslib.util.Strings;

/**
 * Load balancer configuration loaded from EJB properties file. EJB properties file is named <code>ejb.properties</code> and is
 * loaded from directory where Tiny EJB module is loaded.
 * 
 * @author Iulian Rotaru
 */
class EjbProperties implements IConfigProvider {
	private final Properties properties;

	public EjbProperties(File propertiesFile) throws IOException {
		this.properties = new Properties();
		try (Reader reader = new FileReader(propertiesFile)) {
			this.properties.load(reader);
		}
	}

	// test
	EjbProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	public ILoadBalancerConfig getLoadBalancerConfig(Class<?> interfaceClass) {
		String ejbID = properties.getProperty(interfaceClass.getCanonicalName());
		if (ejbID == null) {
			throw new ServiceConfigurationException("Bad EJB properties. Missing EJB ID for %s", interfaceClass.getCanonicalName());
		}
		return new LoadBalancerConfig(ejbID);
	}

	// --------------------------------------------------------------------------------------------

	private final class LoadBalancerConfig extends Config implements ILoadBalancerConfig {
		public LoadBalancerConfig(String ejbID) {
			super(Strings.concat("ejb.", ejbID, '.'));
		}

		@Override
		public String getSelectorClass() {
			return string("selector");
		}

		@Override
		public int getConnectionTimeout() {
			return integer("timeout.connection");
		}

		@Override
		public int getResponseTimeout() {
			return integer("timeout.response");
		}

		@Override
		public List<INodeConfig> getNodesConfig() {
			String value = string("nodes");
			if (value == null) {
				throw new ServiceConfigurationException("Bad EJB properties. Missing node IDs.");
			}
			List<String> nodes = Strings.split(value, ',');
			List<INodeConfig> configs = new ArrayList<>();
			nodes.forEach(node -> configs.add(new NodeConfig(Strings.concat(prefix, "node.", node, '.'))));
			return configs;
		}
	}

	private final class NodeConfig extends Config implements INodeConfig {
		public NodeConfig(String prefix) {
			super(prefix);
		}

		@Override
		public String getImplementationURL() {
			return string("url");
		}

		@Override
		public int getPriority() {
			return integer("priority");
		}

		@Override
		public double getMaxLoad() {
			return real("maxload");
		}

		@Override
		public boolean ignoreSslFail() {
			return bool("ignore.ssl.fail");
		}
	}

	private class Config {
		protected String prefix;

		public Config(String prefix) {
			this.prefix = prefix;
		}

		protected String string(String name) {
			return properties.getProperty(prefix + name);
		}

		protected int integer(String property) {
			String value = string(property);
			try {
				return value != null ? Integer.parseInt(value) : 0;
			} catch (NumberFormatException e) {
				throw new ServiceConfigurationException("Bad EJB properties. Not integer value %s on property %s.", value, property);
			}
		}

		protected double real(String property) {
			String value = string(property);
			try {
				return value != null ? Double.parseDouble(value) : 0.0D;
			} catch (NumberFormatException e) {
				throw new ServiceConfigurationException("Bad EJB properties. Not double value %s on property %s.", value, property);
			}
		}

		protected boolean bool(String property) {
			String value = string(property);
			if (value == null) {
				return false;
			}
			if (value.equalsIgnoreCase("true")) {
				return true;
			}
			if (value.equalsIgnoreCase("false")) {
				return false;
			}
			throw new ServiceConfigurationException("Bad EJB properties. Not boolean value %s on property %s.", value, property);
		}
	}
}
