package com.jslib.container.ejb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import com.jslib.loadbalancer.ILoadBalancerConfig;

/**
 * Cache for Java dynamic proxies to remote managed classes, aka EJBs. Cache key is the remote managed class interface and cache
 * value is a proxy instance, reused for all application fields of the same type - of course fields annotated with {@literal}EJB
 * annotation.
 * 
 * @author Iulian Rotaru
 */
class EjbProxies {
	private final Map<Class<?>, Object> proxiesCache;
	private final IConfigProvider configProvider;

	public EjbProxies() throws IOException, NamingException {
		this.proxiesCache = new HashMap<>();

		File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		File propertiesFile = new File(jarFile.getParentFile(), "ejb.properties");

		if (propertiesFile.exists()) {
			this.configProvider = new EjbProperties(propertiesFile);
		} else {
			this.configProvider = new EjbEnvironment();
		}
	}

	/**
	 * Create and cache Java dynamic proxy for remote managed class, if not already exists. Remote managed class is identified
	 * by its interface, used as cache key when retrieve proxy instance - see {@link #getProxy(Class)}. If proxy instance is
	 * already created this method silently does nothing.
	 * 
	 * @param interfaceClass interface of the remote managed class, used as cache key.
	 */
	public void createProxy(Class<?> interfaceClass) {
		if (!proxiesCache.containsKey(interfaceClass)) {
			synchronized (this) {
				if (!proxiesCache.containsKey(interfaceClass)) {
					ILoadBalancerConfig config = configProvider.getLoadBalancerConfig(interfaceClass);
					Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] { interfaceClass }, new EjbProxyHandler(config));
					proxiesCache.put(interfaceClass, proxy);
				}
			}
		}
	}

	/**
	 * Get proxy instance for requested remote managed class, identified by its interface. Interface class is used as cache key
	 * and should match {@link #createProxy(Class)} key. If there is no proxy created for requested interface class this method
	 * throws illegal state exception.
	 * 
	 * @param interfaceClass interface of the remote managed class, used as cache key.
	 * @return not null Java dynamic proxy to remote managed class.
	 * @throws IllegalStateException if there is no proxy cached for requested interface class.
	 */
	public Object getProxy(Class<?> interfaceClass) {
		Object proxy = proxiesCache.get(interfaceClass);
		if (proxy == null) {
			throw new IllegalStateException("Not registered remote managed interface " + interfaceClass);
		}
		return proxy;
	}
}
