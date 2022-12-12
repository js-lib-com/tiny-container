package com.jslib.loadbalancer;

public interface INodeConfig {

	/**
	 * <pre>
	 * ejb.{ejbID}.node.{nodeID}.url
	 * </pre>
	 * 
	 * @return
	 */
	String getImplementationURL();

	/**
	 * <pre>
	 * ejb.{ejbID}.node.{nodeID}.priority
	 * </pre>
	 * 
	 * @return
	 */
	int getPriority();

	/**
	 * <pre>
	 * ejb.{ejbID}.node.{nodeID}.maxload
	 * </pre>
	 * 
	 * @return
	 */
	double getMaxLoad();

	/**
	 * <pre>
	 * ejb.{ejbID}.node.{nodeID}.ignore.ssl.fail
	 * </pre>
	 * 
	 * @return
	 */
	boolean ignoreSslFail();

}
