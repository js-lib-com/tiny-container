package com.jslib.tiny.container.rmi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.tiny.container.http.ContentType;
import com.jslib.tiny.container.http.Resource;
import com.jslib.tiny.container.http.encoder.ArgumentsReader;
import com.jslib.tiny.container.http.encoder.ArgumentsReaderFactory;
import com.jslib.tiny.container.http.encoder.ServerEncoders;
import com.jslib.tiny.container.http.encoder.ValueWriter;
import com.jslib.tiny.container.http.encoder.ValueWriterFactory;
import com.jslib.tiny.container.servlet.AppServlet;
import com.jslib.tiny.container.servlet.RequestContext;
import com.jslib.tiny.container.spi.AuthorizationException;
import com.jslib.tiny.container.spi.IContainer;
import com.jslib.tiny.container.spi.IManagedClass;
import com.jslib.tiny.container.spi.IManagedMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.jslib.util.Classes;
import com.jslib.util.Types;

/**
 * Application servlet implementation for HTTP-RMI requests. This servlet invokes remotely accessible methods for services
 * requested by external clients, including XHR requests from browsers.
 * <p>
 * HTTP-RMI servlet should be declared into deployment descriptor. Usually HTTP-RMI servlet is mapped to <code>.rmi</code>
 * extension but there is no formal requirements for that. In fact request path pattern defines extension as optional.
 * 
 * <pre>
 * 	&lt;servlet&gt;
 * 		&lt;servlet-name&gt;rmi-servlet&lt;/servlet-name&gt;
 * 		&lt;servlet-class&gt;js.net.HttpRmiServlet&lt;/servlet-class&gt;
 * 		&lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * 	&lt;/servlet&gt;
 * 	...
 * 	&lt;servlet-mapping&gt;
 * 		&lt;servlet-name&gt;rmi-servlet&lt;/servlet-name&gt;
 * 		&lt;url-pattern&gt;*.rmi&lt;/url-pattern&gt;
 * 	&lt;/servlet-mapping&gt;
 * </pre>
 * <p>
 * Request path pattern for HTTP-RMI. One may notice that request path is a method qualified name using slash as separator.
 * Request path syntax supports extension and URL query parameters but this servlet does not use them.
 * 
 * <pre>
 *    request-path = "/" qualified-class "/" method ["." extension] ["?" query-parameters]
 *    
 *    qualified-class = package *("/" package) "/" class ["/" inner-class]
 *    package = LCHAR *CHAR
 *    class = UCHAR *CHAR
 *    inner-class = class
 *    method = LCHAR *CHAR
 *    
 *    extension = +CHAR
 *    
 *    query-parameters = [query-parameter ["&amp;" query-parameter]]
 *    query-parameter = query-name ["=" query-value]
 *    query-name = +CHAR
 *    query-value = +CHAR
 *    
 *    CHAR = &lt; any character less "/", ".", "?" &gt;
 *    LCHAR = &lt; lower case ASCII character [a-z] &gt;
 *    UCHAR = &lt; upper case ASCII character [A-Z] &gt;
 * </pre>
 * <p>
 * Execution exceptions, other that {@link IOException} are send back to client via
 * {@link AppServlet#sendError(RequestContext, Throwable)}, notable next ones:
 * <ul>
 * <li>ClassNotFoundException if request path syntax is invalid or if there is no remotely accessible managed class with
 * requested interface,
 * <li>NoSuchMethodException if requested method does not exist or is not remotely accessible,
 * <li>IllegalArgumentException argument encoded into HTTP request body does not match method formal parameters list.
 * </ul>
 * <p>
 * If method about be invoked is private and security context is not authorized this servlet uses
 * {@link AppServlet#sendUnauthorized(RequestContext)} to signal to client that authentication is required.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class HttpRmiServlet extends AppServlet {
	/** Java serialization version. */
	private static final long serialVersionUID = -7483018112800298638L;

	/** Class logger. */
	private static final Log log = LogFactory.getLog(HttpRmiServlet.class);

	/** Request path pattern for HTTP-RMI. See class description for supported syntax. */
	private static final Pattern REQUEST_PATH_PATTERN = Pattern.compile("^" + //
			"(\\/[a-z][a-z0-9]*(?:\\/[a-z][a-z0-9]*)*(?:\\/[A-Z][a-zA-Z0-9_]*)+)" + // qualified (inner)class name
			"\\/" + // path separator
			"([a-z][a-zA-Z0-9_]*)" + // method name
			"(?:\\..+)?" + // extension
			"(?:\\?(?:[^=]+=[^=]+)(?:&[^=]+=[^=]+)*)?" + // query parameters
			"$");

	/**
	 * Factory for invocation arguments readers. Create instances to read invocation arguments from HTTP request, accordingly
	 * request content type.
	 */
	private final ArgumentsReaderFactory argumentsReaderFactory;

	/** Factory for return value writers. Create instances to serialize method return value to HTTP response. */
	private final ValueWriterFactory valueWriterFactory;

	/** Initialize invocation arguments reader and return value writer factories. */
	public HttpRmiServlet() {
		log.trace("HttpRmiServlet()");
		// both factories are implemented by the same server encoders
		argumentsReaderFactory = ServerEncoders.getInstance();
		valueWriterFactory = ServerEncoders.getInstance();
	}

	/**
	 * HTTP-RMI request service. This is the implementation of the
	 * {@link AppServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)} abstract method. It locates
	 * remote managed method addressed by request path, deserialize actual parameters from HTTP request, reflexively invoke the
	 * method and serialize method returned value to HTTP response.
	 * <p>
	 * Actual parameters are transported into HTTP request body and are encoded accordingly <code>Content-Type</code> request
	 * header; for supported parameters encodings please see
	 * {@link ServerEncoders#getArgumentsReader(HttpServletRequest, Type[])} factory method. On its side, returned value is
	 * transported into HTTP response body too and is encoded accordingly its type - see
	 * {@link ServerEncoders#getValueWriter(ContentType)} for supported encodings for returned value.
	 * 
	 * @param context HTTP request context.
	 * @throws IOException if HTTP request reading fails for any reason.
	 */
	@Override
	public void handleRequest(RequestContext context) throws IOException {
		HttpServletRequest httpRequest = context.getRequest();
		HttpServletResponse httpResponse = context.getResponse();

		Matcher matcher = REQUEST_PATH_PATTERN.matcher(context.getRequestPath());
		if (!matcher.find()) {
			sendBadRequest(context);
			return;
		}
		String interfaceName = className(matcher.group(1));
		String methodName = matcher.group(2);

		IManagedMethod managedMethod = null;
		ArgumentsReader argumentsReader = null;
		Object value = null;

		try {
			IManagedClass<?> managedClass = getManagedClass(container, interfaceName, httpRequest.getRequestURI());
			managedMethod = getManagedMethod(managedClass, methodName, httpRequest.getRequestURI());

			final Type[] formalParameters = managedMethod.getParameterTypes();
			argumentsReader = argumentsReaderFactory.getArgumentsReader(httpRequest, formalParameters);
			Object[] arguments = argumentsReader.read(httpRequest, formalParameters);

			Object instance = managedClass.getInstance();
			value = managedMethod.invoke(instance, arguments);
		} catch (AuthorizationException e) {
			sendUnauthorized(context);
			return;
		} catch (Throwable t) {
			// all exception, including class not found, no such method and illegal argument are send back to client as they are
			sendError(context, t);
			return;
		} finally {
			if (argumentsReader != null) {
				argumentsReader.clean();
			}
		}

		httpResponse.setCharacterEncoding("UTF-8");
		if (Types.isVoid(managedMethod.getReturnType())) {
			httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		ContentType contentType = valueWriterFactory.getContentTypeForValue(value);
		httpResponse.setStatus(HttpServletResponse.SC_OK);
		httpResponse.setContentType(contentType.getValue());

		ValueWriter valueWriter = valueWriterFactory.getValueWriter(contentType);
		valueWriter.write(httpResponse, value);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Get remotely accessible managed class registered to a certain interface class.
	 * 
	 * @param container parent container,
	 * @param interfaceName name of the interface class identifying requested managed class,
	 * @param requestURI request URI for logging.
	 * @return managed class, never null.
	 * @throws ClassNotFoundException if interface class not found on run-time class path, managed class not defined or is not
	 *             remotely accessible.
	 */
	private static IManagedClass<?> getManagedClass(IContainer container, String interfaceName, String requestURI) throws ClassNotFoundException {
		Class<?> interfaceClass = Classes.forOptionalName(interfaceName);
		if (interfaceClass == null) {
			log.error("HTTP-RMI request for not existing class |%s|.", interfaceName);
			throw new ClassNotFoundException(requestURI);
		}
		IManagedClass<?> managedClass = container.getManagedClass(interfaceClass);
		if (managedClass == null) {
			log.error("HTTP-RMI request for not existing managed class |%s|.", interfaceName);
			throw new ClassNotFoundException(requestURI);
		}
		return managedClass;
	}

	/**
	 * Get managed method that is remotely accessible and has requested name.
	 * 
	 * @param managedClass managed class where method is supposed to be declared,
	 * @param methodName method name,
	 * @param requestURI request URI for logging.
	 * @return managed method, never null.
	 * @throws NoSuchMethodException if managed class has not any method with requested name, managed method was found but is
	 *             not remotely accessible or it returns a {@link Resource}.
	 */
	private static IManagedMethod getManagedMethod(IManagedClass<?> managedClass, String methodName, String requestURI) throws NoSuchMethodException {
		IManagedMethod managedMethod = managedClass.getManagedMethod(methodName);
		if (managedMethod == null) {
			log.error("HTTP-RMI request for not existing managed method |%s#%s|.", managedClass, methodName);
			throw new NoSuchMethodException(requestURI);
		}
		if (Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
			log.error("HTTP-RMI request for managed method |%s#%s| returning a resource.", managedClass, methodName);
			throw new NoSuchMethodException(requestURI);
		}
		return managedMethod;
	}

	/**
	 * Extract qualified class name, including inner classes, from class path part of the request path. Class path parameter is
	 * the class name but with slash as separator, e.g. <code>/sixqs/site/controller/ParticipantController</code>. This helper
	 * handles inner classes using standard notation with '$' separator; for example
	 * <code>/js/test/net/RmiController/Query</code> is converted to <code>js.test.net.RmiController$Query</code>.
	 * <p>
	 * Class nesting is not restricted to a single level. For example <code>/js/test/net/RmiController/Query/Item</code> is
	 * converted to <code>js.test.net.RmiController$Query$Item</code>.
	 * <p>
	 * It is expected that class path argument to be well formed. If class path argument is not valid this method behavior is
	 * not defined.
	 * 
	 * @param classPath qualified class name using slash as separator and with leading path separator.
	 * @return qualified class name using dollar notation for inner classes.
	 */
	private static String className(String classPath) {
		StringBuilder className = new StringBuilder();
		char separator = '.';
		char c = classPath.charAt(1);
		// i = 1 to skip leading path separator
		for (int i = 1;;) {
			if (c == '/') {
				c = separator;
			}
			className.append(c);

			if (++i == classPath.length()) {
				break;
			}

			c = classPath.charAt(i);
			// after first class detected change separator to dollar since rest are inner classes
			// this solution copes well with not restricted inner level
			if (Character.isUpperCase(c)) {
				separator = '$';
			}
		}
		return className.toString();
	}
}
