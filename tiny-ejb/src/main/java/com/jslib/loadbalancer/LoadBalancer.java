package com.jslib.loadbalancer;

import java.util.List;
import java.util.Vector;

import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.util.Classes;

public class LoadBalancer {
	private final INodeSelector selector;
	private final int connectionTimeout;
	private final int responseTimeout;

	private final List<INode> nodes;

	private int totalUsageCount;

	public LoadBalancer(ILoadBalancerConfig config) {
		if (config.getNodesConfig().isEmpty()) {
			throw new ServiceConfigurationException("Bad load balancer configuration. Missing nodes.");
		}

		String selectorClass = config.getSelectorClass();
		if (selectorClass == null) {
			selectorClass = RoundRobinNodeSelector.class.getCanonicalName();
		}
		this.selector = Classes.newInstance(selectorClass);
		this.connectionTimeout = config.getConnectionTimeout();
		this.responseTimeout = config.getResponseTimeout();

		this.nodes = new Vector<>();
		config.getNodesConfig().forEach(nodeConfig -> this.nodes.add(new Node(nodeConfig)));
	}

	public boolean hasConnectionTimeout() {
		return connectionTimeout != 0;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public boolean hasResponseTimeout() {
		return responseTimeout != 0;
	}

	public int getResponseTimeout() {
		return responseTimeout;
	}

	public INode getNode() {
		if (nodes.size() == 1) {
			return nodes.get(0);
		}
		++totalUsageCount;
		return selector.selectNode(nodes, totalUsageCount);
	}
}
