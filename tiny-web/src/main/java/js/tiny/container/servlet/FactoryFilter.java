package js.tiny.container.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.Factory;
import js.tiny.container.spi.IContainer;

/**
 * Bind global factory {@link Factory} to current servlet request thread. This filter should be configured for third party
 * servlets as a mean to allow guest logic to access Tiny Container services, using standard delegation - see sample code for
 * JAX-WS web services.
 * <p>
 * Here is an example of this filter configuration for JAX-WS web services. Note that JAX-WS has its own logic to create
 * services instances and cannot be mixed with Tiny Container.
 * 
 * <pre>
 * <filter>
 * 	&lt;filter-name&gt;soap-filter&lt;/filter-name&gt;
 * 	&lt;filter-class&gt;js.tiny.container.servlet.FactoryFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * ...
 * &lt;filter-mapping&gt;
 * 	&lt;filter-name&gt;soap-filter&lt;/filter-name&gt;
 * 	&lt;url-pattern&gt;/soap/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * 
 * With this filter in place, JAX-WS web services can access Tiny Container instances via global factory.
 * 
 * <pre>
 * &#64;WebService(name = "ExpertService")
 * &#64;SOAPBinding(style = SOAPBinding.Style.RPC)
 * public class WebServiceImpl {
 * 	&#64;WebMethod
 * 	public int createIncident(@WebParam(name = "phone") String phone) {
 * 		return service().createIncident(phone);
 * 	}
 * 
 * 	private static ExpertService service() {
 * 		// ExpertService is a Tiny Container managed class that implement the actual service
 * 		// it has full access to all container services and is delegated by this JAX-WS web service
 * 		return Factory.getInstance(ExpertService.class);
 * 	}
 * }
 * </pre>
 * 
 * While, admittedly, this filter was created for JAX-WS integration there is no formal limitation and can be indeed used with
 * any third party servlet services.
 * 
 * @author Iulian Rotaru
 */
public class FactoryFilter implements Filter {
	private static final Log log = LogFactory.getLog(FactoryFilter.class);

	/**
	 * Container singleton reference. There is a single tiny container instance per servlet context so is safe to stored it in
	 * filter instance - that is also a single instance per servlet context.
	 */
	private IContainer container;

	@Override
	public void init(FilterConfig config) throws ServletException {
		final ServletContext context = config.getServletContext();
		log.trace("Initialize filter |%s#%s|.", context.getServletContextName(), config.getFilterName());
		container = (IContainer) context.getAttribute(TinyContainer.ATTR_INSTANCE);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		Factory.bind(container);
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		log.trace("destroy()");
	}
}
