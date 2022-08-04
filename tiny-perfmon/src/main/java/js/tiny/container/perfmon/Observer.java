package js.tiny.container.perfmon;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

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
 */
@Startup
public class Observer implements Runnable {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Observer.class);

	/** Meters logger. Into log configuration this logger can be retrieved via {@link IMeter} class. */
	private static final Log meterLog = LogFactory.getLog(IMeter.class);

	/** Timeout for observer thread stop. */
	private static final int THREAD_STOP_TIMEOUT = 4000;

	private final MetersStore meters;
	
	/** Observer worker thread. */
	private Thread thread;

	/** Flag indicating that worker thread is running. */
	private volatile boolean running;

	/** Meters sampling period, in milliseconds. */
	private int period = 60000;

	/**
	 * Construct instrumentation manager for given parent application.
	 * 
	 * @param app parent application.
	 */
	@Inject
	public Observer(MetersStore metersStore) {
		log.trace("Observer(MetersStore)");
		this.meters = metersStore;
	}

	@PostConstruct
	public void postConstruct() {
		log.trace("postConstruct()");
		// start observer thread only if application is configured with observer period
		if (period > 0) {
			running = true;
			thread = new Thread(this, getClass().getSimpleName());
			thread.start();
		}
	}
	
	@PreDestroy
	public void preDestroy() {
		log.trace("preDestroy()");
		if (running) {
			synchronized (this) {
				running = false;
				thread.interrupt();
				try {
					this.wait(THREAD_STOP_TIMEOUT);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
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

			meterLog.info("Invocation meters dump:");
			for (IInvocationMeter meter : meters.getInvocationMeters()) {
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
}
