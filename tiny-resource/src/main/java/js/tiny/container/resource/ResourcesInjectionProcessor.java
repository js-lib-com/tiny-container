package js.tiny.container.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IAnnotationsScanner;
import js.util.Classes;
import js.util.Strings;

/**
 * Post processor for instance resources injection. Remember that a resource is a service external to container for which there
 * is a connector. Also term <code>resource</code> refers to simple environment entries, as defined by EJB specification. This
 * processor injects both resource objects and simple environment entries but is not allowed to inject resources into static
 * fields.
 * 
 * In essence this processor scans for {@link Resource} annotation on managed classes, and for every field retrieves its value
 * from JNDI and injects it reflexively. Note that only fields from managed class implementation are scanned but not super
 * classes.
 * 
 * {@link Resource} annotation has two means to retrieve objects from JNDI: {@link Resource#lookup()} and
 * {@link Resource#name()}. For <code>lookup</code> this implementation uses global environment <code>java:global/env</code>
 * whereas for <code>name</code> uses component relative environment, <code>java:comp/env</code>. If {@link Resource} annotation
 * has no attribute uses class canonical name followed by field name, separated by slash ('/').
 * 
 * @author Iulian Rotaru
 */
public class ResourcesInjectionProcessor implements IInstancePostConstructProcessor, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(ResourcesInjectionProcessor.class);

	private static final String GLOBAL_ENV = "java:global/env";
	private static final String COMP_ENV = "java:comp/env";

	private static final Map<Integer, Collection<Field>> MANAGED_FIELDS = new HashMap<>();

	private final Context globalEnvironment;
	private final Context componentEnvironment;

	public ResourcesInjectionProcessor() {
		this.globalEnvironment = environment(GLOBAL_ENV);
		this.componentEnvironment = environment(COMP_ENV);
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

	ResourcesInjectionProcessor(Context globalEnvironment, Context componentEnvironment) {
		this.globalEnvironment = globalEnvironment;
		this.componentEnvironment = componentEnvironment;
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public Iterable<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		MANAGED_FIELDS.put(managedClass.getKey(), scanFields(managedClass.getImplementationClass()));
		return Collections.emptyList();
	}

	@Override
	public Iterable<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		return Collections.emptyList();
	}

	/**
	 * Initialize resource fields from managed class with value retrieved from JNDI.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 */
	@Override
	public <T> void onInstancePostConstruct(IManagedClass<T> managedClass, T instance) {
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

			Object value = getJndiValue(field);
			if (value == null) {
				log.debug("Null dependency for field |%s|. Leave it unchanged.", field);
				continue;
			}

			Classes.setFieldValue(instance, field, value);
			log.debug("Inject field |%s| value |%s|.", field, value);
		}
	}

	/**
	 * Load JNDI object or simple environment entry identified by field {@link Resource} annotation. Returns null if JNDI value
	 * is not found.
	 * 
	 * Provided class field is guaranteed to have {@link Resource} annotation. {@link Resource} annotation has two means to
	 * retrieve objects from JNDI: {@link Resource#lookup()} and {@link Resource#name()}. For <code>lookup</code> this
	 * implementation uses global environment <code>java:global/env</code> whereas for <code>name</code> uses component relative
	 * environment, <code>java:comp/env</code>.
	 * 
	 * If {@link Resource} annotation has no attribute infer resource <code>name</code> from class field: uses declaring class
	 * canonical name followed by field name, separated by slash ('/').
	 * 
	 * @param field class field, guaranteed to have {@link Resource} annotation.
	 * @return JNDI object or simple environment entry or null if not found.
	 */
	private Object getJndiValue(Field field) {
		Resource resourceAnnotation = field.getAnnotation(Resource.class);
		assert resourceAnnotation != null;

		String lookupName = resourceAnnotation.lookup();
		if (!lookupName.isEmpty()) {
			return jndiLookup(globalEnvironment, GLOBAL_ENV, lookupName);
		}

		String name = resourceAnnotation.name();
		if (name.isEmpty()) {
			name = Strings.concat(field.getDeclaringClass().getName(), '/', field.getName());
		}
		if (name.startsWith("java:")) {
			throw new IllegalArgumentException("Resource name should be relative to component environment: " + name);
		}
		return jndiLookup(componentEnvironment, COMP_ENV, name);
	}

	/**
	 * Perform JNDI object lookup on given naming context. If naming context is null this method silently returns null.
	 * 
	 * @param namingContext naming context, possible null,
	 * @param contextName context name, for logging,
	 * @param name JNDI object name relative to naming context.
	 * @return JNDI object or null if not found.
	 */
	private static Object jndiLookup(Context namingContext, String contextName, String name) {
		if (namingContext == null) {
			return null;
		}
		Object value = null;
		try {
			value = namingContext.lookup(name);
			log.info("Load JDNI object |%s/%s| of type |%s|.", contextName, name, value.getClass());
		} catch (NamingException e) {
			log.warn("Missing JNDI object |%s/%s|.", contextName, name);
		}
		return value;
	}

	/**
	 * Scan class resources dependencies declared by {@link Resource} annotation. This method scans all fields, no matter
	 * private, protected or public. Anyway it is considered a bug if resource annotation is found on final or static field.
	 * 
	 * Returns a collection of reflective fields with accessibility set but in not particular order. If given type argument is
	 * null returns empty collection.
	 * 
	 * @param type class to scan dependencies for, null tolerated.
	 * @return dependencies collection, in no particular order.
	 * @throws BugError if annotation is used on final or static field.
	 */
	private static Collection<Field> scanFields(Class<?> type) {
		if (type == null) {
			return Collections.emptyList();
		}
		Collection<Field> dependencies = new ArrayList<>();
		for (Field field : type.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Resource.class)) {
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
}
