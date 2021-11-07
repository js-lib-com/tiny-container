package js.tiny.container;

import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.core.Container;

/**
 * Container interface for standalone applications. 
 * 
 * @author Iulian Rotaru
 */
public interface AppContainer extends AutoCloseable {
	
	public static AppContainer create(Config config) throws ConfigException {
		Container container = new Container();
		container.config(config);
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
