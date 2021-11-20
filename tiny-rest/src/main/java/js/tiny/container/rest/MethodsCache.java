package js.tiny.container.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.RequestPreprocessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Strings;

public class MethodsCache {
	private static final Object mutex = new Object();
	private static MethodsCache instance;

	public static MethodsCache instance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new MethodsCache();
				}
			}
		}
		return instance;
	}

	private final Map<String, IManagedMethod> cache = new HashMap<>();

	public String add(IManagedMethod managedMethod) {
		String key = key(managedMethod);
		cache.put(key, managedMethod);
		return key;
	}

	public IManagedMethod get(String requestPath) {
		return cache.get(key(requestPath));
	}

	/**
	 * Generate storage key for REST methods cache. This key is create from declaring class and managed method request paths and
	 * is used on cache initialization. It is paired with {@link #key(String)} created from request path on actual method
	 * invocation.
	 * <p>
	 * Here is storage key syntax that should be identical with retrieval key. Key has optional resource path and sub-resource
	 * path. Resource path is the declaring class request path, {@link IManagedClass#getRequestPath()} and sub-resource path is
	 * managed method request path, {@link IManagedMethod#getRequestPath()}.
	 * 
	 * <pre>
	 * key = ["/" resource ] "/" sub-resource
	 * resource = declaring class request path
	 * sub-resource = managed method request path
	 * </pre>
	 * 
	 * @param managedMethod REST method.
	 * @return REST method key.
	 */
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
		return path(managedClass.getAnnotation(Path.class));
	}

	private static String path(IManagedMethod managedMethod) {
		return path(managedMethod.scanAnnotation(Path.class));
	}

	private static String path(Path path) {
		String value = path != null ? path.value() : null;
		if (value != null) {
			value = value.trim();
		}
		return value != null && !value.isEmpty() ? value : null;
	}

	/**
	 * Generate retrieval key for REST methods cache. This key is used by request routing logic to locate REST method about to
	 * invoke. It is based on request path extracted from request URI, see {@link RequestPreprocessor} and
	 * {@link RequestContext#getRequestPath()} - and should be identical with storage key.
	 * <p>
	 * Retrieval key syntax is identical with storage key but is based on request path, that on its turn is extracted from
	 * request URI. In fact this method just trim query parameters and extension, if any.
	 * 
	 * <pre>
	 * request-path = ["/" resource] "/" sub-resource ["?" query-string]
	 * key = ["/" resource ] "/" sub-resource
	 * resource = managed class request-path
	 * sub-resource = managed method request-path
	 * </pre>
	 * 
	 * @param requestPath request path identify REST resource to retrieve.
	 * @return REST method key.
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
