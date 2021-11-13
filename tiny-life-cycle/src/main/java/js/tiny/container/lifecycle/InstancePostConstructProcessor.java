package js.tiny.container.lifecycle;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import js.lang.ManagedPostConstruct;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.util.Params;

/**
 * Execute {@link ManagedPostConstruct#postConstruct()} on managed instance. Instance post-construction is executed after
 * initialization and configuration, of course only if managed instance implements {@link ManagedPostConstruct} interface.
 * 
 * @author Iulian Rotaru
 */
public class InstancePostConstructProcessor extends BaseInstanceLifeCycle implements IInstancePostConstructProcessor {
	private static final Log log = LogFactory.getLog(InstancePostConstructProcessor.class);

	/** Cache for @PostConstruct methods filled on the fly. */
	private static final Map<Class<?>, Method> POST_CONSTRUCT_METHODS = new HashMap<>();

	public InstancePostConstructProcessor() {
		log.trace("InstancePostConstructProcessor()");
	}

	@Override
	public Priority getPriority() {
		return Priority.CONSTRUCTOR;
	}

	/**
	 * Execute post-construct on managed instance. In order to perform instance post-construction, managed instance should
	 * implement {@link ManagedPostConstruct} interface.
	 * 
	 * @param instance instance of given managed class, not null.
	 * @throws RuntimeException if instance post-construction fails due to exception of application defined logic.
	 */
	@Override
	public <T> void onInstancePostConstruct(T instance) {
		Params.notNull(instance, "Instance");
		final Class<?> implementationClass = instance.getClass();

		Method method = getAnnotatedMethod(POST_CONSTRUCT_METHODS, implementationClass, PostConstruct.class);
		if (method == null) {
			return;
		}

		log.debug("Post-construct managed instance |%s|", instance.getClass());
		try {
			method.invoke(instance);
		} catch (Throwable t) {
			if (t instanceof InvocationTargetException) {
				t = ((InvocationTargetException) t).getTargetException();
			}
			throw new RuntimeException(format("Managed instance |%s| post-construct fail: %s", implementationClass.getCanonicalName(), t.getMessage()));
		}
	}
	
	// --------------------------------------------------------------------------------------------
	
	void resetCache() {
		POST_CONSTRUCT_METHODS.clear();
	}
}
