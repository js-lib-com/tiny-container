package js.tiny.container.core;

import java.util.ArrayList;
import java.util.List;

import js.app.container.AppContainer;
import js.app.container.AppContainerException;
import js.lang.Config;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IClassDescriptor;

public class AppContainerProvider implements js.app.container.AppContainerProvider {
	private static final Log log = LogFactory.getLog(AppContainerProvider.class);

	public AppContainerProvider() {
		log.trace("AppContainerProvider()");
	}

	@Override
	public AppContainer createAppContainer(Object... arguments) {
		log.trace("createAppContainer(Object...)");
		try {
			if (arguments.length == 1 && arguments[0] instanceof Config) {
				return config((Config) arguments[0]);
			}
			return modules(arguments);
		} catch (Exception e) {
			throw new AppContainerException();
		}
	}

	private AppContainer config(Config config) throws ConfigException {
		Container container = new Container();

		List<IClassDescriptor<?>> descriptors = new ArrayList<>();
		for (Config managedClasses : config.findChildren("managed-classes")) {
			for (Config managedClass : managedClasses.getChildren()) {
				descriptors.add(new ClassDescriptor<>(managedClass));
			}
		}

		container.create(descriptors);

		container.start();
		return container;
	}

	private AppContainer modules(Object... modules) throws ConfigException {
		Container container = new Container();
		container.config(modules);
		container.start();
		return container;
	}
}
