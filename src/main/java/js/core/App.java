package js.core;

import js.lang.Configurable;
import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;

/**
 * Base class for maintaining global application state. You can provide your own implementation by creating a subclass and
 * declaring it on managed classes descriptor. The App class is instantiated before any other managed class with managed life
 * cycle and is destroyed last.
 * <p>
 * Application instance can be injected into managed classes or retrieved from application factory.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class App implements ManagedLifeCycle {
	/** Class logger. */
	private static Log log = LogFactory.getLog(App.class);

	/** Application context provides access to container public services. */
	private final AppContext context;

	/**
	 * Create application instance with given application context.
	 * 
	 * @param context application context.
	 */
	public App(AppContext context) {
		log.trace("App(AppContext)");
		this.context = context;
	}

	/**
	 * Callback invoked by container after instance created. If subclass implements {@link Configurable} container guarantees
	 * configuration is executed before executing this callback.
	 * <p>
	 * Current implementation just delegates {@link #onCreate(AppContext)}. If subclass overrides this callback it must call
	 * super in order to have on create hook executed.
	 * <p>
	 * Exceptions thrown by this callback are fatal and results in container failing to start.
	 * 
	 * @throws Exception hook exception is routed to container.
	 */
	@Override
	public void postConstruct() throws Exception {
		log.trace("postConstruct()");
		onCreate(context);
	}

	/**
	 * Callback executed by container before instance destroying. In this context instance <code>destroying</code> means
	 * removing instance from scope factory cache and allowing GC to reclaim instance memory.
	 * <p>
	 * Current implementation just delegates {@link #onDestroy(AppContext)}. If subclass overrides this callback it must call
	 * super in order to have on destroy hook executed.
	 * <p>
	 * Exceptions thrown by this callback are recorded to error logger but does not interrupt destroying process.
	 * 
	 * @throws Exception hook exception is routed to container.
	 */
	@Override
	public void preDestroy() throws Exception {
		log.trace("preDestroy()");
		onDestroy(context);
	}

	/**
	 * Hook invoked on application creation. Subclass may override this hook and provide user defined logic to be executed at
	 * application start. Since this hook is invoked by {@link #postConstruct()} container guaranteed is executed after
	 * configuration, of course if subclass implements {@link Configurable}.
	 * <p>
	 * Exceptions thrown by this hook are fatal and results in container failing to start.
	 * 
	 * @param context application context.
	 * @throws Exception hook exception is routed to container.
	 */
	protected void onCreate(AppContext context) throws Exception {
	}

	/**
	 * Hook invoked on application destroying. Subclass may override this hook and provide user defined logic to be executed at
	 * application end.
	 * <p>
	 * Exceptions thrown by this hook are recorded to error logger but does not interrupt destroying process.
	 * 
	 * @param context application context.
	 * @throws Exception hook exception is routed to container.
	 */
	protected void onDestroy(AppContext context) throws Exception {
	}

	/**
	 * Get application context.
	 * 
	 * @return application context.
	 * @see #context
	 */
	public AppContext getContext() {
		return context;
	}
}
