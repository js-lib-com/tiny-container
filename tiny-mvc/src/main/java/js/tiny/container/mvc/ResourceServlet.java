package js.tiny.container.mvc;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.HttpHeader;
import js.tiny.container.http.NoSuchResourceException;
import js.tiny.container.http.Resource;
import js.tiny.container.http.encoder.ArgumentsReader;
import js.tiny.container.http.encoder.ArgumentsReaderFactory;
import js.tiny.container.http.encoder.ServerEncoders;
import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.mvc.annotation.ResponseContentType;
import js.tiny.container.servlet.AppServlet;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

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
 */
public class ResourceServlet extends AppServlet {
	private static final long serialVersionUID = 1397044817485752955L;

	private static final Log log = LogFactory.getLog(ResourceServlet.class);

	private final MethodsCache cache;

	/**
	 * Factory for invocation arguments readers. Create instances to read resource methods arguments from HTTP request,
	 * accordingly request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	@Inject
	public ResourceServlet() {
		log.trace("ResourcesServlet()");
		this.cache = MethodsCache.instance();
		this.argumentsReaderFactory = ServerEncoders.getInstance();
	}

	/**
	 * Test constructor.
	 * 
	 * @param cache mock methods cache,
	 * @param argumentsReaderFactory mock arguments reader factory.
	 */
	public ResourceServlet(MethodsCache cache, ArgumentsReaderFactory argumentsReaderFactory) {
		log.trace("ResourceServlet(ArgumentsReaderFactory)");
		this.cache = cache;
		this.argumentsReaderFactory = argumentsReaderFactory;
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
			IManagedMethod method = cache.get(context.getRequestPath());
			if (method == null) {
				throw new NoSuchMethodException(httpRequest.getRequestURI());
			}

			final Type[] formalParameters = method.getParameterTypes();
			argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
			Object[] arguments = argumentsReader.read(httpRequest, formalParameters);

			Object controller = container.getInstance(method.getDeclaringClass());
			resource = method.invoke(controller, arguments);
			if (resource == null) {
				throw new BugError("Null resource for request |%s| returned by method |%s|.", httpRequest.getRequestURI(), method);
			}

			ResponseContentType responseContentType = method.getAnnotation(ResponseContentType.class);
			if (responseContentType != null) {
				httpResponse.setContentType(responseContentType.value());
			}
		} catch (AuthorizationException e) {
			// at this point, resource is private and need to redirect to a login page
			// if application provides one, tiny container is configured with, and use it
			// otherwise servlet container should have one
			// if no login page found send back servlet container error - that could be custom error page, if declared

			String loginPage = container.getLoginPage();
			if (loginPage != null) {
				// XMLHttpRequest specs mandates that redirection codes to be performed transparently by user agent
				// this means redirect from server does not reach script counterpart
				// as workaround uses 200 OK and X-JSLIB-Location extension header
				if (HttpHeader.isXHR(context.getRequest())) {
					log.trace("Send X-JSLIB-Location |%s| for rejected XHR request: |%s|", container.getLoginPage(), context.getRequestURI());
					httpResponse.setStatus(HttpServletResponse.SC_OK);
					httpResponse.setHeader(HttpHeader.X_HEADER_LOCATION, container.getLoginPage());
					return;
				}

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
		} catch (Exception e) {
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
}
