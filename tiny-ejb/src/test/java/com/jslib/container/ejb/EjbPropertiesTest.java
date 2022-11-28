package com.jslib.container.ejb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.loadbalancer.ILoadBalancerConfig;
import com.jslib.loadbalancer.INodeConfig;

@RunWith(MockitoJUnitRunner.class)
public class EjbPropertiesTest {
	@Mock
	private Properties properties;

	private EjbProperties ejbProperties;

	@Before
	public void beforeTest() {
		ejbProperties = new EjbProperties(properties);
	}

	@Test
	public void GivenNotInitializedProperties_WhenGetLoadBalancerConfig_ThenNullValues() {
		// given
		when(properties.getProperty("com.jslib.container.ejb.EjbPropertiesTest.IService")).thenReturn("1");
		when(properties.getProperty("ejb.1.nodes")).thenReturn("a");

		// when
		ILoadBalancerConfig balancerConfig = ejbProperties.getLoadBalancerConfig(IService.class);

		// then
		assertThat(balancerConfig, notNullValue());
		assertThat(balancerConfig.getSelectorClass(), nullValue());
		assertThat(balancerConfig.getConnectionTimeout(), equalTo(0));
		assertThat(balancerConfig.getResponseTimeout(), equalTo(0));
		
		assertThat(balancerConfig.getNodesConfig().size(), equalTo(1));
		INodeConfig nodeConfig = balancerConfig.getNodesConfig().get(0);
		assertThat(nodeConfig, notNullValue());
		assertThat(nodeConfig.getImplementationURL(), nullValue());
		assertThat(nodeConfig.getPriority(), equalTo(0));
		assertThat(nodeConfig.getMaxLoad(), equalTo(0.0D));
		assertThat(nodeConfig.ignoreSslFail(), equalTo(false));
	}

	@Test
	public void GivenInitializedProperties_WhenGetLoadBalancerConfig_ThenValues() {
		// given
		when(properties.getProperty("com.jslib.container.ejb.EjbPropertiesTest.IService")).thenReturn("1");
		when(properties.getProperty("ejb.1.selector")).thenReturn("com.jslib.container.ejb.RandomNodeSelector");
		when(properties.getProperty("ejb.1.timeout.connection")).thenReturn("4000");
		when(properties.getProperty("ejb.1.timeout.response")).thenReturn("10000");
		
		when(properties.getProperty("ejb.1.nodes")).thenReturn("a");
		when(properties.getProperty("ejb.1.node.a.url")).thenReturn("https://server.com/api/");
		when(properties.getProperty("ejb.1.node.a.priority")).thenReturn("100");
		when(properties.getProperty("ejb.1.node.a.maxload")).thenReturn("0.1");
		when(properties.getProperty("ejb.1.node.a.ignore.ssl.fail")).thenReturn("true");
		                             
		// when
		ILoadBalancerConfig balancerConfig = ejbProperties.getLoadBalancerConfig(IService.class);

		// then
		assertThat(balancerConfig, notNullValue());
		assertThat(balancerConfig.getSelectorClass(), equalTo("com.jslib.container.ejb.RandomNodeSelector"));
		assertThat(balancerConfig.getConnectionTimeout(), equalTo(4000));
		assertThat(balancerConfig.getResponseTimeout(), equalTo(10000));
		
		assertThat(balancerConfig.getNodesConfig().size(), equalTo(1));
		INodeConfig nodeConfig = balancerConfig.getNodesConfig().get(0);
		assertThat(nodeConfig, notNullValue());
		assertThat(nodeConfig.getImplementationURL(), equalTo("https://server.com/api/"));
		assertThat(nodeConfig.getPriority(), equalTo(100));
		assertThat(nodeConfig.getMaxLoad(), equalTo(0.1D));
		assertThat(nodeConfig.ignoreSslFail(), equalTo(true));
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenNotIntegerConnectionTimeout_WhenGetConnectionTimeout_ThenException() {
		// given
		when(properties.getProperty("com.jslib.container.ejb.EjbPropertiesTest.IService")).thenReturn("1");
		when(properties.getProperty("ejb.1.timeout.connection")).thenReturn("4000.1");
		                             
		// when
		ILoadBalancerConfig balancerConfig = ejbProperties.getLoadBalancerConfig(IService.class);
		balancerConfig.getConnectionTimeout();

		// then
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenNotDoubleMaxLoad_WhenGetMaxLoad_ThenException() {
		// given
		when(properties.getProperty("com.jslib.container.ejb.EjbPropertiesTest.IService")).thenReturn("1");
		when(properties.getProperty("ejb.1.nodes")).thenReturn("a");
		when(properties.getProperty("ejb.1.node.a.maxload")).thenReturn("0.1X");
		                             
		// when
		ILoadBalancerConfig balancerConfig = ejbProperties.getLoadBalancerConfig(IService.class);
		balancerConfig.getNodesConfig().get(0).getMaxLoad();

		// then
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenNotBooleanIgnoreSslFail_WhenGetIgnoreSslFail_ThenException() {
		// given
		when(properties.getProperty("com.jslib.container.ejb.EjbPropertiesTest.IService")).thenReturn("1");
		when(properties.getProperty("ejb.1.nodes")).thenReturn("a");
		when(properties.getProperty("ejb.1.node.a.ignore.ssl.fail")).thenReturn("yes");
		                             
		// when
		ILoadBalancerConfig balancerConfig = ejbProperties.getLoadBalancerConfig(IService.class);
		balancerConfig.getNodesConfig().get(0).ignoreSslFail();

		// then
	}

	private static interface IService {

	}
}
