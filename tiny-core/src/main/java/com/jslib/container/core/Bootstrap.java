package com.jslib.container.core;

import java.io.InputStream;

import com.jslib.api.container.EmbeddedContainer;
import com.jslib.api.container.EmbeddedContainerException;
import com.jslib.api.container.EmbeddedContainerProvider;
import com.jslib.api.injector.IModule;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.cdi.CDI;
import com.jslib.container.spi.Factory;
import com.jslib.lang.Config;
import com.jslib.lang.ConfigBuilder;
import com.jslib.lang.ConfigException;
import com.jslib.util.Classes;

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
			container.init(CDI.create());
			startContainer(container, arguments);
			return container;
		} catch (Exception e) {
			log.dump("Fail to create application container:", e);
			throw new EmbeddedContainerException();
		}
	}

	public void startContainer(Container container, Object... arguments) throws ConfigException {
		if (arguments.length == 0) {
			log.debug("Load injector bindings from default '/app.xml' resource.");
			ConfigBuilder builder = new ConfigBuilder(Classes.getResourceAsStream("/app.xml"));
			container.configure(builder.build());
		}

		else if (arguments[0] == null) {
			// if application bindings descriptor is not present argument is null
			log.debug("Empty injector bindings.");
			Config config = new Config("app");
			container.configure(config);
		}

		else if (arguments[0] instanceof InputStream) {
			log.debug("Load injector bindings from configuration stream.");
			ConfigBuilder builder = new ConfigBuilder((InputStream) arguments[0]);
			container.configure(builder.build());
		}

		else if (arguments[0] instanceof Config) {
			log.debug("Load injector bindings from configuration object.");
			container.configure((Config) arguments[0]);
		}

		else {
			IModule[] modules = new IModule[arguments.length];
			for (int i = 0; i < arguments.length; ++i) {
				log.debug("Load injector bindings from module |{java_type}|.", arguments[i].getClass());
				modules[i] = (IModule) arguments[i];
			}
			container.modules(modules);
		}

		Factory.bind(container);
		log.debug("Starting container...");
		container.start();
	}
}
