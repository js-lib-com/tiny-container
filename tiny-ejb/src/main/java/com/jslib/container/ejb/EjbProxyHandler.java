package com.jslib.container.ejb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.loadbalancer.ILoadBalancerConfig;
import com.jslib.loadbalancer.INode;
import com.jslib.loadbalancer.LoadBalancer;
import com.jslib.net.client.HttpRmiClient;

import jakarta.inject.Inject;

class EjbProxyHandler implements InvocationHandler {
	private static final Log log = LogFactory.getLog(EjbProxyHandler.class);

	private final LoadBalancer loadBalancer;

	@Inject
	public EjbProxyHandler(ILoadBalancerConfig config) {
		this.loadBalancer = new LoadBalancer(config);
	}

	EjbProxyHandler(LoadBalancer loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
		INode node = loadBalancer.getNode();
		node.acquire();

		String ejbClass = method.getDeclaringClass().getCanonicalName();
		try {
			log.debug("Invoke EJB {java_type} on remote implementation {}.", ejbClass, node.getImplementationURL());
			HttpRmiClient client = new HttpRmiClient(node.getImplementationURL(), ejbClass);
			String traceId = LogFactory.getLogContext().get("trace_id");
			if (traceId != null) {
				client.setHttpHeader("X-Trace-Id", traceId);
			}

			if (loadBalancer.hasConnectionTimeout()) {
				client.setConnectionTimeout(loadBalancer.getConnectionTimeout());
			}
			if (loadBalancer.hasResponseTimeout()) {
				client.setResponseTimeout(loadBalancer.getResponseTimeout());
			}
			client.setReturnType(method.getGenericReturnType());
			client.setExceptions(method.getExceptionTypes());

			return client.invoke(method.getName(), arguments);
		} catch (Throwable t) {
			node.incrementsErrorsCount();
			throw t;
		} finally {
			long processingTime = node.release();
			log.info("EJB {java_type} invocation complete. Processing time {processing_time} msec.", ejbClass, processingTime);
		}
	}
}
