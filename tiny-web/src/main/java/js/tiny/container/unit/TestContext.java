package js.tiny.container.unit;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.tiny.container.core.ClassDescriptor;
import js.tiny.container.servlet.ITinyContainer;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.Factory;
import js.tiny.container.spi.IClassDescriptor;

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
		
		List<IClassDescriptor<?>> descriptors = new ArrayList<>();
		for (Config managedClasses : config.findChildren("managed-classes")) {
			for (Config managedClass : managedClasses.getChildren()) {
				descriptors.add(new ClassDescriptor<>(managedClass));
			}
		}
		container.config(descriptors);

		Factory.bind(container);
		container.start();

		return container;
	}
}
