package js.tiny.container.unit;

import java.io.File;
import java.io.InputStream;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.Factory;

public final class TestContext {
	public static ITinyContainer start() throws Exception {
		return init(new ConfigBuilder("<app></app>").build());
	}

	public static ITinyContainer start(File config) throws Exception {
		return init(new ConfigBuilder(config).build());
	}

	public static ITinyContainer start(String config) throws Exception {
		return init(new ConfigBuilder(config).build());
	}

	public static ITinyContainer start(InputStream config) throws Exception {
		return init(new ConfigBuilder(config).build());
	}
	
	private static ITinyContainer init(Config config) throws Exception {
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

		return container;
	}
}
