package js.tiny.container.core;

import js.app.container.AppContainer;
import js.app.container.AppContainerException;
import js.app.container.AppContainerProvider;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.Factory;

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
		if (arguments.length == 0) {
			ConfigBuilder builder = new ConfigBuilder(getClass().getResourceAsStream("/app.xml"));
			container.configure(builder.build());
		} else if (arguments.length == 1 && arguments[0] instanceof Config) {
			container.configure((Config) arguments[0]);
		} else {
			container.config(arguments);
		}
		
		Factory.bind(container);
		container.start();
	}
}
