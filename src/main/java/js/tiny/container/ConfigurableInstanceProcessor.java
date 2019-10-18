package js.tiny.container;

import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.log.Log;
import js.log.LogFactory;

/**
 * Configure instance with configuration object provided by instance managed class. The actual instance configuration is
 * performed only if instance is {@link Configurable} and managed class has configuration object. If both conditions are
 * satisfied this instance processor executes {@link Configurable#config(Config)} on instance.
 * <p>
 * Configuration is about running application defined code that can fail. This processor catch all exceptions, including those
 * related to invalid configuration object and throw {@link BugError} since is clearly a not intended behavior.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class ConfigurableInstanceProcessor implements InstanceProcessor {
	private static final Log log = LogFactory.getLog(ConfigurableInstanceProcessor.class);

	/**
	 * Configure instance with configuration object provided by instance managed class. In order to perform instance
	 * configuration, instance should implement {@link Configurable} and managed class should have configuration object, see
	 * {@link ManagedClassSPI#getConfig()}. If both conditions are satisfied this method executes
	 * {@link Configurable#config(Config)} on instance.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError if instance configuration fails either due to invalid configuration object or exception on configuration
	 *             user defined logic.
	 */
	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		if (!(instance instanceof Configurable)) {
			return;
		}
		Config config = managedClass.getConfig();
		if (config == null) {
			if(!(instance instanceof OptionalConfigurable)) {
				return;
			}
			log.info("Default configuration for managed class |%s|.", managedClass);
		}

		try {
			((Configurable) instance).config(config);
		} catch (ConfigException e) {
			throw new BugError("Invalid configuration for managed class |%s|:\r\n\t- %s", managedClass, e);
		} catch (Throwable t) {
			throw new BugError(t);
		}
	}
}
