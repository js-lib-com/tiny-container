package js.tiny.container.servlet;

import java.util.List;

import js.lang.Config;

/**
 * Provider that contribute to {@link TinyConfigBuilder} - tiny container configuration. Provider configuration is appended at
 * the end of the container configuration object, in no particular order.
 * 
 * @author Iulian Rotaru
 */
public interface TinyConfigProvider {
	/**
	 * Get a list of configuration objects to add to tiny container configuration. Returned list must not be empty but is legal
	 * to have a single element.
	 * 
	 * @return non empty list of configuration objects.
	 */
	List<Config> getConfig();
}
