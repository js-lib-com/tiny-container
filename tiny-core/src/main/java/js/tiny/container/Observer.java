package js.tiny.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import js.lang.Config;
import js.lang.Configurable;
import js.lang.ManagedLifeCycle;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.AppContext;
import js.tiny.container.core.Factory;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedMethod;

/**
 * Performance meters observer. Collect periodically application invocation meters and dump values to meter logger. In order to
 * enable observer one should declare it into application descriptor and configure observer logger appender.
 * <p>
 * Enable observer and set period to one minute. Note that period is in milliseconds and that there is no guarantee on strict
 * period precision.
 * 
 * <pre>
 * &lt;app-descriptor&gt;
 * 	&lt;observer period="600000" /&gt;
 * 	...
 * &lt;/app-descriptor&gt;
 * </pre>
 * <p>
 * Here is a sample logging configuration with appender for meters dump.
 * 
 * <pre>
 * &lt;appender name="OBS"&gt;
 * 	&lt;class&gt;org.apache.log4j.RollingFileAppender&lt;/class&gt;
 * 	&lt;format&gt;%d{dd HH:mm:ss,SSS} %m%n&lt;/format&gt;
 * 	&lt;parameters&gt;
 * 		&lt;file&gt;${logs}/meters.log&lt;/file&gt;
 * 		...
 * 	&lt;/parameters&gt;
 * &lt;/appender&gt;
 * ...         
 * &lt;logger name="js.core.Meter"&gt;
 * 	&lt;appender&gt;OBS&lt;/appender&gt;
 * 	&lt;level&gt;INFO&lt;/level&gt;
 * &lt;/logger&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version draft
 */
final class Observer implements Configurable, ManagedLifeCycle, Runnable {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Observer.class);

	/** Meters logger. Into log configuration this logger can be retrieved via {@link Meter} class. */
	private static final Log meterLog = LogFactory.getLog(Meter.class);

	/** Timeout for observer thread stop. */
	private static final int THREAD_STOP_TIMEOUT = 4000;

	/** Application name displayed into meters dump header. */
	private final String appName;

	/** Observer worker thread. */
	private Thread thread;

	/** Flag indicating that worker thread is running. */
	private volatile boolean running;

	/** Meters sampling period, in milliseconds. */
	private int period;

	/**
	 * Construct instrumentation manager for given parent application.
	 * 
	 * @param app parent application.
	 */
	private Observer(AppContext app) {
		log.trace("Observer(App)");
		this.appName = app.getAppName();
	}

	@Override
	public void config(Config config) {
		log.trace("config(Config.Element)");
		period = config.getAttribute("period", int.class, 0);
	}

	@Override
	public void postConstruct() throws Exception {
		log.trace("postConstruct()");
		// start observer thread only if application is configured with observer period
		if (period > 0) {
			running = true;
			thread = new Thread(this, getClass().getSimpleName());
			thread.start();
		}
	}

	@Override
	public void preDestroy() throws Exception {
		log.trace("preDestroy()");
		if (running) {
			synchronized (this) {
				running = false;
				thread.interrupt();
				this.wait(THREAD_STOP_TIMEOUT);
			}
		}
	}

	@Override
	public void run() {
		log.debug("Start meters observer on thread |%s|.", thread);

		for (;;) {
			try {
				Thread.sleep(period);
			} catch (InterruptedException unused) {
				if (!running) {
					break;
				}
			}

			List<InvocationMeter> invocationMeters = getMeters();
			Collections.sort(invocationMeters, new Comparator<InvocationMeter>() {
				@Override
				public int compare(InvocationMeter m1, InvocationMeter m2) {
					return ((Long) m1.getMaxProcessingTime()).compareTo(m2.getMaxProcessingTime());
				}
			});

			meterLog.info("Start observer meters dump for %s:", appName);
			for (InvocationMeter meter : invocationMeters) {
				if (meter.getInvocationsCount() != 0) {
					meterLog.info(meter.toExternalForm());
				}
			}
		}

		synchronized (this) {
			this.notify();
		}
		log.debug("Stop meters observer on thread |%s|.", thread);
	}

	/**
	 * Collect invocation meters from application managed classes.
	 * 
	 * @return application invocation meters.
	 */
	private static List<InvocationMeter> getMeters() {
		List<InvocationMeter> invocationMeters = new ArrayList<InvocationMeter>();
		IContainer container = (IContainer) Factory.getAppFactory();
		for (IManagedMethod managedMethod : container.getManagedMethods()) {
			invocationMeters.add(((ManagedMethod) managedMethod).getMeter());
		}
		return invocationMeters;
	}
}
