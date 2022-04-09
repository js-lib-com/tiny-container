package js.tiny.container.core;

import java.io.InputStream;

import js.embedded.container.EmbeddedContainer;
import js.embedded.container.EmbeddedContainerException;
import js.embedded.container.EmbeddedContainerProvider;
import js.injector.IModule;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.Factory;
import js.util.Params;

public class Bootstrap implements EmbeddedContainerProvider {
	private static final Log log = LogFactory.getLog(Bootstrap.class);

	public Bootstrap() {
		log.trace("Bootstrap()");
	}

	@Override
	public EmbeddedContainer createAppContainer(Object... arguments) {
		log.trace("createAppContainer(Object...)");
		try {
			Container container = new Container();
			startContainer(container, arguments);
			return container;
		} catch (Exception e) {
			log.dump("Fail to create application container:", e);
			throw new EmbeddedContainerException();
		}
	}

	public void startContainer(Container container, Object... arguments) throws ConfigException {
		Params.GT(arguments.length, 0, "Arguments length");

		if (arguments.length == 1) {
			if (arguments[0] == null) {
				// if application bindings descriptor is not present argument is null
				log.debug("Empty application bindings.");
				Config config = new Config("app");
				container.configure(config);
			} else if (arguments[0] instanceof InputStream) {
				log.debug("Load bindings from configuration stream.");
				ConfigBuilder builder = new ConfigBuilder((InputStream) arguments[0]);
				container.configure(builder.build());
			} else if (arguments[0] instanceof Config) {
				log.debug("Load bindings from configuration object.");
				container.configure((Config) arguments[0]);
			}
		} else {
			IModule[] modules = new IModule[arguments.length];
			for (int i = 0; i < arguments.length; ++i) {
				log.debug("Load bindings from module |%s|.", arguments[i].getClass());
				modules[i] = (IModule) arguments[i];
			}
			container.modules(modules);
		}

		Factory.bind(container);
		log.debug("Starting container...");
		container.start();
	}
}
