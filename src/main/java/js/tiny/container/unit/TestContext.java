package js.tiny.container.unit;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import js.lang.Config;
import js.tiny.container.core.AppContext;
import js.tiny.container.core.Factory;
import js.tiny.container.servlet.TinyContainer;

public final class TestContext {
	public static AppContext start() throws Exception {
		return init(new TestConfigBuilder().build());
	}

	public static AppContext start(File config) throws Exception {
		return init(new TestConfigBuilder(config).build());
	}

	public static AppContext start(String config) throws Exception {
		return init(new TestConfigBuilder(config).build());
	}

	public static AppContext start(String config, Properties properties) throws Exception {
		return init(new TestConfigBuilder(config, properties).build());
	}

	public static AppContext start(InputStream config) throws Exception {
		return init(new TestConfigBuilder(config).build());
	}

	public static void setProperty(String name, Object value) {
		TinyContainer container = (TinyContainer) Factory.getInstance(AppContext.class);
		container.setProperty(name, value);
	}
	
	private static AppContext init(Config config) throws Exception {
		String catalinaBase = "fixture/tomcat";
		System.setProperty("catalina.base", catalinaBase);

		File serverDir = new File(catalinaBase);
		serverDir.mkdirs();
		for (String dir : new String[] { "conf", "lib", "logs", "webapps", "work" }) {
			new File(serverDir, dir).mkdirs();
		}

		TinyContainer container = new TinyContainer();
		container.config(config);

		Factory.bind(container);
		container.start();

		return (AppContext) container;
	}
}
