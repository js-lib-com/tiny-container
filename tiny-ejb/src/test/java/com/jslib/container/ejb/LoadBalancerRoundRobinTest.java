package com.jslib.container.ejb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.loadbalancer.ILoadBalancerConfig;
import com.jslib.loadbalancer.INode;
import com.jslib.loadbalancer.INodeConfig;
import com.jslib.loadbalancer.LoadBalancer;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalancerRoundRobinTest {
	@Mock
	private ILoadBalancerConfig balancerConfig;
	@Mock
	private INodeConfig node1Config;
	@Mock
	private INodeConfig node2Config;

	private LoadBalancer loadBalancer;

	@Before
	public void beforeTest() {
		when(balancerConfig.getNodesConfig()).thenReturn(Arrays.asList(node1Config, node2Config));
		when(node1Config.getImplementationURL()).thenReturn("http://server1.com/");
		when(node2Config.getImplementationURL()).thenReturn("http://server2.com/");

		loadBalancer = new LoadBalancer(balancerConfig);
	}

	@Test
	public void GivenFirstNodeRetrieve_WhenGetNode_ThenFirstNode() {
		// given

		// when
		INode node = loadBalancer.getNode();

		// then
		assertThat(node, notNullValue());
		assertThat(node.getImplementationURL(), equalTo("http://server1.com/"));
	}

	@Test
	public void GivenSecondNodeRetrieve_WhenGetNode_ThenSecondNode() {
		// given
		loadBalancer.getNode();
		
		// when
		INode node = loadBalancer.getNode();

		// then
		assertThat(node, notNullValue());
		assertThat(node.getImplementationURL(), equalTo("http://server2.com/"));
	}

	@Test
	public void GivenThirdNodeRetrieve_WhenGetNode_ThenFirstNode() {
		// given
		loadBalancer.getNode();
		loadBalancer.getNode();

		// when
		INode node = loadBalancer.getNode();

		// then
		assertThat(node, notNullValue());
		assertThat(node.getImplementationURL(), equalTo("http://server1.com/"));
	}
}
