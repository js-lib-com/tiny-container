package js.tiny.container.lifecycle;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedClass;
import js.util.Params;

public class InstancePreDestroyProcessor extends BaseInstanceLifeCycle implements IInstancePreDestroyProcessor {
	private static final Log log = LogFactory.getLog(InstancePreDestroyProcessor.class);

	/** Cache for @PreDestroy methods filled on the fly. */
	private static final Map<Class<?>, Method> PRE_DESTROY_METHODS = new HashMap<>();

	public InstancePreDestroyProcessor() {
		log.trace("InstancePreDestroyProcessor()");
	}

	@Override
	public Priority getPriority() {
		return Priority.DESTRUCTOR;
	}

	@Override
	public <T> void onInstancePreDestroy(IManagedClass<T> managedClass, T instance) {
		Params.notNull(instance, "Instance");
		final Class<?> implementationClass = instance.getClass();

		Method method = getAnnotatedMethod(PRE_DESTROY_METHODS, implementationClass, PreDestroy.class);
		if (method == null) {
			return;
		}

		log.debug("Pre-destroy managed instance |%s|.", instance.getClass());
		try {
			method.invoke(instance);
		} catch (Throwable t) {
			log.dump(format("Managed instance |%s| pre-destroy fail:", instance.getClass()), t);
		}
	}
	
	// --------------------------------------------------------------------------------------------
	
	void resetCache() {
		PRE_DESTROY_METHODS.clear();
	}
}
