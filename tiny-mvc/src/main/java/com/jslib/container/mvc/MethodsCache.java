package com.jslib.container.mvc;

import java.util.HashMap;
import java.util.Map;

import com.jslib.container.http.Resource;
import com.jslib.container.mvc.annotation.Controller;
import com.jslib.container.mvc.annotation.RequestPath;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.servlet.RequestPreprocessor;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.util.Strings;

/**
 * Cache for resource methods. A resource method is one returning a {@link Resource} instance.
 * 
 * @author Iulian Rotaru
 */
public class MethodsCache {
	private final Map<String, IManagedMethod> cache = new HashMap<>();

	public String add(IManagedMethod method) {
		String key = key(method);
		cache.put(key, method);
		return key;
	}

	public IManagedMethod get(String requestPath) {
		return cache.get(key(requestPath));
	}

	static String key(IManagedMethod managedMethod) {
		StringBuilder key = new StringBuilder();
		String classPath = path(managedMethod.getDeclaringClass());
		if (classPath != null) {
			key.append('/');
			key.append(classPath);
		}
		key.append('/');
		String methodPath = path(managedMethod);
		if (methodPath == null) {
			methodPath = Strings.memberToDashCase(managedMethod.getName());
		}
		key.append(methodPath);
		return key.toString();
	}

	private static String path(IManagedClass<?> managedClass) {
		Controller controller = managedClass.scanAnnotation(Controller.class);
		String value = controller != null ? controller.value() : null;
		if (value != null) {
			value = value.trim();
		}
		return value != null && !value.isEmpty() ? value : null;
	}

	private static String path(IManagedMethod managedMethod) {
		RequestPath requestPath = managedMethod.scanAnnotation(RequestPath.class);
		String value = requestPath != null ? requestPath.value() : null;
		if (value != null) {
			value = value.trim();
		}
		return value != null && !value.isEmpty() ? value : null;
	}

	/**
	 * Generate retrieval key for resource methods cache. This key is used by request routing logic to locate resource method to
	 * invoke. It is based on request path extracted from request URI, see {@link RequestPreprocessor} and
	 * {@link RequestContext#getRequestPath()} - and should be identical with storage key.
	 * <p>
	 * Retrieval key syntax is identical with storage key but is based on request path, that on its turn is extracted from
	 * request URI. In fact this method just trim query parameters and extension, if any.
	 * 
	 * <pre>
	 * request-path = ["/" controller] "/" resource ["." extension] ["?" query-string]
	 * key = ["/" controller-path ] "/" resource-path
	 * controller-path = request-path controller
	 * resource-path = request-path resource
	 * </pre>
	 * 
	 * @param requestPath request path identify resource to retrieve.
	 * @return resource method key.
	 */
	static String key(String requestPath) {
		int queryParametersIndex = requestPath.lastIndexOf('?');
		if (queryParametersIndex == -1) {
			queryParametersIndex = requestPath.length();
		}
		int extensionIndex = requestPath.lastIndexOf('.', queryParametersIndex);
		if (extensionIndex == -1) {
			extensionIndex = queryParametersIndex;
		}
		return requestPath.substring(0, extensionIndex);
	}
}
