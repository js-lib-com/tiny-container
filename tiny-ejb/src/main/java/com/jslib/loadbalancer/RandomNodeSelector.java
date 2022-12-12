package com.jslib.loadbalancer;

import java.util.List;
import java.util.Random;

public class RandomNodeSelector implements INodeSelector {
	@Override
	public INode selectNode(List<INode> nodes, int totalUsageCount) {
		final Random random = new Random();
		final int randomSelection = random.nextInt(nodes.size());
		return nodes.get(randomSelection);
	}
}
