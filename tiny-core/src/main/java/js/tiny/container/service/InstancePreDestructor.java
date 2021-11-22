package js.tiny.container.service;

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

public class InstancePreDestructor extends BaseInstanceLifecycle implements IInstancePreDestroyProcessor {
	private static final Log log = LogFactory.getLog(InstancePreDestructor.class);

	/** Cache for @PreDestroy methods filled on the fly. */
	private static final Map<Class<?>, Method> PRE_DESTROY_METHODS = new HashMap<>();

	public InstancePreDestructor() {
		log.trace("InstancePreDestroyProcessor()");
	}

	@Override
	public Priority getPriority() {
		return Priority.DESTRUCTOR;
	}

	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		return scanAnnotatedMethod(PRE_DESTROY_METHODS, managedClass.getImplementationClass(), PreDestroy.class);
	}

	@Override
	public <T> void onInstancePreDestroy(T instance) {
		Params.notNull(instance, "Instance");
		final Class<?> implementationClass = instance.getClass();
		log.debug("Pre-destroy managed instance |%s|.", implementationClass);
		

		Method method = PRE_DESTROY_METHODS.get(implementationClass);
		if (method == null) {
			throw new IllegalStateException(format("Missing pre-destroy method on implementation class |%s|.", implementationClass));
		}

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
