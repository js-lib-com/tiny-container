package com.jslib.loadbalancer;

import java.util.List;

public interface INodeSelector {

	INode selectNode(List<INode> nodes, int totalUsageCount);
	
}
