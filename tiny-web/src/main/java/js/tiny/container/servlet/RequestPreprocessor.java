package js.tiny.container.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.ContainerSPI;
import js.util.Strings;

/**
 * Servlet filter for request pre-processing. This filter is a critical part of the request routing process and complements
 * servlet container implementation. A request for Tiny Container has mandatory context path, optional locale and security
 * domain, followed by servlet path and path information. This filter is executed before servlet container mapping and takes
 * care to remove locale and security domain. This way servlet mapping from deployment descriptor can focus only on servlet
 * path.
 * <p>
 * Locale is used only if application is built for multiple locales; locale is a two letter language code, ISO 3166-1. Security
 * domain is present if application uses roles based security provided by servlet container. This filter extracts locale and
 * security domain from current request URI and forward it.
 * 
 * <pre>
 * request-uri = "/" context-path ["/" locale] ["/" security-domain] "/" servlet-path ["/" path-info]
 * </pre>
 * 
 * Note that servlet container security is enacted before this filter execution and deployment descriptor should consider locale
 * when mapping <code>web-resource-collection</code> from <code>security-constrain</code>. See routing details on <a
 * href="/container/overview-summary.html#request-routing">Request Routing</a> from Tiny Container Overview.
 * <p>
 * Current request pre-processor version has two filter parameters: <code>locale</code> and <code>security-domain</code>. Both
 * are comma separated lists. As stated <code>locale</code> item should be two letter language code and
 * <code>security-domain</code> item should be path names used by declarative security constrain.
 * 
 * <pre>
 * &lt;filter&gt;
 * 	&lt;filter-name&gt;request-preprocessor&lt;/filter-name&gt;
 * 	&lt;filter-class&gt;js.servlet.RequestPreprocessor&lt;/filter-class&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;locale&lt;/param-name&gt;
 * 		&lt;param-value&gt;en,iw,ro&lt;/param-value&gt;
 * 	&lt;/init-param&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;security-domain&lt;/param-name&gt;
 * 		&lt;param-value&gt;admin,info&lt;/param-value&gt;
 * 	&lt;/init-param&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 * 	&lt;filter-name&gt;request-preprocessor&lt;/filter-name&gt;
 * 	&lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * 
 * It is considered a resource not found and rejected with 404 if a request URI contains a locale code that is not listed into
 * filter parameter. If <code>locale</code> filter parameter is not declared all locale codes that may be present into request
 * URI are passed unprocessed. If request URI contains a security domain that is not listed into <code>security-domain</code>
 * filter parameter, it is forwarded unprocessed. If request URI is for a static resource this filter forward request URI as it
 * is.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class RequestPreprocessor implements Filter {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(RequestPreprocessor.class);

	/**
	 * The name of <code>locale</code> filter parameter. Locale filter parameter contains a comma separated list of two letters
	 * language code, ISO 3166-1. It is loaded on {@link #locales} field. If application does not have multiple locales support
	 * this filter parameter is not defined.
	 */
	private static final String PARAM_LOCALE = "locale";

	/**
	 * The name of <code>security-domain</code> filter parameter. Security domain filter parameter contains a comma separated
	 * list of paths, as declared on <code>url-pattern</code> from <code>security-constrain</code>. If application does not use
	 * role base security provided by servlet container this filter parameter is not declared. This filter parameter is loaded
	 * into {@link #securityDomains} field.
	 */
	private static final String PARAM_SECURITY_DOMAIN = "security-domain";

	/** Application tiny container, service provider interface. */
	private ContainerSPI container;

	/** Locale list loaded from <code>locale</code> filter parameter, default to empty list if filter parameter is not declared. */
	private List<String> locales = Collections.emptyList();

	/**
	 * Security domains list loaded from <code>security-domain</code> filter parameter, default to empty list if filter
	 * parameter is not declared.
	 */
	private List<String> securityDomains = Collections.emptyList();

	/**
	 * Load locale codes and security domains lists from filter parameters. Filter parameter is a list of command separated
	 * items. If related filter parameter is not declared, field is initialized to empty list.
	 * <p>
	 * This filter loads tiny container reference from servlet context attribute {@link TinyContainer#ATTR_INSTANCE}. If there
	 * is no servlet context attribute with the that name this initialization fails with filter permanently unavailable.
	 * 
	 * @param config filter configuration object.
	 * @throws UnavailableException if tiny container is not properly initialized.
	 */
	@Override
	public void init(FilterConfig config) throws UnavailableException {
		log.trace("init(FilterConfig)");

		// is safe to store container reference on filter instance since container has application life span
		container = (ContainerSPI) config.getServletContext().getAttribute(TinyContainer.ATTR_INSTANCE);
		if (container == null) {
			log.fatal("Tiny container instance not properly created, probably misconfigured. Request preprocessor permanently unvailable.");
			throw new UnavailableException("Tiny container instance not properly created, probably misconfigured.");
		}

		String localeParameter = config.getInitParameter(PARAM_LOCALE);
		if (localeParameter != null) {
			locales = Strings.split(localeParameter, ',');
			for (String locale : locales) {
				log.debug("Register locale |%s| for request pre-processing.", locale);
			}
		}

		String securityDomainParameter = config.getInitParameter(PARAM_SECURITY_DOMAIN);
		if (securityDomainParameter != null) {
			securityDomains = Strings.split(securityDomainParameter, ',');
			for (String securityDomain : securityDomains) {
				log.debug("Register security domain |%s| for request pre-processing.", securityDomain);
			}
		}
	}

	/** Destroy operation is required by filter interface but in request pre-processor implementation is NOP. */
	@Override
	public void destroy() {
		log.trace("destroy()");
	}

	/**
	 * Search request URI for locale and security domain, by comparing with internal lists, and remove found values. Update
	 * locale, security domain and request path on context request from current thread, see
	 * {@link RequestContext#setLocale(Locale)}, {@link RequestContext#setSecurityDomain(String)} and
	 * {@link RequestContext#setRequestPath(String)}.
	 * <p>
	 * Remove locale and security context from current request URI and forward it. If current request URI is for a static
	 * resource, that is, an existing file this filter does nothing.
	 * <p>
	 * It is considered a resource not found and rejected with 404 if a request URI contains a locale code that is not listed
	 * into filter parameter. If <code>locale</code> filter parameter is not declared all locale code that may be present into
	 * request URI are passed unprocessed. If request URI contains a security domain that is not listed into
	 * <code>security-domain</code> filter parameter, it is forwarded unprocessed.
	 * 
	 * @param request servlet request,
	 * @param response servlet response,
	 * @param chain filter chain.
	 * @throws IOException for failure on underlying request and response streams.
	 * @throws ServletException if filter chaining or request forwarding fails.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestURI = httpRequest.getRequestURI();
		String contextPath = httpRequest.getContextPath();
		String requestPath = requestURI.substring(contextPath.length());

		// request-uri = context-path request-path
		// context-path = "/" path-component
		// request-path = 1*("/" path-component)

		File file = new File(request.getServletContext().getRealPath(requestPath));
		if (file.exists()) {
			chain.doFilter(request, response);
			return;
		}

		// request-path = ["/" locale] ["/" security-domain] 1*("/" path-component)

		RequestContext context = container.getInstance(RequestContext.class);

		String queryString = httpRequest.getQueryString();
		context.setRequestURL(queryString != null ? Strings.concat(requestURI, '?', queryString) : requestURI);

		if (!locales.isEmpty()) {
			for (String locale : locales) {
				if (startsWith(requestPath, locale)) {
					requestPath = requestPath.substring(locale.length() + 1);
					context.setLocale(new Locale(locale));
					break;
				}
			}
		}

		for (String securityDomain : securityDomains) {
			if (startsWith(requestPath, securityDomain)) {
				requestPath = requestPath.substring(securityDomain.length() + 1);
				context.setSecurityDomain(securityDomain);
				break;
			}
		}

		context.setRequestPath(requestPath);
		request.getRequestDispatcher(requestPath).forward(request, response);
	}

	/**
	 * Test if request path starts with path component. This predicate returns true if first path component from given request
	 * path equals requested path component. Comparison is not case sensitive. Request path should start with path separator,
	 * otherwise this predicate returns false.
	 * 
	 * @param requestPath request path to test,
	 * @param pathComponent path component that is compared with first path component from request path.
	 * @return true if request path starts with path component.
	 */
	private static boolean startsWith(String requestPath, String pathComponent) {
		if (requestPath.charAt(0) != '/') {
			return false;
		}
		int i = 1;
		for (int j = 0; i < requestPath.length(); ++i, ++j) {
			if (requestPath.charAt(i) == '/') {
				return j == pathComponent.length();
			}
			if (j == pathComponent.length()) {
				return false;
			}
			if (Character.toLowerCase(requestPath.charAt(i)) != Character.toLowerCase(pathComponent.charAt(j))) {
				return false;
			}
		}
		return false;
	}
}
