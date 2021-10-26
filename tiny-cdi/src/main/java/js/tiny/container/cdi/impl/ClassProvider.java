package js.tiny.container.cdi.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;
import javax.inject.Provider;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.IProvisionInvocation;
import js.tiny.container.cdi.service.CDI;

public class ClassProvider<T> implements Provider<T> {
	private static final Log log = LogFactory.getLog(ClassProvider.class);

	private final CDI injector;
	private final Class<? extends T> type;

	public ClassProvider(CDI injector, Class<? extends T> type) {
		this.injector = injector;
		this.type = type;
	}

	@Override
	public T get() {
		try {

			Constructor<? extends T> constructor = getDeclaredConstructor(type);

			Object[] parameters = new Object[constructor.getParameterCount()];
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			for (int i = 0; i < parameters.length; ++i) {
				// TODO: circular dependency
				parameters[i] = injector.getInstance(parameterTypes[i]);
			}

			T instance = constructor.newInstance(parameters);
			injector.fireEvent(IProvisionInvocation.event(this, instance));
			return instance;

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error(e);
		}
		return null;
	}

	@Override
	public String toString() {
		return type.getCanonicalName() + ":CLASS";
	}

	private static <T> Constructor<T> getDeclaredConstructor(Class<T> type) {
		if (type == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Constructor<T>[] declaredConstructors = (Constructor<T>[]) type.getDeclaredConstructors();
		if (declaredConstructors.length == 0) {
			throw new BugError("Invalid implementation class |%s|. Missing constructor.", type);
		}
		Constructor<T> defaultConstructor = null;
		Constructor<T> constructor = null;

		for (Constructor<T> declaredConstructor : declaredConstructors) {
			// synthetic constructors are created by compiler to circumvent JVM limitations, JVM that is not evolving with
			// the same speed as the language; for example, to allow outer class to access private members on a nested class
			// compiler creates a constructor with a single argument of very nested class type
			if (declaredConstructor.isSynthetic()) {
				continue;
			}

			if (declaredConstructor.getAnnotation(Inject.class) != null) {
				constructor = declaredConstructor;
				break;
			}

			if (declaredConstructor.getParameterTypes().length == 0) {
				defaultConstructor = declaredConstructor;
				continue;
			}
			if (constructor != null) {
				throw new BugError("Implementation class |%s| has not a single constructor with parameters. Use @Inject to declare which constructor to use.", type);
			}
			constructor = declaredConstructor;
		}

		if (constructor == null) {
			if (defaultConstructor == null) {
				throw new BugError("Invalid implementation class |%s|. Missing default constructor.", type);
			}
			constructor = defaultConstructor;
		}
		constructor.setAccessible(true);
		return constructor;
	}
}