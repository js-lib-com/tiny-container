package js.tiny.container.core;

import java.util.ArrayList;
import java.util.List;

import js.app.container.AppContainer;
import js.app.container.AppContainerException;
import js.app.container.AppContainerProvider;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.Factory;
import js.tiny.container.spi.IClassDescriptor;

public class Bootstrap implements AppContainerProvider {
	private static final Log log = LogFactory.getLog(Bootstrap.class);

	public Bootstrap() {
		log.trace("Bootstrap()");
	}

	@Override
	public AppContainer createAppContainer(Object... arguments) {
		log.trace("createAppContainer(Object...)");
		try {
			Container container = new Container();
			startContainer(container, arguments);
			return container;
		} catch (Exception e) {
			log.dump("Fail to create application container:", e);
			throw new AppContainerException();
		}
	}

	public void startContainer(Container container, Object... arguments) throws ConfigException {
		if(arguments.length == 0) {
			ConfigBuilder builder = new ConfigBuilder(getClass().getResourceAsStream("/app.xml"));
			config(container, builder.build());
		}
		else if (arguments.length == 1 && arguments[0] instanceof Config) {
			config(container, (Config) arguments[0]);
		} else {
			modules(container, arguments);
		}
		Factory.bind(container);
		container.start();
	}

	private void config(Container container, Config config) throws ConfigException {
		log.trace("config(Container, Config)");

		List<IClassDescriptor<?>> descriptors = new ArrayList<>();
		for (Config managedClasses : config.findChildren("managed-classes")) {
			for (Config managedClass : managedClasses.getChildren()) {
				descriptors.add(new ClassDescriptor<>(managedClass));
			}
		}
		container.config(descriptors);
	}

	private void modules(Container container, Object... modules) throws ConfigException {
		log.trace("modules(Container, Object...");
		container.config(modules);
	}
}
