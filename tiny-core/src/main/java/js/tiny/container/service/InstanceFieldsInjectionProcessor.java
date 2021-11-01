package js.tiny.container.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.DependencyLoader;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;
import js.util.Classes;
import js.util.Strings;

/**
 * Post processor for instance fields injection. Fields are discovered by managed class based on {@link Inject} annotation and
 * provided to this processor by {@link IManagedClass#getDependencies()}. This class inherits dependency processor and delegates
 * {@link DependencyLoader#getDependencyValue(IManagedClass, Class)} for dependency value processing.
 * <p>
 * In essence this processor scans all dependencies detected by managed class and for every field retrieve its dependency value
 * and inject it reflexively.
 * 
 * @author Iulian Rotaru
 */
public class InstanceFieldsInjectionProcessor implements IInstancePostConstructionProcessor, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(InstanceFieldsInitializationProcessor.class);

	private static final Map<Integer, Collection<Field>> MANAGED_FIELDS = new HashMap<>();

	private final Context globalEnvironment;
	private final Context componentEnvironment;

	public InstanceFieldsInjectionProcessor() {
		this.globalEnvironment = environment("java:global/env");
		this.componentEnvironment = environment("java:comp/env");
	}

	private static Context environment(String name) {
		Context context = null;
		try {
			Context serverContext = new InitialContext();
			context = (Context) serverContext.lookup(name);
		} catch (NamingException e) {
			log.error(e);
		}
		return context;
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public Iterable<IServiceMeta> scanServiceMeta(IManagedClass<?> managedClass) {
		MANAGED_FIELDS.put(managedClass.getKey(), scanDependencies(managedClass.getImplementationClass()));
		return Collections.emptyList();
	}

	@Override
	public Iterable<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		return Collections.emptyList();
	}

	/**
	 * Inject dependencies described by given managed class into related managed instance. For every dependency field retrieve
	 * its value using {@link DependencyLoader#getDependencyValue(IManagedClass, Class)} and inject it reflexively.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError if a field dependency cannot be resolved.
	 */
	@Override
	public <T> void onInstancePostConstruction(IManagedClass<T> managedClass, T instance) {
		if (instance == null || !MANAGED_FIELDS.containsKey(managedClass.getKey())) {
			// null instance and no fields conditions are silently ignored
			return;
		}
		for (Field field : MANAGED_FIELDS.get(managedClass.getKey())) {
			if (field.isSynthetic()) {
				// it seems there can be injected fields, created via byte code manipulation, when run with test coverage active
				// not clear why and how but was consistently observed on mock object from unit test run with coverage
				continue;
			}

			Object value = null;
			Resource resourceAnnotation = field.getAnnotation(Resource.class);
			if (resourceAnnotation != null) {
				value = getEnvEntryValue(field, resourceAnnotation);
			} else {
				value = DependencyLoader.getDependencyValue(managedClass, field.getType());
			}

			if (value == null) {
				log.debug("Null dependency for field |%s|. Leave it unchanged.", field);
				continue;
			}

			Classes.setFieldValue(instance, field, value);
			log.debug("Inject field |%s| value |%s|.", field, value);
		}
	}

	private Object getEnvEntryValue(Field field, Resource resourceAnnotation) {
		String lookupName = resourceAnnotation.lookup();
		if (!lookupName.isEmpty()) {
			return getEnvEntryValue(globalEnvironment, lookupName, field.getType());
		}

		String name = resourceAnnotation.name();
		if (name.isEmpty()) {
			name = Strings.concat(field.getDeclaringClass().getName(), '/', field.getName());
		}
		if (name.startsWith("java:")) {
			throw new IllegalArgumentException("Resource name should be relative to component environment: " + name);
		}
		return getEnvEntryValue(componentEnvironment, name, field.getType());
	}

	private static Object getEnvEntryValue(Context context, String name, Class<?> type) {
		if (context == null) {
			return null;
		}
		Object value = null;
		try {
			value = context.lookup(name);
			log.info("Load environment entry |java:comp/env/%s| of type |%s|.", name, type);
		} catch (NamingException e) {
			log.warn("Missing environment entry |java:comp/env/%s|.", name);
		}
		return value;
	}

	/**
	 * Scan class dependencies declared by {@link Inject} annotation. This method scans all fields, no matter private, protected
	 * or public. Anyway it is considered a bug if inject annotation is found on final or static field.
	 * <p>
	 * Returns a collection of reflective fields with accessibility set but in not particular order. If given class argument is
	 * null returns empty collection.
	 * 
	 * @param clazz class to scan dependencies for, null tolerated.
	 * @return dependencies collection, in no particular order.
	 * @throws BugError if annotation is used on final or static field.
	 */
	private static Collection<Field> scanDependencies(Class<?> clazz) {
		if (clazz == null) {
			return Collections.emptyList();
		}
		Collection<Field> dependencies = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Inject.class) && !field.isAnnotationPresent(Resource.class)) {
				continue;
			}
			if (Modifier.isFinal(field.getModifiers())) {
				throw new BugError("Attempt to inject final field |%s|.", field.getName());
			}
			if (Modifier.isStatic(field.getModifiers())) {
				throw new BugError("Attempt to inject static field |%s|.", field.getName());
			}
			field.setAccessible(true);
			dependencies.add(field);
		}
		return dependencies;
	}

	// --------------------------------------------------------------------------------------------
	// tests access

	Collection<Field> getManagedFields(Integer key) {
		return MANAGED_FIELDS.get(key);
	}

	// --------------------------------------------------------------------------------------------

	/** Java service loader declared on META-INF/services */
	public static class Service implements IContainerServiceProvider {
		@Override
		public IContainerService getService(IContainer container) {
			return new InstanceFieldsInjectionProcessor();
		}
	}
}
