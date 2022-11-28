package com.jslib.loadbalancer;

import java.util.List;

public class RoundRobinNodeSelector implements INodeSelector {
	private int index;

	@Override
	public INode selectNode(List<INode> nodes, int totalUsageCount) {
//		double load = (double)nodes.get(0).getUsageCount() / (double)totalUsageCount;
//		if(load >= node.getMaxLoad()) {
//			
//		}
		return nodes.get(index++ % nodes.size());
	}
}
