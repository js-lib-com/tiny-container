package js.tiny.container.rest;

import java.util.ArrayList;
import java.util.List;

import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.RequestPreprocessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Strings;

public class MethodsCache {
	// TODO: replace singleton pattern with javax.inject.Singleton
	private static final Object mutex = new Object();
	private static volatile MethodsCache instance;

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

	private final PathTree<IManagedMethod> cache = new PathTree<>();

	public List<String> add(IManagedMethod managedMethod) {
		List<String> key = key(managedMethod);
		cache.put(key, managedMethod);
		return key;
	}

	public PathTree.Item<IManagedMethod> get(String method, String requestPath) {
		return cache.get(paths(method, requestPath));
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
	static List<String> key(IManagedMethod managedMethod) {
		// scan all method annotations for first with meta-annotation @HttpMethod and gets its value
		String httpMethod = managedMethod.scanAnnotations(annotation -> {
			IHttpMethod httpMethodMeta = IHttpMethod.scan(annotation);
			return httpMethodMeta != null ? httpMethodMeta.value() : null;
		});
		if (httpMethod == null) {
			httpMethod = "GET";
		}

		// build path from class and / or method @Path annotation or from dashed method name
		StringBuilder path = new StringBuilder();
		String classPath = path(IPath.scan(managedMethod.getDeclaringClass()));
		if (classPath != null && !classPath.equals("/")) {
			path.append('/');
			path.append(classPath);
		}
		path.append('/');
		String methodPath = path(IPath.scan(managedMethod));
		if (methodPath == null) {
			methodPath = Strings.memberToDashCase(managedMethod.getName());
		}
		path.append(methodPath);

		return paths(httpMethod, path.toString());
	}

	private static String path(IPath path) {
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
	static List<String> key(String httpMethod, String requestPath) {
		int queryParametersIndex = requestPath.lastIndexOf('?');
		if (queryParametersIndex == -1) {
			queryParametersIndex = requestPath.length();
		}
		int extensionIndex = requestPath.lastIndexOf('.', queryParametersIndex);
		if (extensionIndex == -1) {
			extensionIndex = queryParametersIndex;
		}
		return paths(httpMethod, requestPath.substring(0, extensionIndex));
	}

	private static List<String> paths(String httpMethod, String path) {
		List<String> paths = new ArrayList<>();
		paths.add(httpMethod);
		paths.addAll(Strings.split(path, '/'));
		return paths;
	}
}
