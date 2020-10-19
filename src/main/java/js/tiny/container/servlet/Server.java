package js.tiny.container.servlet;

import java.io.File;
import java.io.FileNotFoundException;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.LogFactory;
import js.log.LogProvider;
import js.util.Classes;

/**
 * Server global state and applications logger initialization. This class assumes standard Tomcat deployment. System property
 * {@link #SERVER_BASE} should be defined and point to web server base directory.
 * <p>
 * This class also takes care to initialize applications logger. If logs directory or logging configuration file not found
 * applications logger is forced to console.
 * <p>
 * <b>Warning:</b> If server base directory not found web server start-up fails.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class Server {
	/** System property storing server base directory. */
	private static final String SERVER_BASE = "catalina.base";

	/** Applications working directory relative to catalina base. */
	private static final String APP_WORK = "work/Applications";

	/** Web server logs directory. */
	private static final String LOGS_DIR = "logs";

	/** Web server configuration directory. */
	private static final String CONF_DIR = "conf";

	/** Applications logger configuration file. */
	private static final String LOG_XML = "log.xml";

	private final LogProvider logProvider;

	/** Hook register to JVM to be executed just before JVM halt. */
	private final Thread shutdownHook;

	/** Server base directory, absolute path. */
	private final File serverBase;

	/** Absolute path to applications working directory. This is location where applications private directories are created. */
	private final File appWorkDir;

	/** Initialize server global state and applications logger. */
	public Server() {
		this.logProvider = Classes.loadService(LogProvider.class);
		this.shutdownHook = new ShutdownHook(this.logProvider);
		this.shutdownHook.setDaemon(false);
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);

		String serverBaseProperty = System.getProperty(SERVER_BASE);
		if (serverBaseProperty == null) {
			throw new BugError(log("Invalid environment: missing ${%s} system property. Server startup abort.", SERVER_BASE));
		}
		this.serverBase = new File(serverBaseProperty);
		if (!this.serverBase.exists()) {
			throw new BugError(log("Invalid environment: bad ${%s} system property. Web server directory |%s| does not exist. Server startup abort.", SERVER_BASE, serverBase));
		}

		this.appWorkDir = new File(serverBase, APP_WORK);
		this.appWorkDir.mkdirs();

		File logsDir = new File(serverBase, LOGS_DIR);
		if (!logsDir.exists()) {
			log("Missing logs directory |%s|. Force logger to console.", logsDir);
			return;
		}
		// system property 'logs' is used by log4j, e.g. RollingFileAppender
		System.setProperty("logs", logsDir.getAbsolutePath());

		File confDir = new File(serverBase, CONF_DIR);
		if (!confDir.isDirectory()) {
			log("Missing web server configuration directory |%s|. Force logger to console.", logsDir);
			return;
		}

		File logDescriptor = new File(confDir, LOG_XML);
		Config config = null;
		try {
			ConfigBuilder builder = new ConfigBuilder(logDescriptor);
			config = builder.build();
		} catch (FileNotFoundException unused) {
			log("Logger configuration file |%s| not found. Force logger to console.", logDescriptor);
			return;
		} catch (ConfigException e) {
			throw new BugError(log("Fail to load logger configuration from |%s|. Server fatal stop. Correct logger configuration or remove it to force logger to console.", logDescriptor.getAbsoluteFile()));
		}
		LogFactory.config(config);
	}

	/**
	 * Get application private directory. Application private directory is where application stores its private files. Note that
	 * <code>privateness</code> is rather a good practice recommendation and is not enforced by some system rights.
	 * 
	 * @param appName application name.
	 * @return application working directory.
	 */
	public File getAppDir(String appName) {
		return new File(appWorkDir, appName);
	}

	// --------------------------------------------------------------------------------------------
	// UITILITY METHODS

	/**
	 * Log formatted message to Java logger since framework logger is not yet initialized.
	 * 
	 * @param message message with optional formatting elements as supported by {@link String#format(String, Object...)},
	 * @param args optional arguments.
	 * @return formatted message.
	 */
	private static String log(String message, Object... args) {
		message = String.format(message, args);
		java.util.logging.Logger.getLogger(Server.class.getCanonicalName()).log(java.util.logging.Level.SEVERE, message);
		return message;
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
