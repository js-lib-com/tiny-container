package js.tiny.container.rest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.AuthorizationException;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.annotation.RequestPath;
import js.tiny.container.http.ContentType;
import js.tiny.container.http.Resource;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.http.encoder.ValueWriter;
import js.tiny.container.http.encoder.ValueWriterFactory;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.RequestPreprocessor;
import js.util.Types;

/**
 * Servlet for services invoked by REST requests. This REST servlet is enacted for web services; it is not usable for dynamic
 * resources. REST servlet actually invokes a REST method that executes service logic and may return a value. If present, value
 * is serialized back to client.
 * <p>
 * In order to enable this servlet it should be declared into deployment descriptor. REST servlet is always mapped by path, most
 * common <code>rest</code> prefix; it never uses extensions.
 * 
 * <pre>
 * 	&lt;servlet&gt;
 * 		&lt;servlet-name&gt;rest-servlet&lt;/servlet-name&gt;
 * 		&lt;servlet-class&gt;js.rest.RestServlet&lt;/servlet-class&gt;
 * 		&lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * 	&lt;/servlet&gt;
 * 	...
 * 	&lt;servlet-mapping&gt;
 * 		&lt;servlet-name&gt;rest-servlet&lt;/servlet-name&gt;
 * 		&lt;url-pattern&gt;/rest/*&lt;/url-pattern&gt;
 * 	&lt;/servlet-mapping&gt;
 * </pre>
 * 
 * <h3>Request Routing</h3>
 * <p>
 * In order to execute service logic, the REST method should be located. Since servlet mapping always uses map by path, this
 * servlet uses {@link HttpServletRequest#getPathInfo()} to locate REST method. Path info syntax is standard:
 * 
 * <pre>
 * path-info = ["/" resource] "/" sub-resource ["?" query-string]
 * </pre>
 * 
 * Resource part identify method declaring class and is optional, in which case default class is used. In this context default
 * class means one without request path. Sub-resource identifies managed method by its request path,
 * {@link ManagedMethodSPI#getRequestPath()}. Note that if managed method is not annotated with {@link RequestPath} it request
 * path is method name converted to dashed case.
 * <p>
 * In order to locate REST method this servlet keeps a cache of methods, {@link #restMethods} initialized eagerly by
 * {@link #init(ServletConfig)}. It maps path info to REST method. When a request is processed a quick lookup identifies the
 * method.
 * 
 * <pre>
 * Class 0        Class 1                 Class i                 REST Methods Mapper
 * +-----------+  +-----------+           +-----------+           +--------------------+
 * | method 00 |  | method 10 |    ...    | method i0 |           | key 00 : method 00 |
 * | method 01 |  | method 11 |           | method i1 |           | key 01 : method 01 | 
 * | ...       |  | ...       |           | ...       |           | ...                | 
 * | method 0j |  | method 1k |           | method il |           | key 0j : method 0j |
 * +-----|-----+  +-----|-----+           +-----|-----+           | key 10 : method 10 |
 *       |              |                       +---------------&gt; | key 11 : method 11 |
 *       |              +---------------------------------------&gt; | ...                |
 *       +------------------------------------------------------&gt; | key 1k : method 1k |
 *              Servlet                                           | key i0 : method i0 |
 *              +-----------------------+                         | key i1 : method i1 |
 *              |  +-------------+      |           +-----+       | ...                |
 *              |  | pathInfo    +------------------&gt; key +-----&gt; | key il : method il |
 *              |  +-------------+      |           +-----+       +----------|---------+
 *              |                       |                                    | 
 *              |  +---------------+    |                                    |
 *              |  | method.invoke | &lt;---------------------------------------+
 *              |  +---------------+    |
 *              +-----------------------+
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version experimental
 */
public class RestServlet extends AppServlet {
	/** Java serialization version. */
	private static final long serialVersionUID = 1970024026367020016L;

	/** Class logger. */
	private static final Log log = LogFactory.getLog(RestServlet.class);

	/**
	 * Factory for invocation arguments readers. Create instances to read invocation arguments from HTTP request, accordingly
	 * request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	/** Factory for return value writers. Create instances to serialize method return value to HTTP response. */
	private final ValueWriterFactory valueWriterFactory;

	/**
	 * REST methods mapped to request paths. A REST method is a remotely accessible method that does not return a
	 * {@link Resource}. This cache is eagerly loaded at this servlet initialization.
	 */
	private final Map<String, ManagedMethodSPI> restMethods = new HashMap<>();

	/** Initialize invocation arguments reader and return value writer factories. */
	public RestServlet() {
		log.trace("RestServlet()");
		// both factories are implemented by the same server encoders
		argumentsReaderFactory = ServerEncoders.getInstance();
		valueWriterFactory = ServerEncoders.getInstance();
	}

	/**
	 * Performed super-initialization by {@link AppServlet#init(ServletConfig)} and loads REST methods cache. A REST method is a
	 * remotely accessible method that does not return a {@link Resource}. Map storage key is generated by
	 * {@link #key(ManagedMethodSPI)} based on managed class and REST method request paths and is paired with retrieval key -
	 * {@link #key(String)}, generated from request path info when method invocation occurs.
	 * 
	 * @param config servlet configuration object.
	 * @throws UnavailableException if tiny container is not properly initialized.
	 * @throws ServletException if servlet initialization fails.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		for (ManagedMethodSPI managedMethod : container.getManagedMethods()) {
			if (!managedMethod.isRemotelyAccessible()) {
				continue;
			}
			if (!Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				restMethods.put(key(managedMethod), managedMethod);
			}
		}
	}

	/**
	 * Handle request for a REST resource. Locate REST method based on request path info, execute method and serialize back to
	 * client returned value, if any. See class description for general behavior.
	 * 
	 * @param context request context.
	 * @throws IOException if reading from HTTP request or writing to HTTP response fail.
	 */
	@Override
	protected void handleRequest(RequestContext context) throws IOException {
		HttpServletRequest httpRequest = context.getRequest();
		HttpServletResponse httpResponse = context.getResponse();

		ArgumentsReader argumentsReader = null;
		Object value = null;
		ManagedMethodSPI method = null;

		try {
			method = restMethods.get(key(httpRequest.getPathInfo()));
			if (method == null) {
				throw new NoSuchMethodException();
			}

			Type[] formalParameters = method.getParameterTypes();
			argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
			Object[] arguments = argumentsReader.read(httpRequest, formalParameters);

			Object instance = container.getInstance(method.getDeclaringClass());
			value = method.invoke(instance, arguments);
		} catch (AuthorizationException e) {
			sendUnauthorized(context);
			return;
		} catch (NoSuchMethodException e) {
			sendNotFound(context, e);
			return;
		} catch (IllegalArgumentException e) {
			// there are opinions that 422 UNPROCESSABLE ENTITY is more appropriate response
			// see https://httpstatuses.com/422
			sendBadRequest(context);
			return;
		} catch (InvocationException e) {
			sendError(context, e);
			return;
		} finally {
			if (argumentsReader != null) {
				argumentsReader.clean();
			}
		}

		// because character encoding is explicitly set, Tomcat force charset attribute on Content-Type header
		// so that application/json becomes on response header application/json;charset=UTF-8
		//
		// Excerpt from Servlet Specification:
		// Calls to setContentType set the character encoding only if the given content type string provides a value for the
		// charset attribute.
		//
		// seems like Tomcat breaks the specs

		httpResponse.setCharacterEncoding("UTF-8");

		if (method.isVoid()) {
			// expected servlet container behavior:
			// since there is nothing written to respond to output stream, container either set content length to zero
			// or closes connection signaling end of content
			httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		// expected servlet container behavior:
		// if client request connection header is close container uses an internal buffer for serialized JSON, add content
		// length response header based on buffer size and closes connection
		// if client request connection header is not explicitly set to close, container uses an internal buffer for serialized
		// JSON but with limited capacity; if capacity is not exceeded set response content length; if capacity is exceeded
		// switch to chunked transfer

		ContentType contentType = method.getReturnContentType();
		if (contentType == null) {
			contentType = valueWriterFactory.getContentTypeForValue(value);
		}
		httpResponse.setStatus(HttpServletResponse.SC_OK);
		httpResponse.setContentType(contentType.getValue());

		ValueWriter valueWriter = valueWriterFactory.getValueWriter(contentType);
		valueWriter.write(httpResponse, value);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Generate storage key for REST methods cache. This key is create from declaring class and managed method request paths and
	 * is used on cache initialization. It is paired with {@link #key(String)} created from request path on actual method
	 * invocation.
	 * <p>
	 * Here is storage key syntax that should be identical with retrieval key. Key has optional resource path and sub-resource
	 * path. Resource path is the declaring class request path, {@link ManagedClassSPI#getRequestPath()} and sub-resource path
	 * is managed method request path, {@link ManagedMethodSPI#getRequestPath()}.
	 * 
	 * <pre>
	 * key = ["/" resource ] "/" sub-resource
	 * resource = declaring class request path
	 * sub-resource = managed method request path
	 * </pre>
	 * 
	 * @param restMethod REST method.
	 * @return REST method key.
	 */
	private static String key(ManagedMethodSPI restMethod) {
		StringBuilder key = new StringBuilder();
		if (restMethod.getDeclaringClass().getRequestPath() != null) {
			key.append('/');
			key.append(restMethod.getDeclaringClass().getRequestPath());
		}
		key.append('/');
		key.append(restMethod.getRequestPath());
		return key.toString();
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
	private static String key(String requestPath) {
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
