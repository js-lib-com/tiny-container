package js.tiny.container.mvc;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.NoSuchResourceException;
import js.tiny.container.http.Resource;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.RequestPreprocessor;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Strings;
import js.util.Types;

/**
 * Servlet implementation for resources generated dynamically, mostly views based on templates. Usually resources servlet is
 * enacted for web resources requested by links and forms. This servlet actually invokes controller method that returns the
 * {@link Resource}, then serialize that resource back to client.
 * <p>
 * In order to enable this servlet it should be declared into deployment descriptor. Usually resources servlet is mapped to
 * <code>.xsp</code> extension but there is no formal requirements for that. XSP name stands for X(HT)ML Server Pages and refers
 * to server generated pages from X(HT)ML templates.
 * 
 * <pre>
 * 	&lt;servlet&gt;
 * 		&lt;servlet-name&gt;xsp-servlet&lt;/servlet-name&gt;
 * 		&lt;servlet-class&gt;js.mvc.ResourceServlet&lt;/servlet-class&gt;
 * 		&lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * 	&lt;/servlet&gt;
 * 	...
 * 	&lt;servlet-mapping&gt;
 * 		&lt;servlet-name&gt;xsp-servlet&lt;/servlet-name&gt;
 * 		&lt;url-pattern&gt;*.xsp&lt;/url-pattern&gt;
 * 	&lt;/servlet-mapping&gt;
 * </pre>
 * <p>
 * Request path for resource retrieval has two major parts: optional controller and resource paths. Extension is ignored but
 * query string is processed. A controller is a managed class annotated with {@link Controller} and <code>controller</code> from
 * request path is {@link IManagedClass#getRequestPath()}. A resource is a managed method from a controller that returns a
 * {@link Resource} and <code>resource</code> from request path is {@link IManagedMethod#getRequestPath()}.
 * 
 * <pre>
 * request-path = ["/" controller] "/" resource ["." extension] ["?" query-string]
 * controller = &lt; managed class request path &gt;
 * resource = &lt; managed method request path &gt;
 * </pre>
 * 
 * This servlet keeps a pool of resource methods mapped to request path, {@link #resourceMethods}. It is eagerly loaded at this
 * servlet initialization. Request handler method, {@link #handleRequest(RequestContext)} gets a request context that has
 * request path prepared, {@link RequestContext#getRequestPath()} that is used to route request to resource method. Once method
 * identified its arguments are deserialized from HTTP request. If arguments does not match method signature, resource servlet
 * responds with method not found with status code 404. If method execution fails, sends server error -
 * {@link HttpServletResponse#sendError(int, String)} with status code 500.
 * <p>
 * If resource is private and HTTP request is not part of an authorized context this servlet redirect to login page, if
 * container has one defined, see {@link IContainer#getLoginPage()}. If there is not login page performs servlet container
 * authentication via {@link HttpServletRequest#authenticate(HttpServletResponse)}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class ResourceServlet extends AppServlet {
	/** Java serialization version. */
	private static final long serialVersionUID = 1397044817485752955L;

	/** Class logger. */
	private static final Log log = LogFactory.getLog(ResourceServlet.class);

	/**
	 * Factory for invocation arguments readers. Create instances to read resource methods arguments from HTTP request,
	 * accordingly request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	/**
	 * Resource methods mapped to request paths. A resource method is a remotely accessible method that returns a
	 * {@link Resource}. This cache is eagerly loaded at this servlet initialization.
	 */
	private final Map<String, IManagedMethod> resourceMethods;

	/** Construct resources servlet instance. */
	@Inject
	public ResourceServlet() {
		log.trace("ResourcesServlet()");
		argumentsReaderFactory = ServerEncoders.getInstance();
		resourceMethods = new HashMap<>();
	}

	/**
	 * Test constructor.
	 * 
	 * @param argumentsReaderFactory mock arguments reader factory.
	 */
	public ResourceServlet(ArgumentsReaderFactory argumentsReaderFactory) {
		log.trace("ResourceServlet(ArgumentsReaderFactory)");
		this.argumentsReaderFactory = argumentsReaderFactory;
		resourceMethods = new HashMap<>();
	}

	/**
	 * Beside initialization performed by {@link AppServlet#init(ServletConfig)} this method takes care to load resource methods
	 * cache. A resource method is a remotely accessible method that returns a {@link Resource}. Map storage key is generated by
	 * {@link #key(IManagedMethod)} based on controller and resource method request paths and is paired with retrieval key -
	 * {@link #key(String)}, generated from request path when method invocation occurs.
	 * 
	 * @param config servlet configuration object.
	 * @throws UnavailableException if tiny container is not properly initialized.
	 * @throws ServletException if servlet initialization fails.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		for (IManagedMethod method : container.getManagedMethods()) {
			if (Types.isKindOf(method.getReturnType(), Resource.class)) {
				resourceMethods.put(key(method), method);
			}
		}
	}

	/**
	 * Handle request for a resource. Locate resource method based on request path from request context, execute method and
	 * serialize back to client returned resource. See class description for general behavior.
	 * 
	 * @param context request context.
	 * @throws ServletException if a servlet container service fails.
	 * @throws IOException if reading from HTTP request or writing to HTTP response fail.
	 */
	@Override
	protected void handleRequest(RequestContext context) throws ServletException, IOException {
		// for exceptions generated before response commit uses HttpServletResponse.sendError
		// on send error, servlet container prepares error response using container internal HTML for response body
		// if <error-page> is configured into deployment descriptor servlet container uses that HTML for response body

		final HttpServletRequest httpRequest = context.getRequest();
		final HttpServletResponse httpResponse = context.getResponse();

		ArgumentsReader argumentsReader = null;
		Resource resource = null;
		try {
			IManagedMethod method = resourceMethods.get(key(context.getRequestPath()));
			if (method == null) {
				throw new NoSuchMethodException(httpRequest.getRequestURI());
			}

			final Type[] formalParameters = method.getParameterTypes();
			argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
			Object[] arguments = argumentsReader.read(httpRequest, formalParameters);

			Object controller = container.getInstance(method.getDeclaringClass());
			resource = method.invoke(controller, arguments);
			if (resource == null) {
				throw new BugError("Null resource |%s|.", httpRequest.getRequestURI());
			}

			ResponseContentTypeMeta responseContentTypeMeta = method.getServiceMeta(ResponseContentTypeMeta.class);
			if (responseContentTypeMeta != null) {
				httpResponse.setContentType(responseContentTypeMeta.value());
			}
		} catch (AuthorizationException e) {
			// at this point, resource is private and need to redirect to a login page
			// if application provides one, tiny container is configured with, and use it
			// otherwise servlet container should have one
			// if no login page found send back servlet container error - that could be custom error page, if declared

			String loginPage = container.getLoginPage();
			if (loginPage != null) {
				httpResponse.sendRedirect(loginPage);
			} else {
				// expected servlet container behavior:
				// if <login-config> section exist into web.xml do what is configured there
				// else send back internal page with message about SC_UNAUTHORIZED

				// authenticate can throw ServletException if fails, perhaps because is not configured
				// let servlet container handle this error, that could be custom error page is configured
				httpRequest.authenticate(httpResponse);
			}
			return;
		} catch (NoSuchMethodException | IllegalArgumentException e) {
			// do not use AppServlet#sendError since it is encoded JSON and for resources need HTML
			dumpError(context, e);
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, httpRequest.getRequestURI());
			return;
		} catch (InvocationException e) {
			// do not use AppServlet#sendError since it is encoded JSON and for resources need HTML
			dumpError(context, e);
			if (e.getCause() instanceof NoSuchResourceException) {
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, httpRequest.getRequestURI());
			} else {
				httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getCause().getMessage());
			}
			return;
		} finally {
			if (argumentsReader != null) {
				argumentsReader.clean();
			}
		}

		// once serialization process started response becomes committed
		// and is not longer possible to use HttpServletResponse#sendError

		// let servlet container handle IO exceptions but since client already start rendering
		// there is no so much to do beside closing or reseting connection

		httpResponse.setStatus(HttpServletResponse.SC_OK);
		resource.serialize(httpResponse);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Generate storage key for resource methods cache. This key is create from controller and resource method request paths and
	 * is used on cache initialization. It is paired with {@link #key(String)} created from request path on actual method
	 * invocation.
	 * <p>
	 * Here is storage key syntax that should be identical with retrieval key. Key has optional controller path - missing if use
	 * default controller and resource method path. Controller path is the declaring class request path,
	 * {@link IManagedClass#getRequestPath()} and resource path is managed method request path,
	 * {@link IManagedMethod#getRequestPath()}.
	 * 
	 * <pre>
	 * key = ["/" controller-path ] "/" resource-path
	 * controller-path = declaring class request path
	 * resource-path = managed method request path
	 * </pre>
	 * 
	 * @param resourceMethod resource method.
	 * @return resource method key.
	 */
	private static String key(IManagedMethod resourceMethod) {
		StringBuilder key = new StringBuilder();
		String classPath = path(resourceMethod.getDeclaringClass());
		if (classPath != null) {
			key.append('/');
			key.append(classPath);
		}
		key.append('/');
		key.append(path(resourceMethod));
		return key.toString();
	}

	private static String path(IManagedClass managedClass) {
		ControllerMeta controllerMeta = managedClass.getServiceMeta(ControllerMeta.class);
		String value = controllerMeta != null ? controllerMeta.value() : null;
		return value != null && !value.isEmpty() ? value : null;
	}

	private static String path(IManagedMethod managedMethod) {
		RequestPathMeta requestPathMeta = managedMethod.getServiceMeta(RequestPathMeta.class);
		return requestPathMeta != null ? requestPathMeta.value() : Strings.memberToDashCase(managedMethod.getMethod().getName());
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
