package com.jslib.container.mvc;

import java.io.IOException;
import java.lang.reflect.Type;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.http.HttpHeader;
import com.jslib.container.http.NoSuchResourceException;
import com.jslib.container.http.Resource;
import com.jslib.container.http.encoder.ArgumentsReader;
import com.jslib.container.http.encoder.ArgumentsReaderFactory;
import com.jslib.container.http.encoder.ServerEncoders;
import com.jslib.container.mvc.annotation.Controller;
import com.jslib.container.mvc.annotation.ResponseContentType;
import com.jslib.container.servlet.AppServlet;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.AuthorizationException;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.jslib.lang.BugError;

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
 * If resource is private and HTTP request is not part of an authorized context this servlet redirect to login page, if this
 * servlet has one defined. Servlet can declare a login page, relative to application context or absolute, using init parameter
 * <code>js.tiny.container.mvc.login</code>.
 * 
 * <pre>
 * 	&lt;servlet&gt;
 * 		&lt;servlet-name&gt;xsp-servlet&lt;/servlet-name&gt;
 * 		&lt;servlet-class&gt;js.mvc.ResourceServlet&lt;/servlet-class&gt;
 *		&lt;init-param&gt;
 *			&lt;param-name&gt;js.tiny.container.mvc.login&lt;/param-name&gt;
 *			&lt;param-value&gt;login.htm&lt;/param-value&gt;
 *		&lt;/init-param&gt;
 * 		&lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * 	&lt;/servlet&gt;
 * </pre>
 * 
 * If there is not login page performs servlet container authentication via
 * {@link HttpServletRequest#authenticate(HttpServletResponse)}.
 * 
 * @author Iulian Rotaru
 */
public class ResourceServlet extends AppServlet {
	private static final long serialVersionUID = 1397044817485752955L;

	private static final Log log = LogFactory.getLog(ResourceServlet.class);

	private static final String PARAMETER_MVC_LOGIN = "js.tiny.container.mvc.login";

	/**
	 * Factory for invocation arguments readers. Create instances to read resource methods arguments from HTTP request,
	 * accordingly request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	private MethodsCache cache;

	@Inject
	public ResourceServlet() {
		log.trace("ResourcesServlet()");
		this.argumentsReaderFactory = ServerEncoders.getInstance();
	}

	/**
	 * Test constructor.
	 * 
	 * @param cache mock methods cache,
	 * @param argumentsReaderFactory mock arguments reader factory.
	 */
	public ResourceServlet(ArgumentsReaderFactory argumentsReaderFactory) {
		log.trace("ResourceServlet(ArgumentsReaderFactory)");
		this.argumentsReaderFactory = argumentsReaderFactory;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		cache = container.getInstance(MethodsCache.class);
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

			resource = method.invoke(method.getDeclaringClass().getInstance(), arguments);
			if (resource == null) {
				throw new BugError("Null resource for request |%s| returned by method |%s|.", httpRequest.getRequestURI(), method);
			}

			ResponseContentType responseContentType = method.scanAnnotation(ResponseContentType.class);
			if (responseContentType != null) {
				resource.setContentType(responseContentType.value());
			}
		} catch (AuthorizationException e) {
			// at this point, resource is private and need to redirect to a login page
			// if application provides one, tiny container is configured with, and use it
			// otherwise servlet container should have one
			// if no login page found send back servlet container error - that could be custom error page, if declared

			String loginPage = httpRequest.getServletContext().getInitParameter(PARAMETER_MVC_LOGIN);
			if (loginPage != null) {
				// XMLHttpRequest specs mandates that redirection codes to be performed transparently by user agent
				// this means redirect from server does not reach script counterpart
				// as workaround uses 200 OK and X-JSLIB-Location extension header
				if (HttpHeader.isXHR(context.getRequest())) {
					log.trace("Send X-JSLIB-Location |{http_location}| for rejected XHR request: |{http_request}|", loginPage, context.getRequestURI());
					httpResponse.setStatus(HttpServletResponse.SC_OK);
					httpResponse.setHeader(HttpHeader.X_HEADER_LOCATION, loginPage);
					return;
				}

				httpResponse.sendRedirect(loginPage);
			} else {
				// expected servlet container behavior:
				// if <login-config> section exist into web.xml do what is configured there
				// else send back internal page with message about SC_UNAUTHORIZED

				// authenticate can throw ServletException if fails, perhaps because is not configured
				// let servlet container handle this error, that could be custom error page if configured
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
				String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
				httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
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
