package com.jslib.container.ejb;

import com.jslib.loadbalancer.ILoadBalancerConfig;

@FunctionalInterface
interface IConfigProvider {

	ILoadBalancerConfig getLoadBalancerConfig(Class<?> interfaceClass);

}
