package com.jslib.container.rest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.http.ContentType;
import com.jslib.container.http.HttpHeader;
import com.jslib.container.http.encoder.ArgumentsReader;
import com.jslib.container.http.encoder.ArgumentsReaderFactory;
import com.jslib.container.http.encoder.ServerEncoders;
import com.jslib.container.http.encoder.ValueWriter;
import com.jslib.container.http.encoder.ValueWriterFactory;
import com.jslib.container.rest.sse.SseEventSinkImpl;
import com.jslib.container.servlet.AppServlet;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.AuthorizationException;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IManagedMethod.Flags;
import com.jslib.container.spi.IManagedParameter;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.util.Types;

import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * Servlet for services invoked by REST requests. This servlet facilitates REST access to business services, based on JAX-RS
 * annotations. This servlet implements only a subset of the JSR 370 specification, but still usable in production.
 * 
 * <h3>Request Routing</h3>
 * 
 * REST requests must be routed to <code>com.jslib.container.rest.RestServlet</code> servlet. For this we need to declare the
 * REST servlet on application deployment descriptor, web.xml.
 * 
 * For REST servlet mapping we cannot use extension like *.rmi because REST protocol, by design, does not have file extensions.
 * The only option we left is path selector and usually it is /rest/*, meaning that everything after /rest/ is delivered to REST
 * servlet.
 * 
 * <pre>
 * 	&lt;servlet&gt;
 * 		&lt;servlet-name&gt;rest-servlet&lt;/servlet-name&gt;
 * 		&lt;servlet-class&gt;com.jslib.container.rest.RestServlet&lt;/servlet-class&gt;
 * 	&lt;/servlet&gt;
 * 	...
 * 	&lt;servlet-mapping&gt;
 * 		&lt;servlet-name&gt;rest-servlet&lt;/servlet-name&gt;
 * 		&lt;url-pattern&gt;/rest/*&lt;/url-pattern&gt;
 * 	&lt;/servlet-mapping&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
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
		cache = getContainer().getInstance(PathMethodsCache.class);
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
				// back door for non standard behavior, compatible with HTTP-RMI
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
				handleSseRequest(httpRequest, httpResponse);
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
		} catch (ParameterNotFoundException e) {
			sendNotFound(context, e);
			return;
		} catch (ParameterConversionException e) {
			// If the JAX-RS provider fails to convert a string into the Java type specified, it is considered a client error.
			// If this failure happens during the processing of an injection for an @MatrixParam, @QueryParam, or @PathParam, an
			// error status of 404 Not Found is sent back to the client. If the failure happens with @HeaderParam or
			// @CookieParam, an error response code of 400 Bad Request is sent.

			// @MatrixParam, @QueryParam and @PathParam are URL annotations
			if (e.getAnnotationType() == AnnotationType.URL) {
				sendNotFound(context, e);
			} else {
				sendBadRequest(context);
			}
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

		if ("cors".equals(httpRequest.getHeader("Sec-Fetch-Mode"))) {
			httpResponse.setHeader("Access-Control-Allow-Origin", "*");
		}

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

		Produces producesMeta = managedMethod.scanAnnotation(Produces.class, IManagedMethod.Flags.INCLUDE_TYPES);
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

	@SuppressWarnings("unchecked")
	private Object[] getArguments(HttpServletRequest httpRequest, PathTree.Item<IManagedMethod> requestPath) throws IOException, ParameterNotFoundException, ParameterConversionException {
		IManagedMethod managedMethod = requestPath.getValue();
		List<IManagedParameter> managedParameters = managedMethod.getManagedParameters();
		if (managedParameters.isEmpty()) {
			return EMPTY_ARGUMENTS;
		}

		Consumes consumesAnnotation = managedMethod.scanAnnotation(Consumes.class, Flags.INCLUDE_TYPES);
		if (consumesAnnotation != null) {
			String[] consumes = consumesAnnotation.value();
			if (consumes.length == 1 && MediaType.APPLICATION_FORM_URLENCODED.equals(consumes[0])) {
				if (managedParameters.size() != 1) {
					log.error("Current implementation for resource with URL encoded form supports only one parameter.");
					return null;
				}
				if (!Types.isKindOf(managedParameters.get(0).getType(), MultivaluedMap.class)) {
					log.error("Current implementation for resource with URL encoded form supports only multi-valued map.");
					return null;
				}

				Map<String, String> form = (Map<String, String>) getEntityArgument(httpRequest, Map.class);
				return new Object[] { new MultivaluedHashMap<>(form) };
			}
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
				arguments[argumentIndex] = getEntityArgument(httpRequest, parameterType);
				continue;
			}

			Annotation annotation = annotations[0];

			if (annotation instanceof Context) {
				arguments[argumentIndex] = getContainer().getInstance(parameterType);
			}

			if (annotation instanceof PathParam) {
				String name = ((PathParam) annotation).value();
				// this logic assumes request path variables order is the same as related formal parameters
				// therefore there is no the need to search variable by name
				arguments[argumentIndex] = convert(AnnotationType.URL, name, requestPath.getVariableValue(pathVariableIndex++), parameterType);
			}

			if (annotation instanceof QueryParam) {
				String name = ((QueryParam) annotation).value();
				arguments[argumentIndex] = convert(AnnotationType.URL, name, urlParameters.getParameter(name), parameterType);
			}

			if (annotation instanceof MatrixParam) {
				String name = ((MatrixParam) annotation).value();
				arguments[argumentIndex] = convert(AnnotationType.URL, name, urlParameters.getParameter(name), parameterType);
			}

			if (annotation instanceof HeaderParam) {
				String name = ((HeaderParam) annotation).value();
				arguments[argumentIndex] = convert(AnnotationType.REQUEST, name, httpRequest.getHeader(name), parameterType);
			}

			if (annotation instanceof CookieParam) {
				String name = ((CookieParam) annotation).value();
				String value = null;
				for (Cookie cookie : httpRequest.getCookies()) {
					if (cookie.getName().equalsIgnoreCase(name)) {
						value = cookie.getValue();
						break;
					}
				}
				arguments[argumentIndex] = convert(AnnotationType.REQUEST, name, value, parameterType);
			}
		}

		return arguments;
	}

	private Object convert(AnnotationType parameterType, String name, String value, Class<?> type) throws ParameterConversionException, ParameterNotFoundException {
		if (value == null) {
			throw new ParameterNotFoundException("Missing parameter %s", name);
		}
		try {
			return converter.asObject(value, type);
		} catch (Throwable e) {
			log.warn("Fail to convert value {} to parameter {} of type {}.", value, name, type);
			throw new ParameterConversionException(parameterType, "Fail to convert parameter %s", name);
		}
	}

	private Object getEntityArgument(HttpServletRequest httpRequest, Type parameterType) throws IOException {
		Type[] formalParameters = new Type[] { parameterType };
		ArgumentsReader argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
		Object[] entityArgument = argumentsReader.read(httpRequest, formalParameters);
		return entityArgument.length == 1 ? entityArgument[0] : null;
	}

	protected void handleSseRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
		if (!httpRequest.isAsyncSupported()) {
			throw new IllegalStateException("REST SSE requires asynchronous mode. Missing <async-supported>true</async-supported> ?");
		}

		AsyncContext asyncContext = httpRequest.startAsync(httpRequest, httpResponse);
		asyncContext.setTimeout(0);

		// since event sink is bound with request scope next instance is the same as that injected on SSE method arguments
		SseEventSinkImpl eventSink = (SseEventSinkImpl) getContainer().getInstance(SseEventSink.class);
		eventSink.setAsyncContext(asyncContext);

		httpResponse.setContentType("text/event-stream;charset=UTF-8");
		// no need to explicitly set character encoding since is already set by content type
		// httpResponse.setCharacterEncoding("UTF-8");

		httpResponse.setHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_CACHE);
		httpResponse.addHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_STORE);
		httpResponse.setHeader(HttpHeader.PRAGMA, HttpHeader.NO_CACHE);
		httpResponse.setDateHeader(HttpHeader.EXPIRES, 0);
		httpResponse.setHeader(HttpHeader.CONNECTION, HttpHeader.KEEP_ALIVE);
		httpResponse.setHeader("Access-Control-Allow-Origin", "*");

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
		Produces producesMeta = managedMethod.scanAnnotation(Produces.class, IManagedMethod.Flags.INCLUDE_TYPES);
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
			if (managedParameter.scanAnnotation(Context.class) != null && SseEventSink.class.equals(managedParameter.getType())) {
				return true;
			}
		}
		return false;
	}
}
