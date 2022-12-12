package com.jslib.loadbalancer;

import java.util.List;

public interface ILoadBalancerConfig {

	/**
	 * <pre>
	 * ejb.{ejbID}.selector
	 * </pre>
	 * 
	 * @return
	 */
	String getSelectorClass();

	/**
	 * <pre>
	 * ejb.{ejbID}.timeout.connection
	 * </pre>
	 * 
	 * @return
	 */
	int getConnectionTimeout();

	/**
	 * <pre>
	 * ejb.{ejbID}.timeout.response
	 * </pre>
	 * 
	 * @return
	 */
	int getResponseTimeout();

	/**
	 * <pre>
	 * ejb.{ejbID}.nodes
	 * </pre>
	 * 
	 * @return
	 */
	List<INodeConfig> getNodesConfig();

}
