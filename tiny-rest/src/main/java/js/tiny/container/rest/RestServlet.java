package js.tiny.container.rest;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.ContentType;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.http.encoder.ValueWriter;
import js.tiny.container.http.encoder.ValueWriterFactory;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedMethod;
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
 * {@link IManagedMethod#getRequestPath()}. Note that if managed method is not annotated with {@link Path} it request path is
 * method name converted to dashed case.
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
	private static final long serialVersionUID = 1970024026367020016L;

	private static final Log log = LogFactory.getLog(RestServlet.class);

	private final MethodsCache cache;

	/**
	 * Factory for invocation arguments readers. Create instances to read invocation arguments from HTTP request, accordingly
	 * request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	/** Factory for return value writers. Create instances to serialize method return value to HTTP response. */
	private final ValueWriterFactory valueWriterFactory;

	@Inject
	public RestServlet() {
		log.trace("RestServlet()");
		this.cache = MethodsCache.instance();
		// both factories are implemented by the same server encoders instance
		this.argumentsReaderFactory = ServerEncoders.getInstance();
		this.valueWriterFactory = ServerEncoders.getInstance();
	}

	public RestServlet(MethodsCache cache, ServerEncoders encoders) {
		log.trace("RestServlet(MethodsCache, ServerEncoders)");
		this.cache = cache;
		this.argumentsReaderFactory = encoders;
		this.valueWriterFactory = encoders;
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
		IManagedMethod managedMethod = null;

		try {
			managedMethod = cache.get(httpRequest.getPathInfo());
			if (managedMethod == null) {
				throw new NoSuchMethodException();
			}

			Type[] formalParameters = managedMethod.getParameterTypes();
			argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
			Object[] arguments = argumentsReader.read(httpRequest, formalParameters);

			Object instance = container.getInstance(managedMethod.getDeclaringClass());
			value = managedMethod.invoke(instance, arguments);
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
		} catch (Exception e) {
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

		if (Types.isVoid(managedMethod.getReturnType())) {
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

		Produces producesMeta = managedMethod.getAnnotation(Produces.class);
		String produces = producesMeta != null ? producesMeta.value().length > 0 ? producesMeta.value()[0] : null : null;

		ContentType contentType = ContentType.valueOf(produces);
		if (contentType == null) {
			contentType = valueWriterFactory.getContentTypeForValue(value);
		}
		httpResponse.setStatus(HttpServletResponse.SC_OK);
		httpResponse.setContentType(contentType.getValue());

		ValueWriter valueWriter = valueWriterFactory.getValueWriter(contentType);
		valueWriter.write(httpResponse, value);
	}
}
