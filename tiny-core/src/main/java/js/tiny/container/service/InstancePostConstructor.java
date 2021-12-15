package js.tiny.container.service;

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
import js.tiny.container.spi.IManagedClass;
import js.util.Params;

/**
 * Execute {@link ManagedPostConstruct#postConstruct()} on managed instance. Instance post-construction is executed after
 * initialization and configuration, of course only if managed instance implements {@link ManagedPostConstruct} interface.
 * 
 * @author Iulian Rotaru
 */
public class InstancePostConstructor extends BaseInstanceLifecycle implements IInstancePostConstructProcessor {
	private static final Log log = LogFactory.getLog(InstancePostConstructor.class);

	/** Cache for @PostConstruct methods filled on the fly. */
	private static final Map<Class<?>, Method> POST_CONSTRUCT_METHODS = new HashMap<>();

	@Override
	public Priority getPriority() {
		return Priority.CONSTRUCTOR;
	}

	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		return scanAnnotatedMethod(POST_CONSTRUCT_METHODS, managedClass.getImplementationClass(), PostConstruct.class);
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
		log.debug("Post-construct managed instance |%s|", implementationClass);

		Method method = POST_CONSTRUCT_METHODS.get(implementationClass);
		if (method == null) {
			throw new IllegalStateException(format("Missing post-construct method on implementation class |%s|.", implementationClass));
		}

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
