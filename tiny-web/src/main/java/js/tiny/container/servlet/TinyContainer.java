package js.tiny.container.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.Principal;
import java.util.Enumeration;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import js.converter.ConverterRegistry;
import js.injector.RequestScoped;
import js.injector.SessionScoped;
import js.injector.ThreadScoped;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogContext;
import js.log.LogFactory;
import js.tiny.container.cdi.CDI;
import js.tiny.container.core.Bootstrap;
import js.tiny.container.core.Container;
import js.tiny.container.net.EventStreamManager;
import js.tiny.container.net.EventStreamManagerImpl;

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
 * @version final
 */
public class TinyContainer extends Container implements ServletContextListener, HttpSessionListener, ITinyContainer, WebContext {
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

	/** The name of web application that own this tiny container. Default value to <code>test-app</code>. */
	private String appName = "test-app";

	/**
	 * Development context is running in the same JVM and is allowed to do cross context forward to this context private
	 * resources WITHOUT AUTHENTICATION. It should be explicitly configured on context parameters with the name
	 * <code>js.tiny.container.dev.context</code>.
	 */
	private String developmentContext;

	/**
	 * Application private storage. Privateness is merely a good practice rather than enforced by some system level rights
	 * protection. So called <code>private</code> directory is on working directory and has context name. Files returned by
	 * {@link #getAppFile(String)} are always relative to this <code>private</code> directory.
	 */
	private File privateDir;

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

	private TinySecurity security;

	public TinyContainer() {
		this(CDI.create(), new TinySecurity());
	}

	/** Test constructor. */
	public TinyContainer(CDI cdi, TinySecurity security) {
		super(cdi);
		log.trace("TinyContainer(CDI, TinySecurity)");

		bind(ITinyContainer.class).instance(this).build();
		bind(WebContext.class).instance(this).build();
		bind(SecurityContext.class).instance(this).build();

		bind(RequestContext.class).in(ThreadScoped.class).build();
		bind(EventStreamManager.class).to(EventStreamManagerImpl.class).in(Singleton.class).build();

		bindScope(RequestScoped.class, new RequestScopeProvider.Factory<>());
		bindScope(SessionScoped.class, new SessionScopeProvider.Factory<>());

		this.security = security;
	}

	@Override
	public void configure(Config config) throws ConfigException {
		super.configure(config);

		// by convention configuration object name is the web application name
		// appName = config.getName();

		privateDir = server.getAppDir(appName);
		if (!privateDir.exists()) {
			privateDir.mkdir();
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
			System.setProperty(name, value);
			log.debug("Load context parameter |%s| value |%s| into system properties.", name, value);
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

			// set tiny container reference on servlet context attribute ONLY if no exception
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
		log.debug("Context |%s| destroying.", appName);
		try {
			close();
		} catch (Throwable t) {
			log.dump(String.format("Fatal error on container |%s| destroy:", appName), t);
		}
	}

	/**
	 * Record session creation to logger trace.
	 * 
	 * @param sessionEvent session event provided by servlet container.
	 */
	@Override
	public void sessionCreated(HttpSessionEvent sessionEvent) {
		log.trace("Create HTTP session |%s|.", sessionEvent.getSession().getId());
	}

	/**
	 * Record session destroying for logger trace.
	 * 
	 * @param sessionEvent session event provided by servlet container.
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent sessionEvent) {
		HttpSession httpSession = sessionEvent.getSession();
		log.trace("Destroy HTTP session |%s|.", httpSession.getId());

		Enumeration<String> attributes = httpSession.getAttributeNames();
		while (attributes.hasMoreElements()) {
			// TODO: check that attribute name has the pattern for scope provider instance
			Object instance = httpSession.getAttribute(attributes.nextElement());
			onInstanceDestroyed(instance);
		}
	}

	// --------------------------------------------------------------------------------------------
	// WEB CONTEXT INTERFACE

	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public File getAppFile(String path) {
		return new File(privateDir, path);
	}

	@Override
	public <T> T getProperty(String name, Class<T> type) {
		return ConverterRegistry.getConverter().asObject(System.getProperty(name), type);
	}

	@Override
	public RequestContext getRequestContext() {
		return getInstance(RequestContext.class);
	}

	@Override
	public SecurityContext getSecurityContext() {
		return this;
	}

	// --------------------------------------------------------------------------------------------
	// SECURITY CONTEXT INTERFACE

	@Override
	public boolean login(String username, String password) {
		return security.login(getInstance(RequestContext.class), username, password);
	}

	@Override
	public void login(Principal user) {
		security.login(getInstance(RequestContext.class), user);
	}

	@Override
	public void logout() {
		security.logout(getInstance(RequestContext.class));
	}

	@Override
	public Principal getUserPrincipal() {
		return security.getUserPrincipal(getInstance(RequestContext.class));
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
		return security.isAuthorized(context, roles);
	}

	// --------------------------------------------------------------------------------------------
	// CONTAINER SPI

	@Override
	public String getLoginPage() {
		return loginPage;
	}
}
