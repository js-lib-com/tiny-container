package com.jslib.container.ejb;

import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.loadbalancer.ILoadBalancerConfig;
import com.jslib.loadbalancer.INodeConfig;
import com.jslib.util.Strings;

/**
 * Single node load balancer configuration loaded from simple environment entry. The way environment entry is declared depends
 * on specific runtime; here is a sample from Tomcat context.
 * 
 * <pre>
 * <Context>
 * 	<Environment name="com.server.IHumanResources.url" type="java.lang.String" value="https://com.server/" />
 * </Context>
 * </pre>
 * 
 * This configuration approach is used when load balancer is actually not used. It provides a mock load balancer configuration
 * with a single node; the node has only implementation URL property.
 * 
 * @author Iulian Rotaru
 */
class EjbEnvironment implements IConfigProvider {
	private static final Log log = LogFactory.getLog(EjbEnvironment.class);

	private static final String COMP_ENV = "java:comp/env";

	private final Context context;

	public EjbEnvironment() throws NamingException {
		Context root = new InitialContext();
		context = (Context) root.lookup(COMP_ENV);
	}

	// test
	EjbEnvironment(Context context) {
		this.context = context;
	}

	@Override
	public ILoadBalancerConfig getLoadBalancerConfig(Class<?> interfaceClass) {
		String ejbUrl = Strings.concat(interfaceClass.getCanonicalName(), ".url");
		try {
			String implementationURL = (String) context.lookup(ejbUrl);
			return new LoadBalancerConfig(implementationURL);
		} catch (NamingException e) {
			log.error("JNDI lookup fail on EJB |{ejb_class}|. Root cause: {exception_class}: {}", ejbUrl, e.getClass(), e.getMessage());
			throw new ServiceConfigurationException("No implementation URL for EJB %s on environment.", interfaceClass);
		}
	}

	// --------------------------------------------------------------------------------------------

	private static final class LoadBalancerConfig implements ILoadBalancerConfig {
		private final List<INodeConfig> nodesConfiguration;

		public LoadBalancerConfig(String implementationURL) {
			this.nodesConfiguration = Arrays.asList(new NodeConfig(implementationURL));
		}

		@Override
		public String getSelectorClass() {
			return null;
		}

		@Override
		public int getConnectionTimeout() {
			return 0;
		}

		@Override
		public int getResponseTimeout() {
			return 0;
		}

		@Override
		public List<INodeConfig> getNodesConfig() {
			return nodesConfiguration;
		}
	}

	private static final class NodeConfig implements INodeConfig {
		private final String implementationURL;

		public NodeConfig(String implementationURL) {
			this.implementationURL = implementationURL;
		}

		@Override
		public String getImplementationURL() {
			return implementationURL;
		}

		@Override
		public int getPriority() {
			return 0;
		}

		@Override
		public double getMaxLoad() {
			return 1.0D;
		}

		@Override
		public boolean ignoreSslFail() {
			return true;
		}
	}
}
