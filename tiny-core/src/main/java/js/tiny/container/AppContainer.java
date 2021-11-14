package js.tiny.container;

import java.util.ArrayList;
import java.util.List;

import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.core.ClassDescriptor;
import js.tiny.container.core.Container;
import js.tiny.container.spi.IClassDescriptor;

/**
 * Container interface for standalone applications.
 * 
 * @author Iulian Rotaru
 */
public interface AppContainer extends AutoCloseable {

	public static AppContainer create(Config config) throws ConfigException {
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

	public static AppContainer create(Object... modules) throws ConfigException {
		Container container = new Container();
		container.config(modules);
		container.start();
		return container;
	}

	<T> T getInstance(Class<T> interfaceClass);

}
