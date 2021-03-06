package js.tiny.container.servlet;

import java.io.File;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogContext;
import js.log.LogFactory;
import js.tiny.container.Container;
import js.tiny.container.InstanceScope;
import js.tiny.container.core.AppContext;
import js.tiny.container.core.Factory;
import js.tiny.container.core.SecurityContext;

/**
 * Container specialization for web applications. This class extends {@link Container} adding implementation for
 * {@link InstanceScope#SESSION}, application context services and security context. Tiny container instance is accessible to
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
 * <li>configure tiny container with created configuration object, see {@link #config(Config)},
 * <li>bind tiny container instance to master factory, see {@link Factory#bind(js.tiny.container.core.AppFactory)},
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
 * @version final
 */
public class TinyContainer extends Container implements ServletContextListener, HttpSessionListener, AppContext {
	/** Server global state and applications logger initialization. */
	private static final Server server = new Server();

	/** Class logger. */
	private static final Log log = LogFactory.getLog(TinyContainer.class);

	/** Container instance is stored on servlet context with this attribute name. */
	public static final String ATTR_INSTANCE = "js.tiny.container.instance";

	/** Session attribute name for principal storage when authentication is provided by application. */
	public static final String ATTR_PRINCIPAL = "js.tiny.container.principal";

	/** Name used for root context path. */
	public static final String ROOT_CONTEXT = "root";

	/** Diagnostic context name for context path, aka application. */
	private static final String LOG_CONTEXT_APP = "app";

	/**
	 * Server and container properties loaded from context parameters defined on external descriptors. Context parameters are
	 * optional and this properties instance can be empty. If present, context parameters are used by {@link TinyConfigBuilder}
	 * to inject variables. Also can be retrieved by application using {@link #getProperty(String)}.
	 * <p>
	 * Tiny container uses {@link ServletContext#getInitParameter(String)} to load this context parameters. Context parameters
	 * source may depend on web server implementation but <code>context-param</code> from deployment descriptor is always
	 * supported.
	 */
	private final Properties contextParameters = new Properties();

	/** The name of web application that own this tiny container. Default value to <code>test-app</code>. */
	private String appName = "test-app";

	/**
	 * Development context is running in the same JVM and is allowed to do cross context forward to this context private
	 * resources WITHOUT AUTHENTICATION. It should be explicitly configured on context parameters with the name
	 * 'js.tiny.container.dev.context'.
	 */
	private String developmentContext;

	/**
	 * Application private storage. Privateness is merely a good practice rather than enforced by some system level rights
	 * protection. So called <code>private</code> directory is on working directory and has context name. Files returned by
	 * {@link #getAppFile(String)} are always relative to this <code>private</code> directory.
	 */
	private File privateDir;

	/**
	 * Optional login realm, default to web application context name. Basic authentication realm sent by servlets when client
	 * attempt to access non authorized resource.
	 * <p>
	 * Basic authentication realm is loaded from application descriptor, <code>login</code> section. If not configured uses the
	 * context name.
	 * 
	 * <pre>
	 * &lt;login&gt;
	 * 	&lt;property name="realm" value="Fax2e-mail" /&gt;
	 * 	...
	 * &lt;/login&gt;
	 * </pre>
	 */
	private String loginRealm;

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

	private SecurityContextProvider securityProvider;

	/** Create tiny container instance. */
	public TinyContainer() {
		super();
		log.trace("TinyContainer()");
		registerScopeFactory(new SessionScopeFactory(this));

		for (SecurityContextProvider accessControl : ServiceLoader.load(SecurityContextProvider.class)) {
			log.info("Found access control implementation |%s|.", accessControl.getClass());
			if (this.securityProvider != null) {
				throw new BugError("Invalid runtime environment. Multiple access control implementation.");
			}
			this.securityProvider = accessControl;
		}

		if (this.securityProvider == null) {
			this.securityProvider = new TinySecurityContext();
		}
	}

	@Override
	protected void registerInstanceProcessor() {
		registerInstanceProcessor(new ContextParamProcessor(this));
		super.registerInstanceProcessor();
	}

	@Override
	protected void registerClassProcessor() {
		registerClassProcessor(new ContextParamProcessor(this));
		super.registerClassProcessor();
	}

	@Override
	public void config(Config config) throws ConfigException {
		super.config(config);

		// by convention configuration object name is the web application name
		appName = config.getName();

		privateDir = server.getAppDir(appName);
		if (!privateDir.exists()) {
			privateDir.mkdir();
		}

		Config loginConfig = config.getChild("login");
		if (loginConfig != null) {
			loginRealm = loginConfig.getProperty("realm", appName);
			loginPage = loginConfig.getProperty("page");
			if (loginPage != null && !loginPage.startsWith("/")) {
				StringBuilder loginPageBuilder = new StringBuilder();
				loginPageBuilder.append('/');
				if (!TinyConfigBuilder.ROOT_APP_NAME.equals(appName)) {
					loginPageBuilder.append(appName);
					loginPageBuilder.append('/');
				}
				loginPageBuilder.append(loginPage);
				loginPage = loginPageBuilder.toString();
			}
		}
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
		final ServletContext servletContext = contextEvent.getServletContext();

		/** Logger diagnostic context stores contextual information regarding current request. */
		LogContext logContext = LogFactory.getLogContext();
		logContext.put(LOG_CONTEXT_APP, servletContext.getContextPath().isEmpty() ? TinyContainer.ROOT_CONTEXT : servletContext.getContextPath().substring(1));

		final long start = System.currentTimeMillis();
		log.debug("Starting application |%s| container...", servletContext.getContextPath());

		Enumeration<String> parameterNames = servletContext.getInitParameterNames();
		while (parameterNames.hasMoreElements()) {
			final String name = parameterNames.nextElement();
			final String value = servletContext.getInitParameter(name);
			contextParameters.setProperty(name, value);
			log.debug("Load context parameter |%s| value |%s|.", name, value);
		}

		// WARN: if development context is declared it can access private resources without authentication
		developmentContext = contextParameters.getProperty("js.tiny.container.dev.context");

		try {
			ConfigBuilder builder = new TinyConfigBuilder(servletContext, contextParameters);
			config(builder.build());

			Factory.bind(this);
			start();

			// set tiny container reference on servlet context attribute ONLY if no exception
			servletContext.setAttribute(TinyContainer.ATTR_INSTANCE, this);
			log.info("Application |%s| container started in %d msec.", appName, System.currentTimeMillis() - start);
		} catch (ConfigException e) {
			log.error(e);
			log.fatal("Bad container |%s| configuration.", appName);
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
		log.debug("Context |%s| destroying.", appName);
		try {
			destroy();
		} catch (Throwable t) {
			log.dump(String.format("Fatal error on container |%s| destroy:", appName), t);
		}
	}

	/**
	 * Record session creation to logger trace.
	 * 
	 * @param sessionEvent session event provided by servlet container.
	 */
	public void sessionCreated(HttpSessionEvent sessionEvent) {
		log.trace("Create HTTP session |%s|.", sessionEvent.getSession().getId());
	}

	/**
	 * Record session destroying for logger trace.
	 * 
	 * @param sessionEvent session event provided by servlet container.
	 */
	public void sessionDestroyed(HttpSessionEvent sessionEvent) {
		log.trace("Destroy HTTP session |%s|.", sessionEvent.getSession().getId());
	}

	// --------------------------------------------------------------------------------------------
	// APPLICATION CONTEXT INTERFACE

	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public File getAppFile(String path) {
		return new File(privateDir, path);
	}

	@Override
	public String getProperty(String name) {
		return contextParameters.getProperty(name);
	}

	@Override
	public <T> T getProperty(String name, Class<T> type) {
		return ConverterRegistry.getConverter().asObject(contextParameters.getProperty(name), type);
	}

	@Override
	public Locale getRequestLocale() {
		return getInstance(RequestContext.class).getLocale();
	}

	@Override
	public String getRemoteAddr() {
		return getInstance(RequestContext.class).getRemoteHost();
	}

	// --------------------------------------------------------------------------------------------
	// SECURITY CONTEXT INTERFACE

	@Override
	public boolean login(String username, String password) {
		return securityProvider.login(getInstance(RequestContext.class), username, password);
	}

	@Override
	public void login(Principal user) {
		securityProvider.login(getInstance(RequestContext.class), user);
	}

	@Override
	public void logout() {
		securityProvider.logout(getInstance(RequestContext.class));
	}

	@Override
	public Principal getUserPrincipal() {
		return securityProvider.getUserPrincipal(getInstance(RequestContext.class));
	}

	@Override
	public boolean isAuthenticated() {
		return getUserPrincipal() != null;
	}

	@Override
	public boolean isAuthorized(String... roles) {
		if (roles == null) {
			throw new BugError("Null roles.");
		}
		// WARN: if development context is declared it can access private resources without authentication
		RequestContext context = getInstance(RequestContext.class);
		if (developmentContext != null && developmentContext.equals(context.getForwardContextPath())) {
			return true;
		}
		return securityProvider.isAuthorized(context, roles);
	}

	// --------------------------------------------------------------------------------------------
	// CONTAINER SPI

	@Override
	public String getLoginRealm() {
		return loginRealm;
	}

	@Override
	public String getLoginPage() {
		return loginPage;
	}

	@Override
	public void setProperty(String name, Object value) {
		if (!(value instanceof String)) {
			value = ConverterRegistry.getConverter().asString(value);
		}
		contextParameters.put(name, value);
	}
}
