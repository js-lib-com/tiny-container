package js.tiny.container.service;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Params;
import js.util.Types;

public class InstancePreDestructor implements IInstancePreDestroyProcessor {
	private static final Log log = LogFactory.getLog(InstancePreDestructor.class);

	// TODO: replace static with injected cache class with singleton scope
	private static final Map<Class<?>, IManagedMethod> methodsCache = new HashMap<>();

	@Override
	public Priority getPriority() {
		return Priority.DESTRUCTOR;
	}

	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		class Found {
			boolean value;
		}

		final Found found = new Found();
		managedClass.getManagedMethods().forEach(managedMethod -> {
			if (IPreDestroy.scan(managedMethod) != null) {
				sanityCheck(managedMethod);
				if (methodsCache.put(managedClass.getImplementationClass(), managedMethod) != null) {
					throw new IllegalStateException("Only one pre-destructor allowed. See managed class " + managedClass);
				}
				found.value = true;
			}
		});
		
		return found.value;
	}

	private static void sanityCheck(IManagedMethod managedMethod) {
		if (managedMethod.isStatic()) {
			throw new IllegalStateException("Pre-destructor should not be static. See " + managedMethod);
		}
		if (managedMethod.getParameterTypes().length > 0) {
			throw new IllegalStateException("Pre-destructor should have no parameter. See " + managedMethod);
		}
		if (!Types.isVoid(managedMethod.getReturnType())) {
			throw new IllegalStateException("Pre-destructor should not return any value. See " + managedMethod);
		}
		if (managedMethod.getExceptionTypes().length != 0) {
			throw new IllegalStateException("Pre-destructor should not throw any checked exception. See " + managedMethod);
		}
	}

	@Override
	public <T> void onInstancePreDestroy(T instance) {
		Params.notNull(instance, "Instance");
		final Class<?> implementationClass = instance.getClass();
		log.debug("Pre-destroy managed instance |%s|.", implementationClass);

		IManagedMethod managedMethod = methodsCache.get(implementationClass);
		if (managedMethod == null) {
			throw new IllegalStateException(format("Missing pre-destroy method on implementation class |%s|.", implementationClass));
		}

		try {
			managedMethod.invoke(instance);
		} catch (Throwable t) {
			log.dump(format("Managed instance |%s| pre-destroy fail:", instance.getClass()), t);
		}
	}

	// --------------------------------------------------------------------------------------------

	public static void resetCache() {
		methodsCache.clear();
	}
}
