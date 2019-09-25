package js.servlet.test.it;

import static org.junit.Assert.assertEquals;

import java.io.File;

import js.lang.BugError;
import js.lang.InvocationException;
import js.util.Classes;

import org.junit.After;
import org.junit.Test;

public class ServerUnitTest {
	@After
	public void afterTest() {
		System.clearProperty("catalina.base");
	}

	@Test
	public void constructor() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
		Object server = newInstance();
		assertEquals(new File("fixture/server/tomcat"), Classes.getFieldValue(server, "serverBase"));
		assertEquals(new File("fixture/server/tomcat/work/Applications"), Classes.getFieldValue(server, "appWorkDir"));
	}

	@Test
	public void getAppDir() throws Exception {
		System.setProperty("catalina.base", "fixture/server/tomcat");
		Object server = newInstance();
		assertEquals(new File("fixture/server/tomcat/work/Applications/test-app"), Classes.invoke(server, "getAppDir", "test-app"));
	}

	/** Missing logs directory should not throw exception. */
	@Test
	public void constructor_NoLogsDir() {
		System.setProperty("catalina.base", "fixture/server/tomcat-no-logs");
		newInstance();
	}

	/** Missing log configuration file should not throw exception. */
	@Test
	public void constructor_NoLogXml() {
		System.setProperty("catalina.base", "fixture/server/tomcat-no-log-xml");
		newInstance();
	}

	/** Missing configuration directory should not throw exception. */
	@Test
	public void constructor_NoConfDir() {
		System.setProperty("catalina.base", "fixture/server/tomcat-no-conf");
		newInstance();
	}

	@Test(expected = BugError.class)
	public void constructor_LogException() {
		System.setProperty("catalina.base", "fixture/server/tomcat-log-exception");
		newInstance();
	}

	@Test(expected = BugError.class)
	public void constructor_NoSystemProperty() {
		newInstance();
	}

	@Test(expected = BugError.class)
	public void constructor_NoBaseDir() {
		System.setProperty("catalina.base", "fake/dir");
		newInstance();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static Object newInstance() {
		try {
			return Classes.newInstance("js.servlet.Server");
		} catch (InvocationException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
		}
		return null;
	}
}
