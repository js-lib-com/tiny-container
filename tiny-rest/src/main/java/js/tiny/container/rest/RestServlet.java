package js.tiny.container.rest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;
import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.ContentType;
import js.tiny.container.http.HttpHeader;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.http.encoder.ValueWriter;
import js.tiny.container.http.encoder.ValueWriterFactory;
import js.tiny.container.rest.PathTree.Item;
import js.tiny.container.rest.sse.SseEventSinkImpl;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IManagedParameter;
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

	private static final Object[] EMPTY_ARGUMENTS = new Object[0];

	private final Converter converter = ConverterRegistry.getConverter();

	/**
	 * Factory for invocation arguments readers. Create instances to read invocation arguments from HTTP request, accordingly
	 * request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	/** Factory for return value writers. Create instances to serialize method return value to HTTP response. */
	private final ValueWriterFactory valueWriterFactory;

	private PathMethodsCache cache;

	@Inject
	public RestServlet() {
		log.trace("RestServlet()");
		// both factories are implemented by the same server encoders instance
		this.argumentsReaderFactory = ServerEncoders.getInstance();
		this.valueWriterFactory = ServerEncoders.getInstance();
	}

	public RestServlet(ServerEncoders encoders) {
		log.trace("RestServlet(ServerEncoders)");
		this.argumentsReaderFactory = encoders;
		this.valueWriterFactory = encoders;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		cache = container.getInstance(PathMethodsCache.class);
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
			String pathInfo = httpRequest.getPathInfo();
			if (pathInfo == null) {
				pathInfo = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
			}
			PathTree.Item<IManagedMethod> requestPath = cache.get(httpRequest.getMethod(), pathInfo);
			managedMethod = requestPath.getValue();
			if (managedMethod == null) {
				throw new NoSuchMethodException();
			}

			Object[] arguments = getArguments(httpRequest, requestPath);
			if (arguments == null) {
				Type[] formalParameters = managedMethod.getParameterTypes();
				argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
				arguments = argumentsReader.read(httpRequest, formalParameters);
			}

			Object instance = managedMethod.getDeclaringClass().getInstance();
			value = managedMethod.invoke(instance, arguments);

			if (isSseRequest(managedMethod)) {
				if (!Types.isVoid(managedMethod.getReturnType())) {
					throw new IllegalStateException("Non void SSE resource method: " + managedMethod);
				}
				handleSseRequest(httpRequest);
				return;
			}

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

		IProduces producesMeta = IProduces.scan(managedMethod);
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

	private Object[] getArguments(HttpServletRequest httpRequest, Item<IManagedMethod> requestPath) throws IOException {
		IManagedMethod managedMethod = requestPath.getValue();
		List<IManagedParameter> managedParameters = managedMethod.getManagedParameters();
		if (managedParameters.isEmpty()) {
			return EMPTY_ARGUMENTS;
		}

		UrlParameters urlParameters = new UrlParameters(httpRequest);
		int entityParametersCount = 0;
		Object[] arguments = new Object[managedParameters.size()];
		for (int argumentIndex = 0, pathVariableIndex = 0; argumentIndex < arguments.length; ++argumentIndex) {
			IManagedParameter managedParameter = managedParameters.get(argumentIndex);
			Class<?> parameterType = (Class<?>) managedParameter.getType();

			Annotation[] annotations = managedParameter.getAnnotations();
			if (annotations.length == 0) {
				if (entityParametersCount++ > 0) {
					log.error("Invalid resource method arguments: multiple entity parameters.");
					return null;
				}

				Type[] formalParameters = new Type[] { parameterType };
				ArgumentsReader argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
				Object[] entityArgument = argumentsReader.read(httpRequest, formalParameters);
				arguments[argumentIndex] = entityArgument.length == 1 ? entityArgument[0] : null;
				continue;
			}

			Annotation annotation = annotations[0];

			if (IContext.cast(annotation) != null) {
				arguments[argumentIndex] = container.getInstance(parameterType);
			}

			IPathParam pathParam = IPathParam.cast(annotation);
			if (pathParam != null) {
				// this logic assumes request path variables order is the same as related formal parameters
				// therefore there is no the need to search variable by name
				arguments[argumentIndex] = requestPath.getVariableValue(pathVariableIndex++, parameterType);
			}

			IQueryParam queryParam = IQueryParam.cast(annotation);
			if (queryParam != null) {
				String queryName = queryParam.value();
				arguments[argumentIndex] = converter.asObject(urlParameters.getParameter(queryName), parameterType);
			}

			IMatrixParam matrixParam = IMatrixParam.cast(annotation);
			if (matrixParam != null) {
				String matrixName = matrixParam.value();
				arguments[argumentIndex] = converter.asObject(urlParameters.getParameter(matrixName), parameterType);
			}

			IHeaderParam headerParam = IHeaderParam.cast(annotation);
			if (headerParam != null) {
				String headerName = headerParam.value();
				arguments[argumentIndex] = converter.asObject(httpRequest.getHeader(headerName), parameterType);
			}

		}

		return arguments;
	}

	protected void handleSseRequest(HttpServletRequest httpRequest) throws Exception {
		if (!httpRequest.isAsyncSupported()) {
			throw new IllegalStateException("REST SSE requires asynchronous mode. Missing <async-supported>true</async-supported> ?");
		}

		AsyncContext asyncContext = httpRequest.startAsync();
		asyncContext.setTimeout(0);

		// since event sink is bound with request scope next instance is the same as that injected on SSE method arguments
		SseEventSinkImpl eventSink = (SseEventSinkImpl) container.getInstance(SseEventSink.class);
		eventSink.setAsyncContext(asyncContext);

		HttpServletResponse httpResponse = (HttpServletResponse) asyncContext.getResponse();
		httpResponse.setContentType("text/event-stream;charset=UTF-8");
		// no need to explicitly set character encoding since is already set by content type
		// httpResponse.setCharacterEncoding("UTF-8");

		httpResponse.setHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_CACHE);
		httpResponse.addHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_STORE);
		httpResponse.setHeader(HttpHeader.PRAGMA, HttpHeader.NO_CACHE);
		httpResponse.setDateHeader(HttpHeader.EXPIRES, 0);
		httpResponse.setHeader(HttpHeader.CONNECTION, HttpHeader.KEEP_ALIVE);

		eventSink.setWriter(httpResponse.getWriter());
	}

	/**
	 * Detect if current request is for a SSE method. Accordingly JAX-RS SEE spec: a resource method that injects an
	 * SseEventSink and produces the media type text/event-stream is an SSE resource method.
	 * 
	 * @param managedMethod managed method pointed by current HTTP request.
	 * @return true if current request is for a SSE method.
	 */
	private boolean isSseRequest(IManagedMethod managedMethod) {
		IProduces producesMeta = IProduces.scan(managedMethod);
		if (producesMeta == null) {
			return false;
		}
		if (producesMeta.value().length != 1) {
			return false;
		}
		if (!MediaType.SERVER_SENT_EVENTS.equalsIgnoreCase(producesMeta.value()[0])) {
			return false;
		}

		for (IManagedParameter managedParameter : managedMethod.getManagedParameters()) {
			if (IContext.scan(managedParameter) && SseEventSink.class.equals(managedParameter.getType())) {
				return true;
			}
		}
		return false;
	}
}
