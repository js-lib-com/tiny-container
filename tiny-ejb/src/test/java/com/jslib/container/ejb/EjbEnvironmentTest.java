package com.jslib.container.ejb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.loadbalancer.ILoadBalancerConfig;
import com.jslib.loadbalancer.INodeConfig;

@RunWith(MockitoJUnitRunner.class)
public class EjbEnvironmentTest {
	@Mock
	private Context context;

	private EjbEnvironment ejbEnvironment;

	@Before
	public void beforeTest() {
		ejbEnvironment = new EjbEnvironment(context);
	}

	@Test
	public void GivenServiceUrl_WhenGetLoadBalancerConfig_ThenNullValues() throws NamingException {
		// given
		when(context.lookup("com.jslib.container.ejb.EjbEnvironmentTest.IService.url")).thenReturn("https://service.com/api/");

		// when
		ILoadBalancerConfig balancerConfig = ejbEnvironment.getLoadBalancerConfig(IService.class);

		// then
		assertThat(balancerConfig, notNullValue());
		assertThat(balancerConfig.getSelectorClass(), nullValue());
		assertThat(balancerConfig.getConnectionTimeout(), equalTo(0));
		assertThat(balancerConfig.getResponseTimeout(), equalTo(0));

		assertThat(balancerConfig.getNodesConfig().size(), equalTo(1));
		INodeConfig nodeConfig = balancerConfig.getNodesConfig().get(0);
		assertThat(nodeConfig, notNullValue());
		assertThat(nodeConfig.getImplementationURL(), equalTo("https://service.com/api/"));
	}

	@Test
	public void GivenServiceUrl_WhenGetLoadBalancerConfig_ThenImplementationURL() throws NamingException {
		// given
		when(context.lookup("com.jslib.container.ejb.EjbEnvironmentTest.IService.url")).thenReturn("https://service.com/api/");

		// when
		ILoadBalancerConfig balancerConfig = ejbEnvironment.getLoadBalancerConfig(IService.class);

		// then
		INodeConfig nodeConfig = balancerConfig.getNodesConfig().get(0);
		assertThat(nodeConfig.getImplementationURL(), equalTo("https://service.com/api/"));
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenNamingException_WhenGetLoadBalancerConfig_ThenException() throws NamingException {
		// given
		when(context.lookup("com.jslib.container.ejb.EjbEnvironmentTest.IService.url")).thenThrow(NamingException.class);

		// when
		ejbEnvironment.getLoadBalancerConfig(IService.class);

		// then
	}

	private static interface IService {

	}
}
