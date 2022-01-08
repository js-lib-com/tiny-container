package js.tiny.container.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;

import javax.enterprise.context.ContextNotActiveException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.SecurityContext;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogContext;
import js.log.LogFactory;
import js.log.LogProvider;
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IClassBinding;
import js.tiny.container.core.Bootstrap;
import js.tiny.container.core.Container;
import js.tiny.container.net.EventStreamManager;
import js.tiny.container.net.EventStreamManagerImpl;
import js.tiny.container.spi.ISecurityContext;
import js.util.Classes;
import js.util.Strings;

/**
 * Container specialization for web applications. This class extends {@link Container} adding implementation for
 * {@link SessionScoped}, application context services and security context. Tiny container instance is accessible to
 * application code through {@link AppContext} interface.
 * <p>
 * This class also implements {@link SecurityContext} services. For servlet container authentication this class delegates HTTP
 * request related services. For application provided authentication this class uses HTTP session to handle {@link Principal}
 * supplied via {@link #login(Principal)}.
 * 
 * <h3>Servlet Container Integration</h3>
 * <p>
 * Servlet container creates and destroy tiny container instance via event listeners. This class implements both servlet context
 * and HTTP session event listeners and should be declared into application deployment descriptor.
 * 
 * <pre>
 * &lt;listener&gt;
 * 	&lt;listener-class&gt;js.servlet.TinyContainer&lt;/listener-class&gt;
 * &lt;/listener&gt;
 * </pre>
 * <p>
 * Event handlers manage tiny container life cycle. There is a single tiny container instance per web application created by
 * servlet container and initialized via {@link #contextInitialized(ServletContextEvent)} event handler. When web application is
 * unloaded tiny container is destroyed by {@link #contextDestroyed(ServletContextEvent)}. On first tiny container creation
 * takes care to initialize server global state. This class also track HTTP sessions creation and destroy for debugging.
 * 
 * <h3>Application Boostrap</h3> Here are overall steps performed by bootstrap logic. It is executed for every web application
 * deployed on server.
 * <ul>
 * <li>servlet container creates tiny container instance because is declared as listener on deployment descriptor,
 * <li>super-container and tiny container constructors are executed,
 * <li>servlet container invokes {@link #contextInitialized(ServletContextEvent)} on newly created tiny container instance,
 * <li>from now on logic is executed by above event handler:
 * <li>initialize {@link #contextParameters} from external descriptors,
 * <li>create {@link TinyConfigBuilder} that parses application descriptor,
 * <li>configure tiny container with created configuration object, see {@link #configure(Config)},
 * <li>bind tiny container instance to master factory,
 * <li>finalize tiny container creation by calling {@link #start()}.
 * </ul>
 * <p>
 * If tiny container is successfully configured and started, bootstrap logic stores its reference on servlet context attribute
 * {@link #ATTR_INSTANCE}. Anyway, if tiny container fails to start for some reason, dump stack trace and leave attribute null.
 * {@link AppServlet#init(javax.servlet.ServletConfig)} and {@link RequestPreprocessor#init(javax.servlet.FilterConfig)} test
 * tiny container attribute and if found null mark servlet as unavailable. This way a web application that fails to start tiny
 * container will have request preprocessor and all servlets unavailable and is not able to process any requests.
 * <p>
 * <b>Implementation note:</b> It is assumed that {@link #contextInitialized(ServletContextEvent)} is invoked before any servlet
 * initialization via {@link AppServlet#init(javax.servlet.ServletConfig)}. Servlet specification does provide explicit evidence
 * for this prerequisite. Anyway there is something that can lead to this conclusion on section 10.12, see below; also found
 * support on API-DOC. Here is the relevant excerpt: <em>All ServletContextListeners are notified of context initialization
 * before any filter or servlet in the web application is initialized.</em>
 * 
 * <p>
 * For completeness here are web application deployment steps, excerpt from servlet specification 3.0, section 10.12:
 * <ul>
 * <li>Instantiate an instance of each event listener identified by a <code>listener</code> element in the deployment
 * descriptor.
 * <li>For instantiated listener instances that implement ServletContextListener, call the contextInitialized() method.
 * <li>Instantiate an instance of each filter identified by a <code>filter</code> element in the deployment descriptor and call
 * each filter instance’s init() method.
 * <li>Instantiate an instance of each servlet identified by a <code>servlet</code> element that includes a
 * <code>load-on-startup</code> element in the order defined by the load-onstartup element values, and call each servlet
 * instance’s init() method.
 * </ul>
 * 
 * @author Iulian Rotaru
 */
public class TinyContainer extends Container implements ServletContextListener, HttpSessionListener, ServletRequestListener, ITinyContainer, SecurityContext {
	private static final Log log = LogFactory.getLog(TinyContainer.class);

	/** Container instance is stored on servlet context with this attribute name. */
	public static final String ATTR_INSTANCE = "js.tiny.container.instance";

	/** Session attribute name for principal storage when authentication is provided by application. */
	public static final String ATTR_PRINCIPAL = "js.tiny.container.principal";

	/** Name used for root context path. */
	public static final String ROOT_CONTEXT = "root";

	/** Diagnostic context name for context path, aka application. */
	private static final String LOG_CONTEXT_APP = "app";

	/** The name of web application that own this tiny container. */
	private String appName;

	/**
	 * Development context is running in the same JVM and is allowed to do cross context forward to this context private
	 * resources WITHOUT AUTHENTICATION. It should be explicitly configured on context parameters with the name
	 * <code>js.tiny.container.dev.context</code>.
	 */
	private String developmentContext;

	/**
	 * Location for application login page, null if not configured. This field value is loaded from application descriptor.
	 * Location can be relative or absolute to servlet container root, in which case starts with path separator. Container takes
	 * care to convert to absolute location if configured value is relative.
	 * 
	 * <p>
	 * Login page location is loaded from application descriptor, <code>login</code> section.
	 * 
	 * <pre>
	 * &lt;login&gt;
	 * 	...
	 * 	&lt;property name="page" value="index.htm" /&gt;
	 * &lt;/login&gt;
	 * </pre>
	 * 
	 * <p>
	 * This login page location is an alternative to servlet container declarative <code>form-login-page</code> from
	 * <code>login-config</code> section from deployment descriptor. Anyway, if application has private resources accessed via
	 * XHR, this login page location is mandatory. Otherwise client agent default login form is used when XHR attempt to access
	 * not authorized resources.
	 */
	private String loginPage;

	/**
	 * Security context is defined in security module and this instance can be null if security module is not provided on
	 * runtime.
	 */
	private ISecurityContext security;

	public TinyContainer() {
		this(CDI.create());
	}

	/** Test constructor. */
	public TinyContainer(CDI cdi) {
		super(cdi);
		log.trace("TinyContainer(CDI)");

		bind(ITinyContainer.class).instance(this).build();
		bind(SecurityContext.class).instance(this).build();

		bind(HttpServletRequest.class).provider(new HttpRequestProvider()).build();
		bind(RequestContext.class).build();
		bind(EventStreamManager.class).to(EventStreamManagerImpl.class).in(Singleton.class).build();

		bindScope(jakarta.enterprise.context.RequestScoped.class, new RequestScopeProvider.Factory<>());
		bindScope(jakarta.enterprise.context.SessionScoped.class, new SessionScopeProvider.Factory<>());
		bindScope(javax.enterprise.context.RequestScoped.class, new RequestScopeProvider.Factory<>());
		bindScope(javax.enterprise.context.SessionScoped.class, new SessionScopeProvider.Factory<>());
	}

	@Override
	protected void create(List<IClassBinding<?>> bindings) {
		super.create(bindings);
		// security can be null if security module is not deployed on runtime
		security = getOptionalInstance(ISecurityContext.class);
	}

	// --------------------------------------------------------------------------------------------
	// SERVLET CONTAINER LISTENERS

	/**
	 * Implements tiny container boostrap logic. See class description for overall performed steps. Also loads context
	 * parameters from external descriptors using {@link ServletContext#getInitParameter(String)}.
	 * <p>
	 * Context initialized listener is not allowed to throw exceptions and it seems there is no standard way to ask servlet
	 * container to abort launching the web application. Tiny container solution is to leave servlet context attribute
	 * {@link #ATTR_INSTANCE} null. On {@link AppServlet#init(javax.servlet.ServletConfig)} mentioned attribute is tested for
	 * null and, if so, permanently mark servlet unavailable.
	 * <p>
	 * <b>Implementation note:</b> bootstrap process logic is based on assumption that this <code>contextInitialized</code>
	 * handler is called before servlets initialization. Here is the relevant excerpt from API-DOC: <em>All
	 * ServletContextListeners are notified of context initialization before any filter or servlet in the web application is
	 * initialized.</em>
	 * <p>
	 * If tiny container start ends in fatal error, signal the host container that hopefully will stop the application. Stack
	 * trace is dumped to logger.
	 * 
	 * @param contextEvent context event provided by servlet container.
	 */
	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		final long start = System.currentTimeMillis();
		final ServletContext servletContext = contextEvent.getServletContext();
		// if present, context path always starts with path separator
		appName = servletContext.getContextPath().isEmpty() ? TinyContainer.ROOT_CONTEXT : servletContext.getContextPath().substring(1);

		// add application name as diagnostic data to logger diagnostic context
		LogContext logContext = LogFactory.getLogContext();
		logContext.put(LOG_CONTEXT_APP, appName);
		log.debug("Initializing servlet context |%s|.", servletContext.getServletContextName());

		Enumeration<String> parameterNames = servletContext.getInitParameterNames();
		while (parameterNames.hasMoreElements()) {
			final String name = parameterNames.nextElement();
			final String value = servletContext.getInitParameter(name);
			final String initParameterName = initParameterName(name);
			System.setProperty(initParameterName, value);
			log.debug("Load context parameter |%s| value |%s| into system properties |%s|.", name, value, initParameterName);
		}

		// WARN: if development context is declared it can access private resources without authentication
		developmentContext = System.getProperty("js.tiny.container.dev.context");

		Bootstrap bootstrap = new Bootstrap();
		try {
			File contextDir = new File(servletContext.getRealPath(""));
			File webinfDir = new File(contextDir, "WEB-INF");
			File appDescriptorfile = new File(webinfDir, "app.xml");
			ConfigBuilder configBuilder = new ConfigBuilder(new FileInputStream(appDescriptorfile));

			bootstrap.startContainer(this, configBuilder.build());

			servletContext.setAttribute(TinyContainer.ATTR_INSTANCE, this);
			log.info("Application |%s| container started in %d msec.", appName, System.currentTimeMillis() - start);
		} catch (ConfigException e) {
			log.error(e);
			log.fatal("Bad container |%s| configuration.", appName);
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (Error | RuntimeException e) {
			log.dump(String.format("Fatal error on container |%s| start:", appName), e);
			log.debug("Signal fatal error |%s| to host container. Application abort.", e.getClass());
			throw e;
		}
	}

	/**
	 * Release resources used by this tiny container instance. After execution this method no HTTP requests can be handled.
	 * <p>
	 * <b>Implementation note:</b> tiny container destruction logic is based on assumption that this
	 * <code>contextDestroyed</code> handler is called after all web application's servlets destruction. Here is the relevant
	 * excerpt from API-DOC: <em>All servlets and filters have been destroy()ed before any ServletContextListeners are notified
	 * of context destruction.</em>
	 * 
	 * @param contextEvent context event provided by servlet container.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent contextEvent) {
		ServletContext servletContext = contextEvent.getServletContext();
		log.debug("Destroying servlet context |%s|.", servletContext.getServletContextName());
		close();
	}

	/**
	 * Record session creation to logger trace.
	 * 
	 * @param sessionEvent session event provided by servlet container.
	 */
	@Override
	public void sessionCreated(HttpSessionEvent sessionEvent) {
		HttpSession httpSession = sessionEvent.getSession();
		log.debug("Creating HTTP session |%s|.", httpSession.getId());
	}

	/**
	 * Record session destroying for logger trace.
	 * 
	 * @param sessionEvent session event provided by servlet container.
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent sessionEvent) {
		HttpSession httpSession = sessionEvent.getSession();
		log.debug("Destroying HTTP session |%s|.", httpSession.getId());
		SessionScopeProvider.destroyContext(this, httpSession);
	}

	@Override
	public void requestInitialized(ServletRequestEvent requestEvent) {
		HttpServletRequest httpRequest = (HttpServletRequest) requestEvent.getServletRequest();
		log.trace("Initializing HTTP request |%s|.", httpRequest.getRequestURI());
		HttpRequestProvider.createContext(httpRequest);
	}

	@Override
	public void requestDestroyed(ServletRequestEvent requestEvent) {
		HttpServletRequest httpRequest = (HttpServletRequest) requestEvent.getServletRequest();
		log.trace("Destroying HTTP request |%s|.", httpRequest.getRequestURI());
		HttpRequestProvider.destroyContext(httpRequest);
		RequestScopeProvider.destroyContext(this, httpRequest);
	}

	// --------------------------------------------------------------------------------------------

	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public boolean login(String username, String password) {
		if (security == null) {
			throw new IllegalStateException("Missing security provider.");
		}
		return security.login(username, password);
	}

	@Override
	public void login(Principal user) {
		if (security == null) {
			throw new IllegalStateException("Missing security provider.");
		}
		security.login(user);
	}

	@Override
	public void logout() {
		if (security == null) {
			throw new IllegalStateException("Missing security provider.");
		}
		security.logout();
	}

	@Override
	public Principal getUserPrincipal() {
		if (security == null) {
			throw new IllegalStateException("Missing security provider.");
		}
		return security.getUserPrincipal();
	}

	@Override
	public boolean isAuthenticated() {
		if (security == null) {
			throw new IllegalStateException("Missing security provider.");
		}
		return security.isAuthenticated();
	}

	@Override
	public boolean isAuthorized(String... roles) {
		if (security == null) {
			throw new IllegalStateException("Missing security provider.");
		}
		if (roles == null) {
			throw new BugError("Null roles.");
		}
		// WARN: if development context is declared it can access private resources without authentication
		RequestContext context = getInstance(RequestContext.class);
		if (developmentContext != null && developmentContext.equals(context.getForwardContextPath())) {
			return true;
		}
		return security.isAuthorized(roles);
	}

	@Override
	public String getLoginPage() {
		return loginPage;
	}

	@Override
	public boolean isUserInRole(String role) {
		return security != null ? security.isAuthorized(role) : false;
	}

	@Override
	public boolean isSecure() {
		try {
			return getInstance(HttpServletRequest.class).isSecure();
		} catch (ContextNotActiveException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	@Override
	public String getAuthenticationScheme() {
		HttpServletRequest httpRequest = null;
		try {
			httpRequest = getInstance(HttpServletRequest.class);
		} catch (ContextNotActiveException e) {
			throw new IllegalStateException(e.getMessage());
		}
		if (httpRequest == null || httpRequest.getAuthType() == null) {
			return null;
		}

		switch (httpRequest.getAuthType()) {
		case HttpServletRequest.BASIC_AUTH:
			return SecurityContext.BASIC_AUTH;

		case HttpServletRequest.CLIENT_CERT_AUTH:
			return SecurityContext.CLIENT_CERT_AUTH;

		case HttpServletRequest.DIGEST_AUTH:
			return SecurityContext.DIGEST_AUTH;

		case HttpServletRequest.FORM_AUTH:
			return SecurityContext.FORM_AUTH;
		}
		return null;
	}

	@Override
	public <T> T getInitParameter(String name, Class<T> type) {
		return super.getInitParameter(initParameterName(name), type);
	}

	private String initParameterName(String name) {
		return Strings.concat(appName, '.', name);
	}

	// --------------------------------------------------------------------------------------------

	static {
		LogProvider logProvider = Classes.loadService(LogProvider.class);
		ShutdownHook shutdownHook = new ShutdownHook(logProvider);
		shutdownHook.setDaemon(false);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private static class ShutdownHook extends Thread {
		private final LogProvider logProvider;

		public ShutdownHook(LogProvider logProvider) {
			this.logProvider = logProvider;
		}

		@Override
		public void run() {
			logProvider.forceImmediateFlush();
		}
	}
}
