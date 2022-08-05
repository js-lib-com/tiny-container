package com.jslib.container.service;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IInstancePostConstructProcessor;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;

import jakarta.annotation.PostConstruct;
import com.jslib.lang.ManagedPostConstruct;
import com.jslib.util.Params;
import com.jslib.util.Types;

/**
 * Execute {@link ManagedPostConstruct#postConstruct()} on managed instance. Instance post-construction is executed after
 * initialization and configuration, of course only if managed instance implements {@link ManagedPostConstruct} interface.
 * 
 * @author Iulian Rotaru
 */
public class InstancePostConstructor implements IInstancePostConstructProcessor {
	private static final Log log = LogFactory.getLog(InstancePostConstructor.class);

	private final Map<Class<?>, IManagedMethod> methodsCache = new HashMap<>();

	@Override
	public Priority getPriority() {
		return Priority.CONSTRUCTOR;
	}

	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		class Found {
			boolean value;
		}

		final Found found = new Found();
		managedClass.getManagedMethods().forEach(managedMethod -> {
			if (managedMethod.scanAnnotation(PostConstruct.class) != null) {
				sanityCheck(managedMethod);
				if (methodsCache.put(managedClass.getImplementationClass(), managedMethod) != null) {
					throw new IllegalStateException("Only one post-constructor allowed. See managed class " + managedClass);
				}
				found.value = true;
			}
		});

		return found.value;
	}

	private static void sanityCheck(IManagedMethod managedMethod) {
		if (managedMethod.isStatic()) {
			throw new IllegalStateException("Post-constructor should not be static. See " + managedMethod);
		}
		if (managedMethod.getParameterTypes().length > 0) {
			throw new IllegalStateException("Post-constructor should have no parameter. See " + managedMethod);
		}
		if (!Types.isVoid(managedMethod.getReturnType())) {
			throw new IllegalStateException("Post-constructor should not return any value. See " + managedMethod);
		}
		if (managedMethod.getExceptionTypes().length != 0) {
			throw new IllegalStateException("Post-constructor should not throw any checked exception. See " + managedMethod);
		}
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

		IManagedMethod managedMethod = methodsCache.get(implementationClass);
		if (managedMethod == null) {
			throw new IllegalStateException(format("Missing post-construct method on implementation class |%s|.", implementationClass));
		}

		try {
			managedMethod.invoke(instance);
		} catch (Throwable t) {
			if (t instanceof InvocationTargetException) {
				t = ((InvocationTargetException) t).getTargetException();
			}
			log.dump(format("Managed instance |%s| post-construct fail:", implementationClass.getCanonicalName()), t);
			throw new RuntimeException(format("Managed instance |%s| post-construct fail: %s: %s", implementationClass.getCanonicalName(), t.getClass().getCanonicalName(), t.getMessage()));
		}
	}
}
